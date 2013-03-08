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
package org.sonatype.nexus.proxy.router;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractConfigurable;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRouting;
import org.sonatype.nexus.configuration.model.CRoutingCoreConfiguration;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.target.TargetSet;
import org.sonatype.nexus.util.ItemPathUtils;

/**
 * The simplest re-implementation for RepositoryRouter that does only routing.
 * 
 * @author cstamas
 */
@Component( role = RepositoryRouter.class )
public class DefaultRepositoryRouter
    extends AbstractConfigurable
    implements RepositoryRouter
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement
    private ApplicationConfiguration applicationConfiguration;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    @Requirement
    private NexusItemAuthorizer itemAuthorizer;

    public boolean isFollowLinks()
    {
        return getCurrentConfiguration( false ).isResolveLinks();
    }

    public void setFollowLinks( boolean followLinks )
    {
        getCurrentConfiguration( true ).setResolveLinks( followLinks );
    }

    // =

    protected void initializeConfiguration()
        throws ConfigurationException
    {
        if ( getApplicationConfiguration().getConfigurationModel() != null )
        {
            configure( getApplicationConfiguration() );
        }
    }

    @Override
    protected ApplicationConfiguration getApplicationConfiguration()
    {
        return applicationConfiguration;
    }

    @Override
    protected Configurator getConfigurator()
    {
        return null;
    }

    @Override
    protected CRouting getCurrentConfiguration( boolean forWrite )
    {
        return ( (CRoutingCoreConfiguration) getCurrentCoreConfiguration() ).getConfiguration( forWrite );
    }

    @Override
    protected CoreConfiguration wrapConfiguration( Object configuration )
        throws ConfigurationException
    {
        if ( configuration instanceof ApplicationConfiguration )
        {
            return new CRoutingCoreConfiguration( (ApplicationConfiguration) configuration );
        }
        else
        {
            throw new ConfigurationException( "The passed configuration object is of class \""
                + configuration.getClass().getName() + "\" and not the required \""
                + ApplicationConfiguration.class.getName() + "\"!" );
        }
    }

    // =

    public StorageItem dereferenceLink( StorageLinkItem link )
        throws AccessDeniedException, ItemNotFoundException, IllegalOperationException, StorageException
    {
        return dereferenceLink( link, false, false );
    }

    public StorageItem dereferenceLink( StorageLinkItem link, boolean localOnly, boolean remoteOnly )
        throws AccessDeniedException, ItemNotFoundException, IllegalOperationException, StorageException
    {
        logger.debug( "Dereferencing link {}", link.getTarget() );

        ResourceStoreRequest req = new ResourceStoreRequest( link.getTarget().getPath(), localOnly, remoteOnly );

        req.getRequestContext().setParentContext( link.getItemContext() );

        return link.getTarget().getRepository().retrieveItem( req );
    }

    public StorageItem retrieveItem( ResourceStoreRequest request )
        throws ItemNotFoundException, IllegalOperationException, StorageException, AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            StorageItem item;

            try
            {
                request.pushRequestPath( route.getRepositoryPath() );

                item = route.getTargetedRepository().retrieveItem( request );
            }
            finally
            {
                request.popRequestPath();
            }

            return mangle( false, request, route, item );
        }
        else
        {
            // this is "above" repositories
            return retrieveVirtualPath( request, route );
        }
    }

    public void storeItem( ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes )
        throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
        StorageException, AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            try
            {
                request.pushRequestPath( route.getRepositoryPath() );

                route.getTargetedRepository().storeItem( request, is, userAttributes );
            }
            finally
            {
                request.popRequestPath();
            }
        }
        else
        {
            // this is "above" repositories
            throw new IllegalRequestException( request, "The path '" + request.getRequestPath()
                + "' does not points to any repository!" );
        }
    }

    public void copyItem( ResourceStoreRequest from, ResourceStoreRequest to )
        throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
        StorageException, AccessDeniedException
    {
        RequestRoute fromRoute = getRequestRouteForRequest( from );

        RequestRoute toRoute = getRequestRouteForRequest( to );

        if ( fromRoute.isRepositoryHit() && toRoute.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            try
            {
                from.pushRequestPath( fromRoute.getRepositoryPath() );
                to.pushRequestPath( toRoute.getRepositoryPath() );

                if ( fromRoute.getTargetedRepository() == toRoute.getTargetedRepository() )
                {
                    fromRoute.getTargetedRepository().copyItem( from, to );
                }
                else
                {
                    StorageItem item = fromRoute.getTargetedRepository().retrieveItem( from );

                    if ( item instanceof StorageFileItem )
                    {
                        try
                        {
                            toRoute.getTargetedRepository().storeItem( to, ( (StorageFileItem) item ).getInputStream(),
                                item.getRepositoryItemAttributes().asMap() );
                        }
                        catch ( IOException e )
                        {
                            // XXX: this is nonsense, to box IOException into subclass of IOException!
                            throw new LocalStorageException( e );
                        }
                    }
                    else if ( item instanceof StorageCollectionItem )
                    {
                        toRoute.getTargetedRepository().createCollection( to, item.getRepositoryItemAttributes().asMap() );
                    }
                    else
                    {
                        throw new IllegalRequestException( from, "Cannot copy item of class='"
                            + item.getClass().getName() + "' over multiple repositories." );
                    }

                }
            }
            finally
            {
                from.popRequestPath();
                to.popRequestPath();
            }
        }
        else
        {
            // this is "above" repositories
            if ( !fromRoute.isRepositoryHit() )
            {
                throw new IllegalRequestException( from, "The path '" + from.getRequestPath()
                    + "' does not points to any repository!" );
            }
            else
            {
                throw new IllegalRequestException( to, "The path '" + to.getRequestPath()
                    + "' does not points to any repository!" );
            }
        }
    }

    public void moveItem( ResourceStoreRequest from, ResourceStoreRequest to )
        throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
        StorageException, AccessDeniedException
    {
        copyItem( from, to );

        deleteItem( from );
    }

    public Collection<StorageItem> list( ResourceStoreRequest request )
        throws ItemNotFoundException, IllegalOperationException, StorageException, AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            Collection<StorageItem> items;

            try
            {
                request.pushRequestPath( route.getRepositoryPath() );

                items = route.getTargetedRepository().list( request );
            }
            finally
            {
                request.popRequestPath();
            }

            ArrayList<StorageItem> result = new ArrayList<StorageItem>( items.size() );

            for ( StorageItem item : items )
            {
                result.add( mangle( true, request, route, item ) );
            }

            return result;
        }
        else
        {
            // this is "above" repositories
            return listVirtualPath( request, route );
        }
    }

    public void createCollection( ResourceStoreRequest request, Map<String, String> userAttributes )
        throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
        StorageException, AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            try
            {
                request.pushRequestPath( route.getRepositoryPath() );

                route.getTargetedRepository().createCollection( request, userAttributes );
            }
            finally
            {
                request.popRequestPath();
            }
        }
        else
        {
            // this is "above" repositories
            throw new IllegalRequestException( request, "The path '" + request.getRequestPath()
                + "' does not points to any repository!" );
        }
    }

    public void deleteItem( ResourceStoreRequest request )
        throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
        StorageException, AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            try
            {
                request.pushRequestPath( route.getRepositoryPath() );

                route.getTargetedRepository().deleteItem( request );
            }
            finally
            {
                request.popRequestPath();
            }
        }
        else
        {
            // this is "above" repositories
            throw new IllegalRequestException( request, "The path '" + request.getRequestPath()
                + "' does not points to any repository!" );
        }
    }

    public TargetSet getTargetsForRequest( ResourceStoreRequest request )
    {
        TargetSet result = new TargetSet();

        try
        {
            RequestRoute route = getRequestRouteForRequest( request );

            if ( route.isRepositoryHit() )
            {
                // it hits a repository, mangle path and call it

                try
                {
                    request.pushRequestPath( route.getRepositoryPath() );

                    result.addTargetSet( route.getTargetedRepository().getTargetsForRequest( request ) );
                }
                finally
                {
                    request.popRequestPath();
                }
            }
        }
        catch ( ItemNotFoundException e )
        {
            // nothing, empty set will be returned
        }

        return result;
    }

    // ===
    // Private
    // ===

    protected StorageItem mangle( boolean isList, ResourceStoreRequest request, RequestRoute route, StorageItem item )
        throws AccessDeniedException, ItemNotFoundException, IllegalOperationException, StorageException
    {
        if ( isList )
        {
            ( (AbstractStorageItem) item ).setPath( ItemPathUtils.concatPaths( route.getOriginalRequestPath(),
                item.getName() ) );
        }
        else
        {
            ( (AbstractStorageItem) item ).setPath( route.getOriginalRequestPath() );
        }

        if ( isFollowLinks() && item instanceof StorageLinkItem )
        {
            return dereferenceLink( (StorageLinkItem) item );
        }
        else
        {
            return item;
        }
    }

    // XXX: a todo here is to make the "aliases" ("groups" for GroupRepository.class) dynamic,
    // and even think about new layout: every kind should have it's own "folder", you don't want to see
    // maven2 and P2 repositories along each other...
    public RequestRoute getRequestRouteForRequest( ResourceStoreRequest request )
        throws ItemNotFoundException
    {
        RequestRoute result = new RequestRoute();

        result.setOriginalRequestPath( request.getRequestPath() );

        result.setResourceStoreRequest( request );

        String correctedPath =
            request.getRequestPath().startsWith( RepositoryItemUid.PATH_SEPARATOR ) ? request.getRequestPath().substring(
                1, request.getRequestPath().length() )
                : request.getRequestPath();

        String[] explodedPath = null;

        if ( StringUtils.isEmpty( correctedPath ) )
        {
            explodedPath = new String[0];
        }
        else
        {
            explodedPath = correctedPath.split( RepositoryItemUid.PATH_SEPARATOR );
        }

        Class<? extends Repository> kind = null;

        result.setRequestDepth( explodedPath.length );

        if ( explodedPath.length >= 1 )
        {
            // we have kind information ("repositories" vs "groups" etc)
            for ( RepositoryTypeDescriptor rtd : repositoryTypeRegistry.getRegisteredRepositoryTypeDescriptors() )
            {
                if ( rtd.getPrefix().equals( explodedPath[0] ) )
                {
                    kind = rtd.getRole();

                    break;
                }
            }

            if ( kind == null )
            {
                // unknown explodedPath[0]
                throw new ItemNotFoundException( request );
            }

            result.setStrippedPrefix( ItemPathUtils.concatPaths( explodedPath[0] ) );
        }

        if ( explodedPath.length >= 2 )
        {
            // we have repoId information in path
            Repository repository = null;

            try
            {
                repository = getRepositoryForPathPrefixOrId( explodedPath[1], kind );
                // explodedPath[1] is not _always_ ID anymore! It is PathPrefix _or_ ID! NEXUS-1710
                // repository = repositoryRegistry.getRepositoryWithFacet( explodedPath[1], kind );

                if ( !repository.isExposed() )
                {
                    // this is not the main facet or the repo is not exposed
                    throw new ItemNotFoundException( request );
                }
            }
            catch ( NoSuchRepositoryException e )
            {
                // obviously, the repoId (explodedPath[1]) points to some nonexistent repoID
                throw new ItemNotFoundException( request, e );
            }

            result.setStrippedPrefix( ItemPathUtils.concatPaths( explodedPath[0], explodedPath[1] ) );

            result.setTargetedRepository( repository );

            String repoPath = "";

            for ( int i = 2; i < explodedPath.length; i++ )
            {
                repoPath = ItemPathUtils.concatPaths( repoPath, explodedPath[i] );
            }

            if ( result.getOriginalRequestPath().endsWith( RepositoryItemUid.PATH_SEPARATOR ) )
            {
                repoPath = repoPath + RepositoryItemUid.PATH_SEPARATOR;
            }

            result.setRepositoryPath( repoPath );
        }

        return result;
    }

    protected Repository getRepositoryForPathPrefixOrId( String pathPrefixOrId, Class<? extends Repository> kind )
        throws NoSuchRepositoryException
    {
        List<? extends Repository> repositories = repositoryRegistry.getRepositoriesWithFacet( kind );

        Repository idMatched = null;

        Repository pathPrefixMatched = null;

        for ( Repository repository : repositories )
        {
            if ( StringUtils.equals( repository.getId(), pathPrefixOrId ) )
            {
                idMatched = repository;
            }

            if ( StringUtils.equals( repository.getPathPrefix(), pathPrefixOrId ) )
            {
                pathPrefixMatched = repository;
            }
        }

        if ( idMatched != null )
        {
            // id wins
            return idMatched;
        }

        if ( pathPrefixMatched != null )
        {
            // if no id found, prefix wins
            return pathPrefixMatched;
        }

        // nothing found
        throw new NoSuchRepositoryException( "pathPrefixOrId: '" + pathPrefixOrId + "'" );
    }

    protected StorageItem retrieveVirtualPath( ResourceStoreRequest request, RequestRoute route )
        throws ItemNotFoundException
    {
        ResourceStoreRequest req = new ResourceStoreRequest( route.getOriginalRequestPath() );

        DefaultStorageCollectionItem result = new DefaultStorageCollectionItem( this, req, true, false );

        result.getItemContext().putAll( request.getRequestContext() );

        return result;
    }

    protected Collection<StorageItem> listVirtualPath( ResourceStoreRequest request, RequestRoute route )
        throws ItemNotFoundException
    {
        if ( route.getRequestDepth() == 0 )
        {
            // 1st level
            ArrayList<StorageItem> result = new ArrayList<StorageItem>();

            for ( RepositoryTypeDescriptor rtd : repositoryTypeRegistry.getRegisteredRepositoryTypeDescriptors() )
            {
                // check is there any repo registered
                if ( !repositoryRegistry.getRepositoriesWithFacet( rtd.getRole() ).isEmpty() )
                {
                    ResourceStoreRequest req =
                        new ResourceStoreRequest( ItemPathUtils.concatPaths( request.getRequestPath(), rtd.getPrefix() ) );

                    DefaultStorageCollectionItem repositories =
                        new DefaultStorageCollectionItem( this, req, true, false );

                    repositories.getItemContext().putAll( request.getRequestContext() );

                    result.add( repositories );
                }
            }

            return result;
        }
        else if ( route.getRequestDepth() == 1 )
        {
            // 2nd level
            List<? extends Repository> repositories = null;

            Class<? extends Repository> kind = null;

            for ( RepositoryTypeDescriptor rtd : repositoryTypeRegistry.getRegisteredRepositoryTypeDescriptors() )
            {
                if ( route.getStrippedPrefix().startsWith( "/" + rtd.getPrefix() ) )
                {
                    kind = rtd.getRole();

                    repositories = repositoryRegistry.getRepositoriesWithFacet( kind );

                    break;
                }
            }

            // if no prefix matched, Item not found
            if ( repositories == null || repositories.isEmpty() )
            {
                throw new ItemNotFoundException( request );
            }

            // filter access to the repositories
            // NOTE: do this AFTER the null/empty check so we return an empty list vs. an ItemNotFound
            repositories = filterAccessToRepositories( repositories );

            ArrayList<StorageItem> result = new ArrayList<StorageItem>( repositories.size() );

            for ( Repository repository : repositories )
            {
                if ( repository.isExposed() && repository.isBrowseable() )
                {
                    DefaultStorageCollectionItem repoItem = null;

                    ResourceStoreRequest req = null;

                    if ( Repository.class.equals( kind ) )
                    {
                        req =
                            new ResourceStoreRequest( ItemPathUtils.concatPaths( request.getRequestPath(),
                                repository.getId() ) );
                    }
                    else
                    {
                        req =
                            new ResourceStoreRequest( ItemPathUtils.concatPaths( request.getRequestPath(),
                                repository.getPathPrefix() ) );
                    }

                    repoItem = new DefaultStorageCollectionItem( this, req, true, false );

                    repoItem.getItemContext().putAll( request.getRequestContext() );

                    result.add( repoItem );
                }
            }

            return result;
        }
        else
        {
            throw new ItemNotFoundException( request );
        }
    }

    private List<Repository> filterAccessToRepositories( Collection<? extends Repository> repositories )
    {
        if ( repositories == null )
        {
            return null;
        }

        List<Repository> filteredRepositories = new ArrayList<Repository>();

        for ( Repository repository : repositories )
        {
            if ( this.itemAuthorizer.isViewable( NexusItemAuthorizer.VIEW_REPOSITORY_KEY, repository.getId() ) )
            {
                filteredRepositories.add( repository );
            }
        }

        return filteredRepositories;

    }

    public boolean authorizePath( ResourceStoreRequest request, Action action )
    {
        TargetSet matched = this.getTargetsForRequest( request );

        try
        {
            RequestRoute route = this.getRequestRouteForRequest( request );

            if ( route.getTargetedRepository() != null )
            {
                // if this repository is contained in any group, we need to get those targets, and tweak the TargetMatch
                try
                {
                    request.pushRequestPath( route.getRepositoryPath() );

                    matched.addTargetSet( this.itemAuthorizer.getGroupsTargetSet( route.getTargetedRepository(),
                        request ) );
                }
                finally
                {
                    request.popRequestPath();
                }
            }
        }
        catch ( ItemNotFoundException e )
        {
            // ignore it, do nothing
            
            // cstamas says: above is untrue. It means that user should get 404, but there is no 
            // proper solution to do it from this method! The culprit is that "view privilege" piggybacks
            // on the getRequestRouteForRequest() method that is not meant for this, it does it's job
            // well for routing....
            // So, we still have a bug here (NXCM-3600), and I am leaving it intact, since repo-level
            // security will catch any non authorized ones as well.
        }

        return this.itemAuthorizer.authorizePath( matched, action );
    }

    public String getName()
    {
        return "Repository Router Settings";
    }

}
