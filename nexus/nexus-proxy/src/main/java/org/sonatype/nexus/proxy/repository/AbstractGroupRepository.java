/**
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
package org.sonatype.nexus.proxy.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.configuration.ConfigurationPrepareForSaveEvent;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.events.RepositoryEventEvictUnusedItems;
import org.sonatype.nexus.proxy.events.RepositoryGroupMembersChangedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsGroupLocalOnlyAttribute;
import org.sonatype.nexus.proxy.mapping.RequestRepositoryMapper;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.charger.ChargerHolder;
import org.sonatype.nexus.proxy.repository.charger.GroupItemRetrieveCallable;
import org.sonatype.nexus.proxy.repository.charger.ItemRetrieveCallable;
import org.sonatype.nexus.proxy.repository.threads.ThreadPoolManager;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.sisu.charger.CallableExecutor;
import org.sonatype.sisu.charger.internal.AllArrivedChargeStrategy;
import org.sonatype.sisu.charger.internal.FirstArrivedInOrderChargeStrategy;

/**
 * An abstract group repository. The specific behaviour (ie. metadata merge) should be implemented in subclases.
 * 
 * @author cstamas
 */
public abstract class AbstractGroupRepository
    extends AbstractRepository
    implements GroupRepository, CallableExecutor
{
    /** Secret switch that allows disabling use of Charger if needed */
    private final boolean USE_CHARGER_FOR_GROUP_REQUESTS = SystemPropertiesHelper.getBoolean( getClass().getName()
        + ".useParallelGroupRequests", false );

    @Requirement
    private RepositoryRegistry repoRegistry;

    @Requirement
    private RequestRepositoryMapper requestRepositoryMapper;

    @Requirement
    private ChargerHolder chargerHolder;

    @Requirement
    private ThreadPoolManager poolManager;

    @Override
    protected AbstractGroupRepositoryConfiguration getExternalConfiguration( boolean forWrite )
    {
        return (AbstractGroupRepositoryConfiguration) super.getExternalConfiguration( forWrite );
    }

    @Override
    public void onEvent( Event<?> evt )
    {
        // we must do this before the super.onEvent() call!
        boolean membersChanged =
            getCurrentCoreConfiguration().isDirty()
                && ( getExternalConfiguration( false ).getMemberRepositoryIds().size() != getExternalConfiguration(
                    true ).getMemberRepositoryIds().size() || !getExternalConfiguration( false ).getMemberRepositoryIds().containsAll(
                    getExternalConfiguration( true ).getMemberRepositoryIds() ) );

        List<String> currentMemberIds = Collections.emptyList();
        List<String> newMemberIds = Collections.emptyList();
        // we have to "remember" these before commit happens in super.onEvent
        // but ONLY if we are dirty and we do have "member changes" (see membersChanged above)
        // this same boolean drives the firing of the event too, for which we are actually collecting these lists
        // if no event to be fired, these lines should also not execute, since they are "dirtying" the config
        // if membersChange is true, config is already dirty, and we DO KNOW there is member change to happen
        // and we will fire the event too
        if ( membersChanged )
        {
            currentMemberIds = getExternalConfiguration( false ).getMemberRepositoryIds();
            newMemberIds = getExternalConfiguration( true ).getMemberRepositoryIds();
        }

        super.onEvent( evt );

        // act automatically on repo removal. Remove it from myself if member.
        if ( evt instanceof RepositoryRegistryEventRemove )
        {
            RepositoryRegistryEventRemove revt = (RepositoryRegistryEventRemove) evt;

            if ( this.getExternalConfiguration( false ).getMemberRepositoryIds().contains( revt.getRepository().getId() ) )
            {
                removeMemberRepositoryId( revt.getRepository().getId() );
            }
        }
        else if ( evt instanceof ConfigurationPrepareForSaveEvent && membersChanged )
        {
            // fire another event
            getApplicationEventMulticaster().notifyEventListeners(
                new RepositoryGroupMembersChangedEvent( this, currentMemberIds, newMemberIds ) );
        }
    }

    @Override
    public <T> Future<T> submit( Callable<T> task )
    {
        return poolManager.getRepositoryThreadPool( this ).submit( task );
    }

    @Override
    public void expireCaches( ResourceStoreRequest request )
    {
        final List<Repository> members = getMemberRepositories();
        for ( Repository member : members )
        {
            member.expireCaches( request );
        }

        super.expireCaches( request );
    }

    @Override
    public Collection<String> evictUnusedItems( ResourceStoreRequest request, final long timestamp )
    {
        if ( !getLocalStatus().shouldServiceRequest() )
        {
            return Collections.emptyList();
        }

        getLogger().info(
            String.format( "Evicting unused items from group repository %s from path \"%s\"",
                RepositoryStringUtils.getHumanizedNameString( this ), request.getRequestPath() ) );

        HashSet<String> result = new HashSet<String>();

        // here, we just iterate over members and call evict
        final List<Repository> members = getMemberRepositories();
        for ( Repository repository : members )
        {
            result.addAll( repository.evictUnusedItems( request, timestamp ) );
        }

        getApplicationEventMulticaster().notifyEventListeners( new RepositoryEventEvictUnusedItems( this ) );

        return result;
    }

    // ==

    @Override
    protected Collection<StorageItem> doListItems( ResourceStoreRequest request )
        throws ItemNotFoundException, StorageException
    {
        HashSet<String> names = new HashSet<String>();
        ArrayList<StorageItem> result = new ArrayList<StorageItem>();
        boolean found = false;
        try
        {
            addItems( names, result, getLocalStorage().listItems( this, request ) );

            found = true;
        }
        catch ( ItemNotFoundException ignored )
        {
            // ignored
        }

        RepositoryItemUid uid = createUid( request.getRequestPath() );

        final boolean isRequestGroupLocalOnly =
            request.isRequestGroupLocalOnly() || uid.getBooleanAttributeValue( IsGroupLocalOnlyAttribute.class );

        if ( !isRequestGroupLocalOnly )
        {
            for ( Repository repo : getMemberRepositories() )
            {
                if ( !request.getProcessedRepositories().contains( repo.getId() ) )
                {
                    try
                    {
                        addItems( names, result, repo.list( false, request ) );

                        found = true;
                    }
                    catch ( ItemNotFoundException e )
                    {
                        // ignored
                    }
                    catch ( IllegalOperationException e )
                    {
                        // ignored
                    }
                    catch ( StorageException e )
                    {
                        // ignored
                    }
                }
                else
                {
                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug(
                            String.format(
                                "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                                RepositoryStringUtils.getHumanizedNameString( repo ),
                                RepositoryStringUtils.getHumanizedNameString( this ), request.toString() ) );
                    }
                }
            }
        }

        if ( !found )
        {
            throw new ItemNotFoundException( request, this );
        }

        return result;
    }

    private static void addItems( HashSet<String> names, ArrayList<StorageItem> result,
                                  Collection<StorageItem> listItems )
    {
        for ( StorageItem item : listItems )
        {
            if ( names.add( item.getPath() ) )
            {
                result.add( item );
            }
        }
    }

    @Override
    protected StorageItem doRetrieveItem( ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        try
        {
            // local always wins
            return super.doRetrieveItem( request );
        }
        catch ( ItemNotFoundException ignored )
        {
            // ignored
        }

        boolean hasRequestAuthorizedFlag = request.getRequestContext().containsKey( AccessManager.REQUEST_AUTHORIZED );

        if ( !hasRequestAuthorizedFlag )
        {
            request.getRequestContext().put( AccessManager.REQUEST_AUTHORIZED, Boolean.TRUE );
        }

        try
        {
            RepositoryItemUid uid = createUid( request.getRequestPath() );

            final boolean isRequestGroupLocalOnly =
                request.isRequestGroupLocalOnly() || uid.getBooleanAttributeValue( IsGroupLocalOnlyAttribute.class );

            if ( !isRequestGroupLocalOnly )
            {
                if ( USE_CHARGER_FOR_GROUP_REQUESTS )
                {
                    List<Callable<StorageItem>> callables = new ArrayList<Callable<StorageItem>>();

                    for ( Repository repository : getRequestRepositories( request ) )
                    {
                        if ( !request.getProcessedRepositories().contains( repository.getId() ) )
                        {
                            callables.add( new GroupItemRetrieveCallable( getLogger(), repository, request, this ) );
                        }
                        else
                        {
                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug(
                                    String.format(
                                        "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                                        RepositoryStringUtils.getHumanizedNameString( repository ),
                                        RepositoryStringUtils.getHumanizedNameString( this ), request.toString() ) );
                            }
                        }
                    }

                    try
                    {
                        List<StorageItem> items =
                            chargerHolder.getCharger().submit( callables,
                                new FirstArrivedInOrderChargeStrategy<StorageItem>(), this ).getResult();

                        if ( items.size() > 0 )
                        {
                            return items.get( 0 );
                        }
                    }
                    catch ( RejectedExecutionException e )
                    {
                        // this will not happen
                    }
                    catch ( Exception e )
                    {
                        // will not happen, see GroupItemRetrieveCallable class' javadoc, it supresses all of them
                        // to make compiler happy
                        throw new LocalStorageException( "Ouch!", e );
                    }
                }
                else
                {
                    for ( Repository repo : getRequestRepositories( request ) )
                    {
                        if ( !request.getProcessedRepositories().contains( repo.getId() ) )
                        {
                            try
                            {
                                StorageItem item = repo.retrieveItem( request );

                                if ( item instanceof StorageCollectionItem )
                                {
                                    item = new DefaultStorageCollectionItem( this, request, true, false );
                                }

                                return item;
                            }
                            catch ( IllegalOperationException e )
                            {
                                // ignored
                            }
                            catch ( ItemNotFoundException e )
                            {
                                // ignored
                            }
                            catch ( StorageException e )
                            {
                                // ignored
                            }
                            catch ( AccessDeniedException e )
                            {
                                // cannot happen, since we add/check for AccessManager.REQUEST_AUTHORIZED flag
                            }
                        }
                        else
                        {
                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug(
                                    String.format(
                                        "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                                        RepositoryStringUtils.getHumanizedNameString( repo ),
                                        RepositoryStringUtils.getHumanizedNameString( this ), request.toString() ) );
                            }
                        }
                    }
                }
            }
        }
        finally
        {
            if ( !hasRequestAuthorizedFlag )
            {
                request.getRequestContext().remove( AccessManager.REQUEST_AUTHORIZED );
            }
        }

        throw new ItemNotFoundException( request, this );
    }

    public List<String> getMemberRepositoryIds()
    {
        ArrayList<String> result =
            new ArrayList<String>( getExternalConfiguration( false ).getMemberRepositoryIds().size() );

        for ( String id : getExternalConfiguration( false ).getMemberRepositoryIds() )
        {
            result.add( id );
        }

        return Collections.unmodifiableList( result );
    }

    public void setMemberRepositoryIds( List<String> repositories )
        throws NoSuchRepositoryException, InvalidGroupingException
    {
        getExternalConfiguration( true ).clearMemberRepositoryIds();

        for ( String repoId : repositories )
        {
            addMemberRepositoryId( repoId );
        }
    }

    public void addMemberRepositoryId( String repositoryId )
        throws NoSuchRepositoryException, InvalidGroupingException
    {
        // validate THEN modify
        // this will throw NoSuchRepository if needed
        Repository repo = repoRegistry.getRepository( repositoryId );

        // check for cycles
        List<String> memberIds = new ArrayList<String>( getExternalConfiguration( false ).getMemberRepositoryIds() );
        memberIds.add( repo.getId() );
        checkForCyclicReference( getId(), memberIds, getId() );

        // check for compatibility
        if ( !repo.getRepositoryContentClass().isCompatible( getRepositoryContentClass() ) )
        {
            throw new InvalidGroupingException( getRepositoryContentClass(), repo.getRepositoryContentClass() );
        }

        // if we are here, all is well
        getExternalConfiguration( true ).addMemberRepositoryId( repo.getId() );
    }

    private void checkForCyclicReference( final String id, List<String> memberRepositoryIds, String path )
        throws InvalidGroupingException
    {
        if ( memberRepositoryIds.contains( id ) )
        {
            throw new InvalidGroupingException( id, path );
        }

        for ( String memberId : memberRepositoryIds )
        {
            try
            {
                GroupRepository group = repoRegistry.getRepositoryWithFacet( memberId, GroupRepository.class );
                checkForCyclicReference( id, group.getMemberRepositoryIds(), path + '/' + memberId );
            }
            catch ( NoSuchRepositoryException e )
            {
                // not a group repo, just ignore
            }
        }
    }

    public void removeMemberRepositoryId( String repositoryId )
    {
        getExternalConfiguration( true ).removeMemberRepositoryId( repositoryId );
    }

    public List<Repository> getMemberRepositories()
    {
        ArrayList<Repository> result = new ArrayList<Repository>();

        for ( String repoId : getMemberRepositoryIds() )
        {
            try
            {
                Repository repo = repoRegistry.getRepository( repoId );
                result.add( repo );
            }
            catch ( NoSuchRepositoryException e )
            {
                this.getLogger().warn( "Could not find repository: " + repoId, e );
                // XXX throw new StorageException( e );
            }
        }

        return result;
    }

    protected List<Repository> getRequestRepositories( ResourceStoreRequest request )
        throws StorageException
    {
        List<Repository> members = getMemberRepositories();

        try
        {
            return requestRepositoryMapper.getMappedRepositories( this, request, members );
        }
        catch ( NoSuchResourceStoreException e )
        {
            throw new LocalStorageException( e );
        }
    }

    public List<StorageItem> doRetrieveItems( ResourceStoreRequest request )
        throws StorageException
    {
        ArrayList<StorageItem> items = new ArrayList<StorageItem>();

        RepositoryItemUid uid = createUid( request.getRequestPath() );

        final boolean isRequestGroupLocalOnly =
            request.isRequestGroupLocalOnly() || uid.getBooleanAttributeValue( IsGroupLocalOnlyAttribute.class );

        if ( !isRequestGroupLocalOnly )
        {
            if ( USE_CHARGER_FOR_GROUP_REQUESTS )
            {
                List<Callable<StorageItem>> callables = new ArrayList<Callable<StorageItem>>();

                for ( Repository repository : getRequestRepositories( request ) )
                {
                    if ( !request.getProcessedRepositories().contains( repository.getId() ) )
                    {
                        callables.add( new ItemRetrieveCallable( getLogger(), repository, request ) );
                    }
                    else
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug(
                                String.format(
                                    "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                                    RepositoryStringUtils.getHumanizedNameString( repository ),
                                    RepositoryStringUtils.getHumanizedNameString( this ), request.toString() ) );
                        }
                    }
                }

                try
                {
                    return chargerHolder.getCharger().submit( callables, new AllArrivedChargeStrategy<StorageItem>(),
                        this ).getResult();
                }
                catch ( RejectedExecutionException e )
                {
                    // this will not happen
                }
                catch ( StorageException e )
                {
                    throw e;
                }
                catch ( Exception e )
                {
                    // will not happen, ItemRetrieveCallable supresses all except StorageException!
                    // just to make compiler happy
                    throw new LocalStorageException( "Ouch!", e );
                }
            }
            else
            {
                for ( Repository repository : getRequestRepositories( request ) )
                {
                    if ( !request.getProcessedRepositories().contains( repository.getId() ) )
                    {
                        try
                        {
                            StorageItem item = repository.retrieveItem( false, request );

                            items.add( item );
                        }
                        catch ( ItemNotFoundException e )
                        {
                            // that's okay
                        }
                        catch ( RepositoryNotAvailableException e )
                        {
                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug(
                                    RepositoryStringUtils.getFormattedMessage(
                                        "Member repository %s is not available, request failed.", e.getRepository() ) );
                            }
                        }
                        catch ( StorageException e )
                        {
                            throw e;
                        }
                        catch ( IllegalOperationException e )
                        {
                            getLogger().warn( "Member repository request failed", e );
                        }
                    }
                    else
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug(
                                String.format(
                                    "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                                    RepositoryStringUtils.getHumanizedNameString( repository ),
                                    RepositoryStringUtils.getHumanizedNameString( this ), request.toString() ) );
                        }
                    }
                }
            }
        }

        return items;
    }

    // ===================================================================================
    // Inner stuff

    @Override
    public void maintainNotFoundCache( ResourceStoreRequest request )
        throws ItemNotFoundException
    {
        // just maintain the cache (ie. expiration), but don't make NFC
        // affect call delegation to members
        try
        {
            super.maintainNotFoundCache( request );
        }
        catch ( ItemNotFoundException e )
        {
            // ignore it
        }
    }

    @Override
    public List<Repository> getTransitiveMemberRepositories()
    {
        return getTransitiveMemberRepositories( this );
    }

    protected List<Repository> getTransitiveMemberRepositories( GroupRepository group )
    {
        List<Repository> repos = new ArrayList<Repository>();
        for ( Repository repo : group.getMemberRepositories() )
        {
            if ( repo.getRepositoryKind().isFacetAvailable( GroupRepository.class ) )
            {
                repos.addAll( getTransitiveMemberRepositories( repo.adaptToFacet( GroupRepository.class ) ) );
            }
            else
            {
                repos.add( repo );
            }
        }
        return repos;
    }

    @Override
    public List<String> getTransitiveMemberRepositoryIds()
    {
        List<String> ids = new ArrayList<String>();
        for ( Repository repo : getTransitiveMemberRepositories() )
        {
            ids.add( repo.getId() );
        }
        return ids;
    }

}
