/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.maven.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.logging.Slf4jPlexusLogger;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RecreateMavenMetadataWalkerProcessor;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.DottedStoreWalkerFilter;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.wastebasket.DeleteOperation;
import org.sonatype.nexus.util.ItemPathUtils;
import org.sonatype.scheduling.TaskUtil;

/**
 * The Class SnapshotRemoverJob. After a succesful run, the job guarantees that there will remain at least
 * minCountOfSnapshotsToKeep (but maybe more) snapshots per one snapshot collection by removing all older from
 * removeSnapshotsOlderThanDays. If should remove snaps if their release counterpart exists, the whole GAV will be
 * removed.
 *
 * @author cstamas
 */
@Component( role = SnapshotRemover.class )
public class DefaultSnapshotRemover
    extends AbstractLoggingComponent
    implements SnapshotRemover
{

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private Walker walker;

    @Requirement( hint = "maven2" )
    private ContentClass contentClass;

    private VersionScheme versionScheme = new GenericVersionScheme();

    protected RepositoryRegistry getRepositoryRegistry()
    {
        return repositoryRegistry;
    }

    public SnapshotRemovalResult removeSnapshots( SnapshotRemovalRequest request )
        throws NoSuchRepositoryException, IllegalArgumentException
    {
        SnapshotRemovalResult result = new SnapshotRemovalResult();

        logDetails( request );

        if ( request.getRepositoryId() != null )
        {
            Repository repository = getRepositoryRegistry().getRepository( request.getRepositoryId() );

            if ( !process( request, result, repository ) )
            {
                throw new IllegalArgumentException( "The repository with ID=" + repository.getId()
                                                        + " is not valid for Snapshot Removal Task!" );
            }
        }
        else
        {
            for ( Repository repository : getRepositoryRegistry().getRepositories() )
            {
                process( request, result, repository );
            }
        }

        return result;
    }

    private void process( SnapshotRemovalRequest request, SnapshotRemovalResult result, GroupRepository group )
    {
        for ( Repository repository : group.getMemberRepositories() )
        {
            process( request, result, repository );
        }
    }

    private boolean process( SnapshotRemovalRequest request, SnapshotRemovalResult result, Repository repository )
    {
        // only from maven repositories, stay silent for others and simply skip
        if ( !repository.getRepositoryContentClass().isCompatible( contentClass ) )
        {
            getLogger().debug( "Skipping '" + repository.getId() + "' is not a maven 2 repository" );
            return false;
        }

        if ( !repository.getLocalStatus().shouldServiceRequest() )
        {
            getLogger().debug( "Skipping '" + repository.getId() + "' the repository is out of service" );
            return false;
        }

        if ( repository.getRepositoryKind().isFacetAvailable( GroupRepository.class ) )
        {
            process( request, result, repository.adaptToFacet( GroupRepository.class ) );
        }
        else if ( repository.getRepositoryKind().isFacetAvailable( MavenRepository.class ) )
        {
            result.addResult( removeSnapshotsFromMavenRepository( repository.adaptToFacet( MavenRepository.class ),
                                                                  request ) );
        }

        return true;
    }

    /**
     * Removes the snapshots from maven repository.
     *
     * @param repository the repository
     * @throws Exception the exception
     */
    protected SnapshotRemovalRepositoryResult removeSnapshotsFromMavenRepository( MavenRepository repository,
                                                                                  SnapshotRemovalRequest request )
    {
        TaskUtil.checkInterruption();

        SnapshotRemovalRepositoryResult result = new SnapshotRemovalRepositoryResult( repository.getId(), 0, 0, true );

        if ( !repository.getLocalStatus().shouldServiceRequest() )
        {
            return result;
        }

        // we are already processed here, so skip repo
        if ( request.isProcessedRepo( repository.getId() ) )
        {
            return new SnapshotRemovalRepositoryResult( repository.getId(), true );
        }

        request.addProcessedRepo( repository.getId() );

        // if this is not snap repo, do nothing
        if ( !RepositoryPolicy.SNAPSHOT.equals( repository.getRepositoryPolicy() ) )
        {
            return result;
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug(
                "Collecting deletable snapshots on repository " + repository.getId() + " from storage directory "
                    + repository.getLocalUrl() );
        }

        request.getMetadataRebuildPaths().clear();

        // create a walker to collect deletables and let it loose on collections only
        SnapshotRemoverWalkerProcessor snapshotRemoveProcessor =
            new SnapshotRemoverWalkerProcessor( repository, request );

        DefaultWalkerContext ctxMain =
            new DefaultWalkerContext( repository, new ResourceStoreRequest( "/" ), new DottedStoreWalkerFilter() );

        ctxMain.getContext().put( DeleteOperation.DELETE_OPERATION_CTX_KEY, getDeleteOperation( request ) );

        ctxMain.getProcessors().add( snapshotRemoveProcessor );

        walker.walk( ctxMain );

        if ( ctxMain.getStopCause() != null )
        {
            result.setSuccessful( false );
        }

        // and collect results
        result.setDeletedSnapshots( snapshotRemoveProcessor.getDeletedSnapshots() );
        result.setDeletedFiles( snapshotRemoveProcessor.getDeletedFiles() );

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug(
                "Collected and deleted " + snapshotRemoveProcessor.getDeletedSnapshots()
                    + " snapshots with alltogether " + snapshotRemoveProcessor.getDeletedFiles()
                    + " files on repository " + repository.getId() );
        }

        repository.expireCaches( new ResourceStoreRequest( RepositoryItemUid.PATH_ROOT ) );

        RecreateMavenMetadataWalkerProcessor metadataRebuildProcessor =
            new RecreateMavenMetadataWalkerProcessor( Slf4jPlexusLogger.getPlexusLogger( getLogger() ), getDeleteOperation( request ) );

        for ( String path : request.getMetadataRebuildPaths() )
        {
            TaskUtil.checkInterruption();

            DefaultWalkerContext ctxMd =
                new DefaultWalkerContext( repository, new ResourceStoreRequest( path ), new DottedStoreWalkerFilter() );

            ctxMd.getProcessors().add( metadataRebuildProcessor );

            try
            {
                walker.walk( ctxMd );
            }
            catch ( WalkerException e )
            {
                if ( !( e.getCause() instanceof ItemNotFoundException ) )
                {
                    // do not ignore it
                    throw e;
                }
            }
        }

        return result;
    }

    private DeleteOperation getDeleteOperation( final SnapshotRemovalRequest request )
    {
        return request.isDeleteImmediately() ? DeleteOperation.DELETE_PERMANENTLY : DeleteOperation.MOVE_TO_TRASH;
    }

    private void logDetails( SnapshotRemovalRequest request )
    {
        if ( request.getRepositoryId() != null )
        {
            getLogger().info( "Removing old SNAPSHOT deployments from " + request.getRepositoryId() + " repository." );
        }
        else
        {
            getLogger().info( "Removing old SNAPSHOT deployments from all repositories." );
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "With parameters: " );
            getLogger().debug( "    MinCountOfSnapshotsToKeep: " + request.getMinCountOfSnapshotsToKeep() );
            getLogger().debug( "    RemoveSnapshotsOlderThanDays: " + request.getRemoveSnapshotsOlderThanDays() );
            getLogger().debug( "    RemoveIfReleaseExists: " + request.isRemoveIfReleaseExists() );
            getLogger().debug( "    DeleteImmediately: " + request.isDeleteImmediately() );
        }
    }

    private class SnapshotRemoverWalkerProcessor
        extends AbstractWalkerProcessor
    {

        private final MavenRepository repository;

        private final SnapshotRemovalRequest request;

        private final Map<Version, List<StorageFileItem>> remainingSnapshotsAndFiles =
            new HashMap<Version, List<StorageFileItem>>();

        private final Map<Version, List<StorageFileItem>> deletableSnapshotsAndFiles =
            new HashMap<Version, List<StorageFileItem>>();

        private final long dateThreshold;

        private boolean shouldProcessCollection;

        private boolean removeWholeGAV;

        private int deletedSnapshots = 0;

        private int deletedFiles = 0;

        public SnapshotRemoverWalkerProcessor( MavenRepository repository, SnapshotRemovalRequest request )
        {
            this.repository = repository;

            this.request = request;

            int days = request.getRemoveSnapshotsOlderThanDays();

            if ( days > 0 )
            {
                this.dateThreshold = System.currentTimeMillis() - ( days * 86400000L );
            }
            else
            {
                this.dateThreshold = -1;
            }
        }

        protected void addStorageFileItemToMap( Map<Version, List<StorageFileItem>> map, Gav gav, StorageFileItem item )
        {
            Version key = null;
            try
            {
                key = versionScheme.parseVersion( gav.getVersion() );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                try
                {
                    key = versionScheme.parseVersion( "0.0-SNAPSHOT" );
                }
                catch ( InvalidVersionSpecificationException e1 )
                {
                    // nah
                }
            }

            if ( !map.containsKey( key ) )
            {
                map.put( key, new ArrayList<StorageFileItem>() );
            }

            map.get( key ).add( item );
        }

        @Override
        public void processItem( WalkerContext context, StorageItem item )
            throws Exception
        {
        }

        @Override
        public void onCollectionExit( WalkerContext context, StorageCollectionItem coll )
        {
            try
            {
                doOnCollectionExit( context, coll );
            }
            catch ( Exception e )
            {
                // we always simply log the exception and continue
                getLogger().warn( "SnapshotRemover is failed to process path: '" + coll.getPath() + "'.", e );
            }
        }

        public void doOnCollectionExit( WalkerContext context, StorageCollectionItem coll )
            throws Exception
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "onCollectionExit() :: " + coll.getRepositoryItemUid().toString() );
            }

            shouldProcessCollection = coll.getPath().endsWith( "SNAPSHOT" );

            if ( !shouldProcessCollection )
            {
                return;
            }

            deletableSnapshotsAndFiles.clear();

            remainingSnapshotsAndFiles.clear();

            removeWholeGAV = false;

            Gav gav = null;

            Collection<StorageItem> items;

            items = repository.list( false, coll );

            HashSet<Long> versionsToRemove = new HashSet<Long>();

            // gathering the facts
            for ( StorageItem item : items )
            {
                if ( !item.isVirtual() && !StorageCollectionItem.class.isAssignableFrom( item.getClass() ) )
                {
                    gav =
                        ( (MavenRepository) coll.getRepositoryItemUid().getRepository() ).getGavCalculator().pathToGav(
                            item.getPath() );

                    if ( gav != null )
                    {
                        // if we find a pom, check for delete on release
                        if ( !gav.isHash() && !gav.isSignature() && gav.getExtension().equals( "pom" ) )
                        {
                            if ( request.isRemoveIfReleaseExists()
                                && releaseExistsForSnapshot( gav, item.getItemContext() ) )
                            {
                                getLogger().debug( "Found POM and release exists, removing whole gav." );

                                removeWholeGAV = true;

                                // Will break out and junk whole gav
                                break;
                            }
                        }

                        item.getItemContext().put( Gav.class.getName(), gav );

                        if ( gav.getSnapshotTimeStamp() != null )
                        {
                            getLogger().debug( "Using GAV snapshot timestamp" );

                            long itemTimestamp = gav.getSnapshotTimeStamp().longValue();

                            getLogger().debug( "NOW is " + itemTimestamp );

                            // If this timestamp is already marked to be removed, junk it
                            if ( versionsToRemove.contains( new Long( itemTimestamp ) ) )
                            {
                                addStorageFileItemToMap( deletableSnapshotsAndFiles, gav, (StorageFileItem) item );
                            }
                            else
                            {
                                getLogger().debug(
                                    "itemTimestamp=" + itemTimestamp + ", dateTreshold=" + dateThreshold );

                                // if dateTreshold is not used (zero days) OR
                                // if itemTimestamp is less then dateTreshold (NB: both are positive!)
                                // below will the retentionCount overrule if needed this
                                if ( -1 == dateThreshold || itemTimestamp < dateThreshold )
                                {
                                    versionsToRemove.add( new Long( itemTimestamp ) );
                                    addStorageFileItemToMap( deletableSnapshotsAndFiles, gav, (StorageFileItem) item );
                                }
                                else
                                {
                                    addStorageFileItemToMap( remainingSnapshotsAndFiles, gav, (StorageFileItem) item );
                                }
                            }
                        }
                        else
                        {
                            // If no timestamp on gav, then it is a non-unique snapshot
                            // and should _not_ be removed
                            getLogger().debug( "GAV Snapshot timestamp not available, skipping non-unique snapshot" );

                            addStorageFileItemToMap( remainingSnapshotsAndFiles, gav, (StorageFileItem) item );
                        }
                    }
                }
            }

            // and doing the work here
            if ( removeWholeGAV )
            {
                try
                {
                    for ( StorageItem item : items )
                    {
                        try
                        {
                            // preserve possible subdirs
                            if ( !( item instanceof StorageCollectionItem ) )
                            {
                                repository.deleteItem( false, createResourceStoreRequest( item, context ) );
                            }
                        }
                        catch ( ItemNotFoundException e )
                        {
                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug(
                                    "Could not delete whole GAV " + coll.getRepositoryItemUid().toString(), e );
                            }
                        }
                    }
                }
                catch ( Exception e )
                {
                    getLogger().warn( "Could not delete whole GAV " + coll.getRepositoryItemUid().toString(), e );
                }
            }
            else
            {
                // and now check some things
                if ( remainingSnapshotsAndFiles.size() < request.getMinCountOfSnapshotsToKeep() )
                {
                    // do something
                    if ( remainingSnapshotsAndFiles.size() + deletableSnapshotsAndFiles.size()
                        < request.getMinCountOfSnapshotsToKeep() )
                    {
                        // delete nothing, since there is less snapshots in total as allowed
                        deletableSnapshotsAndFiles.clear();
                    }
                    else
                    {
                        TreeSet<Version> keys = new TreeSet<Version>( deletableSnapshotsAndFiles.keySet() );

                        while ( !keys.isEmpty()
                            && remainingSnapshotsAndFiles.size() < request.getMinCountOfSnapshotsToKeep() )
                        {
                            Version keyToMove = keys.last();

                            if ( remainingSnapshotsAndFiles.containsKey( keyToMove ) )
                            {
                                remainingSnapshotsAndFiles.get( keyToMove ).addAll(
                                    deletableSnapshotsAndFiles.get( keyToMove ) );
                            }
                            else
                            {
                                remainingSnapshotsAndFiles.put( keyToMove,
                                                                deletableSnapshotsAndFiles.get( keyToMove ) );
                            }

                            deletableSnapshotsAndFiles.remove( keyToMove );

                            keys.remove( keyToMove );
                        }

                    }
                }

                // NEXUS-814: is this GAV have remaining artifacts?
                boolean gavHasMoreTimestampedSnapshots = remainingSnapshotsAndFiles.size() > 0;

                for ( Version key : deletableSnapshotsAndFiles.keySet() )
                {

                    List<StorageFileItem> files = deletableSnapshotsAndFiles.get( key );
                    deletedSnapshots++;

                    for ( StorageFileItem file : files )
                    {
                        try
                        {
                            // NEXUS-814: mark that we are deleting a TS snapshot, but there are still remaining
                            // ones in repository.
                            if ( gavHasMoreTimestampedSnapshots )
                            {
                                file.getItemContext().put( MORE_TS_SNAPSHOTS_EXISTS_FOR_GAV, Boolean.TRUE );
                            }

                            gav = (Gav) file.getItemContext().get( Gav.class.getName() );

                            repository.deleteItem( false, createResourceStoreRequest( file, context ) );

                            deletedFiles++;
                        }
                        catch ( ItemNotFoundException e )
                        {
                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug( "Could not delete file:", e );
                            }
                        }
                        catch ( Exception e )
                        {
                            getLogger().info( "Could not delete file:", e );
                        }
                    }
                }
            }

            removeDirectoryIfEmpty( coll );

            updateMetadataIfNecessary( context, coll );

        }

        private DeleteOperation getDeleteOperation( final WalkerContext context )
        {
            return (DeleteOperation) context.getContext().get( DeleteOperation.DELETE_OPERATION_CTX_KEY );
        }

        private void updateMetadataIfNecessary( WalkerContext context, StorageCollectionItem coll )
            throws Exception
        {
            // all snapshot files are deleted
            if ( !deletableSnapshotsAndFiles.isEmpty() && remainingSnapshotsAndFiles.isEmpty() )
            {
                String parentPath = ItemPathUtils.getParentPath( coll.getPath() );

                request.getMetadataRebuildPaths().add( parentPath );
            }
            else
            {
                request.getMetadataRebuildPaths().add( coll.getPath() );
            }

        }

        private void removeDirectoryIfEmpty( StorageCollectionItem coll )
            throws StorageException, IllegalOperationException, UnsupportedStorageOperationException
        {
            try
            {
                if ( repository.list( false, coll ).size() > 0 )
                {
                    return;
                }

                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Removing the empty directory leftover: UID=" + coll.getRepositoryItemUid().toString() );
                }

                // directory is empty, never move to trash
                repository.deleteItem( false, createResourceStoreRequest( coll, DeleteOperation.DELETE_PERMANENTLY ) );
            }
            catch ( ItemNotFoundException e )
            {
                // silent, this happens if whole GAV is removed and the dir is removed too
            }
        }

        public boolean releaseExistsForSnapshot( Gav snapshotGav, Map<String, Object> context )
        {
            for ( Repository repository : repositoryRegistry.getRepositories() )
            {
                // we need to filter for:
                // repository that is MavenRepository and is hosted or proxy
                // repository that has release policy
                if ( repository.getRepositoryKind().isFacetAvailable( MavenHostedRepository.class )
                    || repository.getRepositoryKind().isFacetAvailable( MavenProxyRepository.class ) )
                {
                    // actually, we don't care is it proxy or hosted, we only need to filter out groups and other
                    // "composite" reposes like shadows
                    MavenRepository mrepository = repository.adaptToFacet( MavenRepository.class );

                    // look in release reposes only
                    if ( mrepository.isUserManaged()
                        && RepositoryPolicy.RELEASE.equals( mrepository.getRepositoryPolicy() ) )
                    {
                        try
                        {
                            String releaseVersion = null;

                            // NEXUS-3148
                            if ( snapshotGav.getBaseVersion().endsWith( "-SNAPSHOT" ) )
                            {
                                // "-SNAPSHOT" :== 9 chars
                                releaseVersion =
                                    snapshotGav.getBaseVersion().substring( 0,
                                                                            snapshotGav.getBaseVersion().length() - 9 );
                            }
                            else
                            {
                                // "SNAPSHOT" :== 8 chars
                                releaseVersion =
                                    snapshotGav.getBaseVersion().substring( 0,
                                                                            snapshotGav.getBaseVersion().length() - 8 );
                            }

                            Gav releaseGav =
                                new Gav( snapshotGav.getGroupId(), snapshotGav.getArtifactId(), releaseVersion,
                                         snapshotGav.getClassifier(), snapshotGav.getExtension(), null, null, null,
                                         false, null, false, null );

                            String path = mrepository.getGavCalculator().gavToPath( releaseGav );

                            ResourceStoreRequest req = new ResourceStoreRequest( path, true );

                            req.getRequestContext().putAll( context );

                            mrepository.retrieveItem( false, req );

                            return true;
                        }
                        catch ( ItemNotFoundException e )
                        {
                            // nothing
                        }
                        catch ( Exception e )
                        {
                            // nothing
                        }
                    }
                }
            }

            return false;
        }

        private ResourceStoreRequest createResourceStoreRequest( final StorageItem item, final WalkerContext ctx )
        {
            ResourceStoreRequest request = new ResourceStoreRequest( item );

            if ( ctx.getContext().containsKey( DeleteOperation.DELETE_OPERATION_CTX_KEY ) )
            {
                request.getRequestContext().put( DeleteOperation.DELETE_OPERATION_CTX_KEY, ctx.getContext().get( DeleteOperation.DELETE_OPERATION_CTX_KEY )  );
            }

            return request;
        }

        private ResourceStoreRequest createResourceStoreRequest( final StorageCollectionItem item,
                                                                 final DeleteOperation operation )
        {
            ResourceStoreRequest request = new ResourceStoreRequest( item );
            request.getRequestContext().put( DeleteOperation.DELETE_OPERATION_CTX_KEY, operation );
            return request;
        }

        public int getDeletedSnapshots()
        {
            return deletedSnapshots;
        }

        public int getDeletedFiles()
        {
            return deletedFiles;
        }

    }

}
