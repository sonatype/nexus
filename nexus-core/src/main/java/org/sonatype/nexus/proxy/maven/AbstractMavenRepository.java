/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy.maven;

import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_MD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_SHA1;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.SUFFIX_MD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.SUFFIX_SHA1;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.doRetrieveMD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.doRetrieveSHA1;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.newHashItem;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryEventEvictUnusedItems;
import org.sonatype.nexus.proxy.events.RepositoryEventRecreateMavenMetadata;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.maven.EvictUnusedMavenItemsWalkerProcessor.EvictUnusedMavenItemsWalkerFilter;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.nexus.proxy.maven.wl.ProxyRequestFilter;
import org.sonatype.nexus.proxy.maven.wl.WLManager;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.MutableProxyRepositoryKind;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;

/**
 * The abstract (layout unaware) Maven Repository.
 * 
 * @author cstamas
 */
public abstract class AbstractMavenRepository
    extends AbstractProxyRepository
    implements MavenRepository, MavenHostedRepository, MavenProxyRepository
{

    /**
     * Metadata manager.
     */
    @Requirement
    private MetadataManager metadataManager;

    /**
     * The artifact packaging mapper.
     */
    @Requirement
    private ArtifactPackagingMapper artifactPackagingMapper;

    @Requirement
    protected ProxyRequestFilter proxyRequestFilter;

    private MutableProxyRepositoryKind repositoryKind;

    private ArtifactStoreHelper artifactStoreHelper;

    /** if download remote index flag changed, need special handling after save */
    private boolean downloadRemoteIndexEnabled = false;

    @Override
    protected AbstractMavenRepositoryConfiguration getExternalConfiguration( boolean forModification )
    {
        return (AbstractMavenRepositoryConfiguration) super.getExternalConfiguration( forModification );
    }

    @Override
    public boolean commitChanges()
        throws ConfigurationException
    {
        boolean result = super.commitChanges();

        if ( result )
        {
            this.downloadRemoteIndexEnabled = false;
        }

        return result;
    }

    @Override
    public boolean rollbackChanges()
    {
        this.downloadRemoteIndexEnabled = false;

        return super.rollbackChanges();
    }

    @Override
    protected RepositoryConfigurationUpdatedEvent getRepositoryConfigurationUpdatedEvent()
    {
        RepositoryConfigurationUpdatedEvent event = super.getRepositoryConfigurationUpdatedEvent();

        event.setDownloadRemoteIndexEnabled( this.downloadRemoteIndexEnabled );

        return event;
    }

    public ArtifactStoreHelper getArtifactStoreHelper()
    {
        if ( artifactStoreHelper == null )
        {
            artifactStoreHelper = new ArtifactStoreHelper( this );
        }

        return artifactStoreHelper;
    }

    public ArtifactPackagingMapper getArtifactPackagingMapper()
    {
        return artifactPackagingMapper;
    }

    protected ProxyRequestFilter getProxyRequestFilter()
    {
        return proxyRequestFilter;
    }

    /**
     * Override the "default" kind with Maven specifics.
     */
    public RepositoryKind getRepositoryKind()
    {
        if ( repositoryKind == null )
        {
            repositoryKind =
                new MutableProxyRepositoryKind( this, Arrays.asList( new Class<?>[] { MavenRepository.class } ),
                    new DefaultRepositoryKind( MavenHostedRepository.class, null ), new DefaultRepositoryKind(
                        MavenProxyRepository.class, null ) );
        }

        return repositoryKind;
    }

    @Override
    public Collection<String> evictUnusedItems( ResourceStoreRequest request, final long timestamp )
    {
        if ( !getLocalStatus().shouldServiceRequest() )
        {
            return Collections.emptyList();
        }

        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            Collection<String> result =
                doEvictUnusedItems( request, timestamp, new EvictUnusedMavenItemsWalkerProcessor( timestamp ),
                    new EvictUnusedMavenItemsWalkerFilter() );

            eventBus().post( new RepositoryEventEvictUnusedItems( this ) );

            return result;
        }
        else
        {
            return super.evictUnusedItems( request, timestamp );
        }
    }

    public boolean recreateMavenMetadata( ResourceStoreRequest request )
    {
        if ( !getLocalStatus().shouldServiceRequest() )
        {
            return false;
        }

        if ( !getRepositoryKind().isFacetAvailable( HostedRepository.class ) )
        {
            return false;
        }

        if ( StringUtils.isEmpty( request.getRequestPath() ) )
        {
            request.setRequestPath( RepositoryItemUid.PATH_ROOT );
        }

        try
        {
            if ( !this.getLocalStorage().containsItem( this, request ) )
            {
                getLogger().info(
                    "Skip rebuilding Maven2 Metadata in repository ID='" + getId()
                        + "' because it does not contain path='" + request.getRequestPath() + "'." );

                return false;
            }
        }
        catch ( StorageException e )
        {
            getLogger().warn( "Skip rebuilding Maven2 Metadata in repository ID='" + getId() + "'.", e );

            return false;
        }

        getLogger().info(
            "Recreating Maven2 metadata in repository ID='" + getId() + "' from path='" + request.getRequestPath()
                + "'" );

        return doRecreateMavenMetadata( request );
    }

    protected boolean doRecreateMavenMetadata( ResourceStoreRequest request )
    {
        RecreateMavenMetadataWalkerProcessor wp = new RecreateMavenMetadataWalkerProcessor( this.getLogger() );

        DefaultWalkerContext ctx = new DefaultWalkerContext( this, request );

        ctx.getProcessors().add( wp );

        try
        {
            getWalker().walk( ctx );
        }
        catch ( WalkerException e )
        {
            if ( !( e.getWalkerContext().getStopCause() instanceof ItemNotFoundException ) )
            {
                // everything that is not ItemNotFound should be reported,
                // otherwise just neglect it
                throw e;
            }
        }

        eventBus().post( new RepositoryEventRecreateMavenMetadata( this ) );

        return !ctx.isStopped();
    }

    public boolean isDownloadRemoteIndexes()
    {
        return getExternalConfiguration( false ).isDownloadRemoteIndex();
    }

    public void setDownloadRemoteIndexes( boolean downloadRemoteIndexes )
    {
        boolean oldValue = isDownloadRemoteIndexes();
        boolean newValue = downloadRemoteIndexes;

        getExternalConfiguration( true ).setDownloadRemoteIndex( downloadRemoteIndexes );

        if ( oldValue == false && newValue == true )
        {
            this.downloadRemoteIndexEnabled = true;
        }
    }

    public RepositoryPolicy getRepositoryPolicy()
    {
        return getExternalConfiguration( false ).getRepositoryPolicy();
    }

    public void setRepositoryPolicy( RepositoryPolicy repositoryPolicy )
    {
        getExternalConfiguration( true ).setRepositoryPolicy( repositoryPolicy );
    }

    public boolean isCleanseRepositoryMetadata()
    {
        return getExternalConfiguration( false ).isCleanseRepositoryMetadata();
    }

    public void setCleanseRepositoryMetadata( boolean cleanseRepositoryMetadata )
    {
        getExternalConfiguration( true ).setCleanseRepositoryMetadata( cleanseRepositoryMetadata );
    }

    public ChecksumPolicy getChecksumPolicy()
    {
        return getExternalConfiguration( false ).getChecksumPolicy();
    }

    public void setChecksumPolicy( ChecksumPolicy checksumPolicy )
    {
        getExternalConfiguration( true ).setChecksumPolicy( checksumPolicy );
    }

    public int getArtifactMaxAge()
    {
        return getExternalConfiguration( false ).getArtifactMaxAge();
    }

    public void setArtifactMaxAge( int maxAge )
    {
        getExternalConfiguration( true ).setArtifactMaxAge( maxAge );
    }

    public int getMetadataMaxAge()
    {
        return getExternalConfiguration( false ).getMetadataMaxAge();
    }

    public void setMetadataMaxAge( int metadataMaxAge )
    {
        getExternalConfiguration( true ).setMetadataMaxAge( metadataMaxAge );
    }

    public boolean isMavenArtifact( StorageItem item )
    {
        return isMavenArtifactPath( item.getPath() );
    }

    public boolean isMavenMetadata( StorageItem item )
    {
        return isMavenMetadataPath( item.getPath() );
    }

    public boolean isMavenArtifactPath( String path )
    {
        return getGavCalculator().pathToGav( path ) != null;
    }

    public abstract boolean isMavenMetadataPath( String path );

    public abstract boolean shouldServeByPolicies( ResourceStoreRequest request );

    public void storeItemWithChecksums( ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "storeItemWithChecksums() :: " + request.getRequestPath() );
        }

        getArtifactStoreHelper().storeItemWithChecksums( request, is, userAttributes );
    }

    public void deleteItemWithChecksums( ResourceStoreRequest request )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
        StorageException, AccessDeniedException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "deleteItemWithChecksums() :: " + request.getRequestPath() );
        }

        getArtifactStoreHelper().deleteItemWithChecksums( request );
    }

    public void storeItemWithChecksums( boolean fromTask, AbstractStorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "storeItemWithChecksums() :: " + item.getRepositoryItemUid().toString() );
        }

        getArtifactStoreHelper().storeItemWithChecksums( fromTask, item );
    }

    public void deleteItemWithChecksums( boolean fromTask, ResourceStoreRequest request )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "deleteItemWithChecksums() :: " + request.toString() );
        }

        getArtifactStoreHelper().deleteItemWithChecksums( fromTask, request );
    }

    public MetadataManager getMetadataManager()
    {
        return metadataManager;
    }

    // =================================================================================
    // DefaultRepository customizations
    @Override
    protected StorageItem doRetrieveItem( ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        if ( !shouldServeByPolicies( request ) )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "The serving of item " + request.toString() + " is forbidden by Maven repository policy." );
            }

            throw new ItemNotFoundException( request, this );
        }

        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class )
            && !request.getRequestPath().startsWith( "/." ) )
        {
            if ( request.getRequestPath().endsWith( SUFFIX_SHA1 ) )
            {
                return doRetrieveSHA1( this, request, doRetrieveArtifactItem( request, SUFFIX_SHA1 ) ).getHashItem();
            }

            if ( request.getRequestPath().endsWith( SUFFIX_MD5 ) )
            {
                return doRetrieveMD5( this, request, doRetrieveArtifactItem( request, SUFFIX_MD5 ) ).getHashItem();
            }
        }

        return super.doRetrieveItem( request );
    }

    /**
     * Retrieves artifact corresponding to .sha1/.md5 request (or any request suffix).
     */
    private StorageItem doRetrieveArtifactItem( ResourceStoreRequest hashRequest, String suffix )
        throws ItemNotFoundException, StorageException, IllegalOperationException
    {
        final String hashPath = hashRequest.getRequestPath();
        final String itemPath = hashPath.substring( 0, hashPath.length() - suffix.length() );
        hashRequest.pushRequestPath( itemPath );
        try
        {
            return super.doRetrieveItem( hashRequest );
        }
        finally
        {
            hashRequest.popRequestPath();
        }
    }

    @Override
    protected boolean shouldTryRemote( final ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException
    {
        final boolean shouldTryRemote = super.shouldTryRemote( request );
        if ( !shouldTryRemote )
        {
            return false;
        }
        // apply WLFilter to "normal" requests only, not hidden (which is meta or plain hidden)
        final RepositoryItemUid uid = createUid( request.getRequestPath() );
        if ( !uid.getBooleanAttributeValue( IsHiddenAttribute.class ) )
        {
            // but filter it only if request is not marked as NFS
            if ( !request.getRequestContext().containsKey( WLManager.WL_REQUEST_NFS_FLAG_KEY ) )
            {
                final boolean whitelistMatched = getProxyRequestFilter().allowed( this, request );
                if ( !whitelistMatched )
                {
                    getLogger().debug( "WL filter rejected remote request for path {} in {}.",
                        request.getRequestPath(), RepositoryStringUtils.getHumanizedNameString( this ) );
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void storeItem( boolean fromTask, StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
    {
        if ( shouldServeByPolicies( new ResourceStoreRequest( item ) ) )
        {
            super.storeItem( fromTask, item );
        }
        else
        {
            String msg =
                "Storing of item " + item.getRepositoryItemUid().toString()
                    + " is forbidden by Maven Repository policy. Because " + getId() + " is a "
                    + getRepositoryPolicy().name() + " repository";

            getLogger().info( msg );

            throw new UnsupportedStorageOperationException( msg );
        }
    }

    @Override
    public boolean isCompatible( Repository repository )
    {
        if ( super.isCompatible( repository ) && MavenRepository.class.isAssignableFrom( repository.getClass() )
            && getRepositoryPolicy().equals( ( (MavenRepository) repository ).getRepositoryPolicy() ) )
        {
            return true;
        }

        return false;
    }

    // =================================================================================
    // DefaultRepository customizations

    @Override
    protected Collection<StorageItem> doListItems( ResourceStoreRequest request )
        throws ItemNotFoundException, StorageException
    {
        Collection<StorageItem> items = super.doListItems( request );
        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            Map<String, StorageItem> result = new TreeMap<String, StorageItem>();
            for ( StorageItem item : items )
            {
                putChecksumItem( result, request, item, ATTR_REMOTE_SHA1, SUFFIX_SHA1 );
                putChecksumItem( result, request, item, ATTR_REMOTE_MD5, SUFFIX_MD5 );
            }

            for ( StorageItem item : items )
            {
                if ( !result.containsKey( item.getPath() ) )
                {
                    result.put( item.getPath(), item );
                }
            }

            items = result.values();
        }
        return items;
    }

    private void putChecksumItem( Map<String, StorageItem> checksums, ResourceStoreRequest request,
                                  StorageItem artifact, String attrname, String suffix )
    {
        String hash = artifact.getRepositoryItemAttributes().get( attrname );
        if ( hash != null )
        {
            String hashPath = artifact.getPath() + suffix;
            request.pushRequestPath( hashPath );
            try
            {
                checksums.put( hashPath, newHashItem( this, request, artifact, hash ) );
            }
            finally
            {
                request.popRequestPath();
            }
        }
    }

    /**
     * Beside original behavior, only add to NFC when it's not WL that rejected remote access.
     * 
     * @since 2.4
     */
    @Override
    protected boolean shouldAddToNotFoundCache( final ResourceStoreRequest request )
    {
        boolean shouldAddToNFC = super.shouldAddToNotFoundCache( request );
        if ( shouldAddToNFC && request.getRequestContext().containsKey( WLManager.WL_REQUEST_REJECTED_FLAG_KEY ) )
        {
            // TODO: should we un-flag the request?
            shouldAddToNFC = false;
            getLogger().debug( "Maven proxy repository {} WL rejected this request, not adding path {} to NFC.",
                RepositoryStringUtils.getHumanizedNameString( this ), request.getRequestPath() );
        }
        return shouldAddToNFC;
    }
}
