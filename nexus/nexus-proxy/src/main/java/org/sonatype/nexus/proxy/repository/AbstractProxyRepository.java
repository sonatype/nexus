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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.ExceptionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteAccessDeniedException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryEventEvictUnusedItems;
import org.sonatype.nexus.proxy.events.RepositoryEventExpireProxyCaches;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeChanged;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeSet;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCacheCreate;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCacheUpdate;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEvent;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.mirror.DefaultDownloadMirrors;
import org.sonatype.nexus.proxy.mirror.DownloadMirrorSelector;
import org.sonatype.nexus.proxy.mirror.DownloadMirrors;
import org.sonatype.nexus.proxy.repository.EvictUnusedItemsWalkerProcessor.EvictUnusedItemsWalkerFilter;
import org.sonatype.nexus.proxy.repository.threads.ThreadPoolManager;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.remote.AbstractHTTPRemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.walker.WalkerFilter;
import org.sonatype.nexus.util.ConstantNumberSequence;
import org.sonatype.nexus.util.FibonacciNumberSequence;
import org.sonatype.nexus.util.NumberSequence;
import org.sonatype.nexus.util.SystemPropertiesHelper;

/**
 * Adds the proxying capability to a simple repository. The proxying will happen only if reposiory has remote storage!
 * So, this implementation is used in both "simple" repository cases: hosted and proxy, but in 1st case there is no
 * remote storage.
 * 
 * @author cstamas
 */
public abstract class AbstractProxyRepository
    extends AbstractRepository
    implements ProxyRepository
{

    /**
     * Default time to do NOT check an already known remote status: 5 mins.
     */
    private static final long DEFAULT_REMOTE_STATUS_RETAIN_TIME = 5L * 60L * 1000L;

    /**
     * The time while we do NOT check an already known remote status
     */
    private static final long REMOTE_STATUS_RETAIN_TIME = SystemPropertiesHelper.getLong(
        "plexus.autoblock.remote.status.retain.time", DEFAULT_REMOTE_STATUS_RETAIN_TIME );

    /**
     * The maximum amount of time to have a repository in AUTOBlock status: 60 minutes (1hr). This value is system
     * default, is used only as limiting point. When repository steps here, it will be checked for remote status hourly
     * only (unless forced by user).
     */
    private static final long AUTO_BLOCK_STATUS_MAX_RETAIN_TIME = 60L * 60L * 1000L;

    @Requirement
    private ThreadPoolManager poolManager;

    /**
     * The remote status checker thread, used in Proxies for handling autoBlocking. Not to go into Pool above, is
     * handled separately.
     */
    private Thread repositoryStatusCheckerThread;

    /**
     * if remote url changed, need special handling after save
     */
    private boolean remoteUrlChanged = false;

    /**
     * The proxy remote status
     */
    private volatile RemoteStatus remoteStatus = RemoteStatus.UNKNOWN;

    /**
     * Last time remote status was updated
     */
    private volatile long remoteStatusUpdated = 0;

    /**
     * How much should be the last known remote status be retained.
     */
    private volatile NumberSequence remoteStatusRetainTimeSequence = new ConstantNumberSequence(
        REMOTE_STATUS_RETAIN_TIME );

    /**
     * The remote storage.
     */
    private RemoteRepositoryStorage remoteStorage;

    /**
     * Remote storage context to store connection configs.
     */
    private RemoteStorageContext remoteStorageContext;

    /**
     * Proxy selector, if set
     */
    private ProxySelector proxySelector;

    /**
     * Download mirrors
     */
    private DownloadMirrors dMirrors;

    /**
     * Item content validators
     */
    private Map<String, ItemContentValidator> itemContentValidators;

    @Override
    protected AbstractProxyRepositoryConfiguration getExternalConfiguration( boolean forModification )
    {
        return (AbstractProxyRepositoryConfiguration) getCurrentCoreConfiguration().getExternalConfiguration().getConfiguration(
            forModification );
    }

    @Override
    public boolean commitChanges()
        throws ConfigurationException
    {
        boolean result = super.commitChanges();

        if ( result )
        {
            this.remoteUrlChanged = false;
        }

        return result;
    }

    @Override
    public boolean rollbackChanges()
    {
        this.remoteUrlChanged = false;

        return super.rollbackChanges();
    }

    @Override
    protected RepositoryConfigurationUpdatedEvent getRepositoryConfigurationUpdatedEvent()
    {
        RepositoryConfigurationUpdatedEvent event = super.getRepositoryConfigurationUpdatedEvent();

        event.setRemoteUrlChanged( this.remoteUrlChanged );

        return event;
    }

    @Override
    public void expireProxyCaches( final ResourceStoreRequest request )
    {
        if ( !getLocalStatus().shouldServiceRequest() )
        {
            return;
        }

        // do this only if we ARE a proxy
        // crawl the local storage (which is in this case proxy cache)
        // and flip the isExpired attribute bits to true
        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            if ( StringUtils.isEmpty( request.getRequestPath() ) )
            {
                request.setRequestPath( RepositoryItemUid.PATH_ROOT );
            }
            request.setRequestLocalOnly( true );

            getLogger().debug(
                String.format( "Expiring proxy cache in repository %s from path=\"%s\"",
                    RepositoryStringUtils.getHumanizedNameString( this ), request.getRequestPath() ) );

            // 1st, expire all the files below path
            final DefaultWalkerContext ctx = new DefaultWalkerContext( this, request );
            final ExpireCacheWalker expireCacheWalkerProcessor = new ExpireCacheWalker( this );
            ctx.getProcessors().add( expireCacheWalkerProcessor );

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
            
            if( getLogger().isDebugEnabled() )
            {
                if( expireCacheWalkerProcessor.isCacheAltered() )
                {
                    getLogger().info(
                        String.format( "Proxy cache was expired for repository %s from path=\"%s\"",
                            RepositoryStringUtils.getHumanizedNameString( this ), request.getRequestPath() ) );
                }
                else
                {
                    getLogger().debug(
                        String.format( "Proxy cache not altered for repository %s from path=\"%s\"",
                            RepositoryStringUtils.getHumanizedNameString( this ), request.getRequestPath() ) );
                }
            }

            // fire off the new event if crawling did end, so we did flip all the bits
            getApplicationEventMulticaster().notifyEventListeners(
                new RepositoryEventExpireProxyCaches( this, request.getRequestPath(),
                    request.getRequestContext().flatten(), expireCacheWalkerProcessor.isCacheAltered() ) );
        }
    }

    @Override
    public void expireCaches( final ResourceStoreRequest request )
    {
        if ( !getLocalStatus().shouldServiceRequest() )
        {
            return;
        }

        // expire proxy cache
        expireProxyCaches( request );
        // do the stuff we inherited
        super.expireCaches( request );
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
                doEvictUnusedItems( request, timestamp, new EvictUnusedItemsWalkerProcessor( timestamp ),
                    new EvictUnusedItemsWalkerFilter() );

            getApplicationEventMulticaster().notifyEventListeners( new RepositoryEventEvictUnusedItems( this ) );

            return result;
        }
        else
        {
            return super.evictUnusedItems( request, timestamp );
        }
    }

    protected Collection<String> doEvictUnusedItems( ResourceStoreRequest request, final long timestamp,
                                                     EvictUnusedItemsWalkerProcessor processor, WalkerFilter filter )
    {
        getLogger().info(
            String.format( "Evicting unused items from proxy repository %s from path=\"%s\"",
                RepositoryStringUtils.getHumanizedNameString( this ), request.getRequestPath() ) );

        request.setRequestLocalOnly( true );

        DefaultWalkerContext ctx = new DefaultWalkerContext( this, request, filter );

        ctx.getProcessors().add( processor );

        // and let it loose
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

        return processor.getFiles();
    }

    public Map<String, ItemContentValidator> getItemContentValidators()
    {
        if ( itemContentValidators == null )
        {
            itemContentValidators = new HashMap<String, ItemContentValidator>();
        }

        return itemContentValidators;
    }

    public boolean isFileTypeValidation()
    {
        return getExternalConfiguration( false ).isFileTypeValidation();
    }

    public void setFileTypeValidation( boolean doValidate )
    {
        getExternalConfiguration( true ).setFileTypeValidation( doValidate );
    }

    public boolean isItemAgingActive()
    {
        return getExternalConfiguration( false ).isItemAgingActive();
    }

    public void setItemAgingActive( boolean value )
    {
        getExternalConfiguration( true ).setItemAgingActive( value );
    }

    public boolean isAutoBlockActive()
    {
        return getExternalConfiguration( false ).isAutoBlockActive();
    }

    public void setAutoBlockActive( boolean val )
    {
        // NEXUS-3516: if user disables autoblock, and repo is auto-blocked, unblock it
        if ( !val && ProxyMode.BLOCKED_AUTO.equals( getProxyMode() ) )
        {
            getLogger().warn(
                String.format(
                    "Proxy Repository %s was auto-blocked, but user disabled this feature. Unblocking repository, but this MAY cause Nexus to leak connections (if remote repository is still down)!",
                    RepositoryStringUtils.getHumanizedNameString( this ) ) );

            setProxyMode( ProxyMode.ALLOW );
        }

        getExternalConfiguration( true ).setAutoBlockActive( val );
    }

    public Thread getRepositoryStatusCheckerThread()
    {
        return repositoryStatusCheckerThread;
    }

    public void setRepositoryStatusCheckerThread( Thread repositoryStatusCheckerThread )
    {
        this.repositoryStatusCheckerThread = repositoryStatusCheckerThread;
    }

    public long getCurrentRemoteStatusRetainTime()
    {
        return this.remoteStatusRetainTimeSequence.peek();
    }

    public long getNextRemoteStatusRetainTime()
    {
        // step it up, but topped
        if ( this.remoteStatusRetainTimeSequence.peek() <= AUTO_BLOCK_STATUS_MAX_RETAIN_TIME )
        {
            // step it up
            return this.remoteStatusRetainTimeSequence.next();
        }
        else
        {
            // it is topped, so just return current
            return getCurrentRemoteStatusRetainTime();
        }
    }

    public ProxyMode getProxyMode()
    {
        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            return getExternalConfiguration( false ).getProxyMode();
        }
        else
        {
            return null;
        }
    }

    /**
     * ProxyMode is a persisted configuration property, hence it modifies configuration! It is the caller responsibility
     * to save configuration.
     * 
     * @param proxyMode
     * @param sendNotification
     * @param cause
     */
    protected void setProxyMode( ProxyMode proxyMode, boolean sendNotification, Throwable cause )
    {
        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            ProxyMode oldProxyMode = getProxyMode();

            // NEXUS-4537: apply transition constraints: BLOCKED_MANUALLY cannot be transitioned into BLOCKED_AUTO
            if ( !( ProxyMode.BLOCKED_AUTO.equals( proxyMode ) && ProxyMode.BLOCKED_MANUAL.equals( oldProxyMode ) ) )
            {
                // change configuration only if we have a transition
                if ( !oldProxyMode.equals( proxyMode ) )
                {
                    // NEXUS-3552: Tricking the config framework, we are making this applied _without_ making
                    // configuration
                    // dirty
                    if ( ProxyMode.BLOCKED_AUTO.equals( proxyMode ) || ProxyMode.BLOCKED_AUTO.equals( oldProxyMode ) )
                    {
                        getExternalConfiguration( false ).setProxyMode( proxyMode );

                        if ( isDirty() )
                        {
                            // we are dirty, then just set same value in the "changed" one too
                            getExternalConfiguration( true ).setProxyMode( proxyMode );
                        }
                    }
                    else
                    {
                        // this makes it dirty if it was not dirty yet, but this is the intention too
                        getExternalConfiguration( true ).setProxyMode( proxyMode );
                    }
                }

                // setting the time to retain remote status, depending on proxy mode
                // if not blocked_auto, just use default as it was the case before AutoBlock
                if ( ProxyMode.BLOCKED_AUTO.equals( proxyMode ) )
                {
                    if ( !( this.remoteStatusRetainTimeSequence instanceof FibonacciNumberSequence ) )
                    {
                        // take the timeout * 2 as initial step
                        long initialStep = getRemoteConnectionSettings().getConnectionTimeout() * 2L;

                        // make it a fibonacci one
                        this.remoteStatusRetainTimeSequence = new FibonacciNumberSequence( initialStep );

                        // make it step one
                        this.remoteStatusRetainTimeSequence.next();

                        // ping the monitor thread
                        if ( this.repositoryStatusCheckerThread != null )
                        {
                            this.repositoryStatusCheckerThread.interrupt();
                        }
                    }
                }
                else
                {
                    this.remoteStatusRetainTimeSequence = new ConstantNumberSequence( REMOTE_STATUS_RETAIN_TIME );
                }

                // if this is proxy
                // and was !shouldProxy() and the new is shouldProxy()
                if ( proxyMode != null && proxyMode.shouldProxy() && !oldProxyMode.shouldProxy() )
                {
                    // NEXUS-4410: do this only when we are going BLOCKED_MANUAL -> ALLOW transition
                    // In case of Auto unblocking, do not perform purge!
                    if ( !oldProxyMode.shouldAutoUnblock() )
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug( "We have a BLOCKED_MANUAL -> ALLOW transition, purging NFC" );
                        }

                        getNotFoundCache().purge();
                    }

                    resetRemoteStatus();
                }

                if ( sendNotification )
                {
                    // this one should be fired _always_
                    getApplicationEventMulticaster().notifyEventListeners(
                        new RepositoryEventProxyModeSet( this, oldProxyMode, proxyMode, cause ) );

                    if ( !proxyMode.equals( oldProxyMode ) )
                    {
                        // this one should be fired on _transition_ only
                        getApplicationEventMulticaster().notifyEventListeners(
                            new RepositoryEventProxyModeChanged( this, oldProxyMode, proxyMode, cause ) );
                    }
                }
            }
        }
    }

    public void setProxyMode( ProxyMode proxyMode )
    {
        setProxyMode( proxyMode, true, null );
    }

    /**
     * This method should be called by AbstractProxyRepository and it's descendants only. Since this method modifies the
     * ProxyMode property of this repository, and this property is part of configuration, this call will result in
     * configuration flush too (potentially saving any other unsaved changes)!
     * 
     * @param cause
     */
    protected void autoBlockProxying( Throwable cause )
    {
        // depend of proxy mode
        ProxyMode oldProxyMode = getProxyMode();

        // Detect do we deal with S3 remote peer, those are not managed/autoblocked, since we have no
        // proper means using HTTP only to detect the issue.
        {
            RemoteRepositoryStorage remoteStorage = getRemoteStorage();

            /**
             * Special case here to handle Amazon S3 storage. Problem is that if we do a request against a folder, a 403
             * will always be returned, as S3 doesn't support that. So we simple check if its s3 and if so, we ignore
             * the fact that 403 was returned (only in regards to auto-blocking, rest of system will still handle 403
             * response as expected)
             */
            try
            {
                if ( remoteStorage instanceof AbstractHTTPRemoteRepositoryStorage
                    && ( (AbstractHTTPRemoteRepositoryStorage) remoteStorage ).isRemotePeerAmazonS3Storage( this )
                    && cause instanceof RemoteAccessDeniedException )
                {
                    getLogger().debug(
                        "Not autoblocking repository id " + getId() + "since this is Amazon S3 proxy repo" );
                    return;
                }
            }
            catch ( StorageException e )
            {
                // This shouldn't occur, since we are just checking the context
                getLogger().debug( "Unable to validate if proxy repository id " + getId() + "is Amazon S3", e );
            }
        }

        // invalidate remote status
        setRemoteStatus( RemoteStatus.UNAVAILABLE, cause );

        // do we need to do anything at all?
        boolean autoBlockActive = isAutoBlockActive();

        // nag only here
        if ( !ProxyMode.BLOCKED_AUTO.equals( oldProxyMode ) )
        {
            StringBuilder sb = new StringBuilder();

            sb.append( "Remote peer of proxy repository " + RepositoryStringUtils.getHumanizedNameString( this )
                + " threw a " + cause.getClass().getName() + " exception." );

            if ( cause instanceof RemoteAccessException )
            {
                sb.append( " Please set up authorization information for this repository." );
            }
            else if ( cause instanceof StorageException )
            {
                sb.append( " Connection/transport problems occured while connecting to remote peer of the repository." );
            }

            // nag about autoblock if needed
            if ( autoBlockActive )
            {
                sb.append( " Auto-blocking this repository to prevent further connection-leaks and known-to-fail outbound"
                    + " connections until administrator fixes the problems, or Nexus detects remote repository as healthy." );
            }

            // log the event
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().warn( sb.toString(), cause );
            }
            else
            {
                sb.append( " - Cause(s): " ).append( cause.getMessage() );

                Throwable c = cause.getCause();

                while ( c != null )
                {
                    sb.append( " > " ).append( c.getMessage() );

                    c = c.getCause();
                }

                getLogger().warn( sb.toString() );
            }
        }

        // autoblock if needed (above is all about nagging)
        if ( autoBlockActive )
        {
            if ( oldProxyMode != null )
            {
                setProxyMode( ProxyMode.BLOCKED_AUTO, true, cause );
            }

            // NEXUS-3552: Do NOT save configuration, just make it applied (see setProxyMode() how it is done)
            // save configuration only if we made a transition, otherwise no save is needed
            // if ( oldProxyMode != null && !oldProxyMode.equals( ProxyMode.BLOCKED_AUTO ) )
            // {
            // try
            // {
            // // NEXUS-3552: Do NOT save configuration, just make it applied
            // getApplicationConfiguration().saveConfiguration();
            // }
            // catch ( IOException e )
            // {
            // getLogger().warn(
            // "Cannot save configuration after AutoBlocking repository \"" + getName() + "\" (id=" + getId()
            // + ")", e );
            // }
            // }
        }
    }

    /**
     * This method should be called by AbstractProxyRepository and it's descendants only. Since this method modifies the
     * ProxyMode property of this repository, and this property is part of configuration, this call will result in
     * configuration flush too (potentially saving any other unsaved changes)!
     */
    protected void autoUnBlockProxying()
    {
        setRemoteStatus( RemoteStatus.AVAILABLE, null );

        ProxyMode oldProxyMode = getProxyMode();

        if ( oldProxyMode.shouldAutoUnblock() )
        {
            // log the event
            getLogger().warn(
                String.format(
                    "Remote peer of proxy repository %s detected as healthy, un-blocking the proxy repository (it was AutoBlocked by Nexus).",
                    RepositoryStringUtils.getHumanizedNameString( this ) ) );

            setProxyMode( ProxyMode.ALLOW, true, null );
        }

        // NEXUS-3552: Do NOT save configuration, just make it applied (see setProxyMode() how it is done)
        // try
        // {
        // getApplicationConfiguration().saveConfiguration();
        // }
        // catch ( IOException e )
        // {
        // getLogger().warn(
        // "Cannot save configuration after AutoBlocking repository \"" + getName() + "\" (id=" + getId() + ")", e );
        // }
    }

    public RepositoryStatusCheckMode getRepositoryStatusCheckMode()
    {
        return getExternalConfiguration( false ).getRepositoryStatusCheckMode();
    }

    public void setRepositoryStatusCheckMode( RepositoryStatusCheckMode mode )
    {
        getExternalConfiguration( true ).setRepositoryStatusCheckMode( mode );
    }

    public String getRemoteUrl()
    {
        if ( getCurrentConfiguration( false ).getRemoteStorage() != null )
        {
            return getCurrentConfiguration( false ).getRemoteStorage().getUrl();
        }
        else
        {
            return null;
        }
    }

    public void setRemoteUrl( String remoteUrl )
        throws RemoteStorageException
    {
        if ( getRemoteStorage() != null )
        {
            String newRemoteUrl = remoteUrl.trim();

            String oldRemoteUrl = getRemoteUrl();

            if ( !newRemoteUrl.endsWith( RepositoryItemUid.PATH_SEPARATOR ) )
            {
                newRemoteUrl = newRemoteUrl + RepositoryItemUid.PATH_SEPARATOR;
            }

            getRemoteStorage().validateStorageUrl( newRemoteUrl );

            getCurrentConfiguration( true ).getRemoteStorage().setUrl( newRemoteUrl );

            if ( ( StringUtils.isEmpty( oldRemoteUrl ) && StringUtils.isNotEmpty( newRemoteUrl ) )
                || ( StringUtils.isNotEmpty( oldRemoteUrl ) && !oldRemoteUrl.equals( newRemoteUrl ) ) )
            {
                this.remoteUrlChanged = true;
            }
        }
        else
        {
            throw new RemoteStorageException( "No remote storage set on repository \"" + getName() + "\" (ID=\""
                + getId() + "\"), cannot set remoteUrl!" );
        }
    }

    /**
     * Gets the item max age in (in minutes).
     * 
     * @return the item max age in (in minutes)
     */
    public int getItemMaxAge()
    {
        return getExternalConfiguration( false ).getItemMaxAge();
    }

    /**
     * Sets the item max age in (in minutes).
     * 
     * @param itemMaxAge the new item max age in (in minutes).
     */
    public void setItemMaxAge( int itemMaxAge )
    {
        getExternalConfiguration( true ).setItemMaxAge( itemMaxAge );
    }

    protected void resetRemoteStatus()
    {
        remoteStatusUpdated = 0;
    }

    /**
     * Is checking in progress?
     */
    private volatile boolean _remoteStatusChecking = false;

    public RemoteStatus getRemoteStatus( ResourceStoreRequest request, boolean forceCheck )
    {
        // if the last known status is old, simply reset it
        if ( forceCheck || System.currentTimeMillis() - remoteStatusUpdated > REMOTE_STATUS_RETAIN_TIME )
        {
            remoteStatus = RemoteStatus.UNKNOWN;
        }

        if ( getProxyMode() != null && RemoteStatus.UNKNOWN.equals( remoteStatus ) && !_remoteStatusChecking )
        {
            // check for thread and go check it
            _remoteStatusChecking = true;

            poolManager.getRepositoryThreadPool( this ).submit( new RemoteStatusUpdateCallable( request ) );
        }

        return remoteStatus;
    }

    private void setRemoteStatus( RemoteStatus remoteStatus, Throwable cause )
    {
        this.remoteStatus = remoteStatus;

        // UNKNOWN does not count
        if ( RemoteStatus.AVAILABLE.equals( remoteStatus ) || RemoteStatus.UNAVAILABLE.equals( remoteStatus ) )
        {
            this.remoteStatusUpdated = System.currentTimeMillis();
        }
    }

    public RemoteStorageContext getRemoteStorageContext()
    {
        if ( remoteStorageContext == null )
        {
            remoteStorageContext =
                new DefaultRemoteStorageContext( getApplicationConfiguration().getGlobalRemoteStorageContext() );
        }

        return remoteStorageContext;
    }

    public RemoteConnectionSettings getRemoteConnectionSettings()
    {
        return getRemoteStorageContext().getRemoteConnectionSettings();
    }

    public void setRemoteConnectionSettings( RemoteConnectionSettings settings )
    {
        getRemoteStorageContext().setRemoteConnectionSettings( settings );
    }

    public RemoteAuthenticationSettings getRemoteAuthenticationSettings()
    {
        return getRemoteStorageContext().getRemoteAuthenticationSettings();
    }

    public void setRemoteAuthenticationSettings( RemoteAuthenticationSettings settings )
    {
        getRemoteStorageContext().setRemoteAuthenticationSettings( settings );

        if ( getProxyMode() != null && getProxyMode().shouldAutoUnblock() )
        {
            // perm changes? retry if autoBlocked
            setProxyMode( ProxyMode.ALLOW );
        }
    }

    public RemoteProxySettings getRemoteProxySettings()
    {
        return getRemoteStorageContext().getRemoteProxySettings();
    }

    public void setRemoteProxySettings( RemoteProxySettings settings )
    {
        getRemoteStorageContext().setRemoteProxySettings( settings );

        if ( getProxyMode() != null && getProxyMode().shouldAutoUnblock() )
        {
            // perm changes? retry if autoBlocked
            setProxyMode( ProxyMode.ALLOW );
        }
    }

    public ProxySelector getProxySelector()
    {
        if ( proxySelector == null )
        {
            proxySelector = new DefaultProxySelector();
        }

        return proxySelector;
    }

    public void setProxySelector( ProxySelector selector )
    {
        this.proxySelector = selector;
    }

    public RemoteRepositoryStorage getRemoteStorage()
    {
        return remoteStorage;
    }

    public void setRemoteStorage( RemoteRepositoryStorage remoteStorage )
    {
        this.remoteStorage = remoteStorage;

        if ( remoteStorage == null )
        {
            getCurrentConfiguration( true ).setRemoteStorage( null );
        }
        else
        {
            if ( getCurrentConfiguration( true ).getRemoteStorage() == null )
            {
                getCurrentConfiguration( true ).setRemoteStorage( new CRemoteStorage() );
            }

            getCurrentConfiguration( true ).getRemoteStorage().setProvider( remoteStorage.getProviderId() );

            setWritePolicy( RepositoryWritePolicy.READ_ONLY );
        }
    }

    public DownloadMirrors getDownloadMirrors()
    {
        if ( dMirrors == null )
        {
            dMirrors = new DefaultDownloadMirrors( (CRepositoryCoreConfiguration) getCurrentCoreConfiguration() );
        }

        return dMirrors;
    }

    protected DownloadMirrorSelector openDownloadMirrorSelector( ResourceStoreRequest request )
    {
        return this.getDownloadMirrors().openSelector( this.getRemoteUrl() );
    }

    public AbstractStorageItem doCacheItem( AbstractStorageItem item )
        throws LocalStorageException
    {
        boolean shouldCache = true;

        // ask request processors too
        for ( RequestProcessor processor : getRequestProcessors().values() )
        {
            shouldCache = processor.shouldCache( this, item );

            if ( !shouldCache )
            {
                return item;
            }
        }

        AbstractStorageItem result = null;

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "Caching item " + item.getRepositoryItemUid().toString() + " in local storage of repository." );
            }

            final RepositoryItemUidLock itemLock = item.getRepositoryItemUid().getLock();

            itemLock.lock( Action.create );

            final Action action = getResultingActionOnWrite( item.getResourceStoreRequest() );

            try
            {
                getLocalStorage().storeItem( this, item );

                removeFromNotFoundCache( item.getResourceStoreRequest() );

                result = getLocalStorage().retrieveItem( this, new ResourceStoreRequest( item ) );

            }
            finally
            {
                itemLock.unlock();
            }

            result.getItemContext().putAll( item.getItemContext() );

            if ( Action.create.equals( action ) )
            {
                getApplicationEventMulticaster().notifyEventListeners(
                    new RepositoryItemEventCacheCreate( this, result ) );
            }
            else
            {
                getApplicationEventMulticaster().notifyEventListeners(
                    new RepositoryItemEventCacheUpdate( this, result ) );
            }
        }
        catch ( ItemNotFoundException ex )
        {
            getLogger().warn(
                "Nexus BUG in "
                    + RepositoryStringUtils.getHumanizedNameString( this )
                    + ", ItemNotFoundException during cache! Please report this issue along with the stack trace below!",
                ex );

            // this is a nonsense, we just stored it!
            result = item;
        }
        catch ( UnsupportedStorageOperationException ex )
        {
            getLogger().warn(
                "LocalStorage or repository " + RepositoryStringUtils.getHumanizedNameString( this )
                    + " does not handle STORE operation, not caching remote fetched item.", ex );

            result = item;
        }

        return result;
    }

    @Override
    protected StorageItem doRetrieveItem( ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        if ( getLogger().isDebugEnabled() )
        {
            StringBuffer db = new StringBuffer( request.toString() );

            db.append( " :: localOnly=" ).append( request.isRequestLocalOnly() );
            db.append( ", remoteOnly=" ).append( request.isRequestRemoteOnly() );

            if ( getProxyMode() != null )
            {
                db.append( ", ProxyMode=" + getProxyMode().toString() );
            }

            getLogger().debug( db.toString() );
        }

        // we have to re-set locking here explicitly, since we are going to
        // make a "salto-mortale" here, see below
        // we start with "usual" read lock, we still don't know is this hosted or proxy repo
        // if proxy, we still don't know do we have to go remote (local copy is old) or not
        // if proxy and need to go remote, we want to _protect_ ourselves from
        // serving up partial downloads...

        final RepositoryItemUid itemUid = createUid( request.getRequestPath() );

        final RepositoryItemUidLock itemUidLock = itemUid.getLock();

        itemUidLock.lock( Action.read );

        try
        {
            if ( !getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
            {
                // we have no proxy facet, just get 'em!
                return super.doRetrieveItem( request );
            }
            else
            {
                // we have Proxy facet, so we want to check carefully local storage
                // Reason: a previous thread may still _downloading_ the stuff we want to
                // serve to another client, so we have to _wait_ for download, but for download
                // only.
                AbstractStorageItem localItem = null;

                if ( !request.isRequestRemoteOnly() )
                {
                    try
                    {
                        localItem = (AbstractStorageItem) super.doRetrieveItem( request );

                        if ( localItem != null && !isOld( localItem ) )
                        {
                            // local copy is just fine, so, we are proxy but we have valid local copy in cache
                            return localItem;
                        }
                    }
                    catch ( ItemNotFoundException e )
                    {
                        localItem = null;
                    }
                }

                // we are a proxy, and we either don't have local copy or is stale, we need to
                // go remote and potentially check for new version of file, but we still don't know
                // will we actually fetch it (since aging != remote file changed!)
                // BUT, from this point on, we want to _serialize_ access, so upgrade to CREATE lock

                itemUidLock.lock( Action.create );

                try
                {
                    // check local copy again, we were maybe blocked for a download, and we need to
                    // recheck local copy after we acquired exclusive lock
                    if ( !request.isRequestRemoteOnly() )
                    {
                        try
                        {
                            localItem = (AbstractStorageItem) super.doRetrieveItem( request );

                            if ( localItem != null && !isOld( localItem ) )
                            {
                                // local copy is just fine (downloaded by a thread holding us blocked on acquiring
                                // exclusive lock)
                                return localItem;
                            }
                        }
                        catch ( ItemNotFoundException e )
                        {
                            localItem = null;
                        }
                    }

                    // this whole method happens with exclusive lock on UID
                    return doRetrieveItem0( request, localItem );
                }
                finally
                {
                    itemUidLock.unlock();
                }
            }
        }
        finally
        {
            itemUidLock.unlock();
        }
    }

    protected StorageItem doRetrieveItem0( ResourceStoreRequest request, AbstractStorageItem localItem )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        AbstractStorageItem item = null;
        AbstractStorageItem remoteItem = null;

        // proxyMode and request.localOnly decides 1st
        boolean shouldProxy = !request.isRequestLocalOnly() && getProxyMode() != null && getProxyMode().shouldProxy();

        if ( shouldProxy )
        {
            // let's ask RequestProcessor
            for ( RequestProcessor processor : getRequestProcessors().values() )
            {
                shouldProxy = processor.shouldProxy( this, request );

                if ( !shouldProxy )
                {
                    // escape
                    break;
                }
            }
        }

        if ( shouldProxy )
        {
            // we are able to go remote
            if ( localItem == null || isOld( localItem ) )
            {
                // we should go remote coz we have no local copy or it is old
                try
                {
                    boolean shouldGetRemote = false;

                    if ( localItem != null )
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug(
                                "Item " + request.toString()
                                    + " is old, checking for newer file on remote then local: "
                                    + new Date( localItem.getModified() ) );
                        }

                        // check is the remote newer than the local one
                        try
                        {
                            shouldGetRemote = doCheckRemoteItemExistence( localItem, request );

                            if ( !shouldGetRemote )
                            {
                                markItemRemotelyChecked( localItem );

                                if ( getLogger().isDebugEnabled() )
                                {
                                    getLogger().debug(
                                        "No newer version of item " + request.toString() + " found on remote storage." );
                                }
                            }
                            else
                            {
                                if ( getLogger().isDebugEnabled() )
                                {
                                    getLogger().debug(
                                        "Newer version of item " + request.toString() + " is found on remote storage." );
                                }
                            }

                        }
                        catch ( RemoteAccessDeniedException ex )
                        {
                            // NEXUS-4593 do not autoblock, 403 is "ok"

                            // do not go remote, but we did not mark it as "remote checked" also.
                            // let the user do proper setup and probably it will try again
                            shouldGetRemote = false;
                        }
                        catch ( RemoteStorageException ex )
                        {
                            autoBlockProxying( ex );

                            // do not go remote, but we did not mark it as "remote checked" also.
                            // let the user do proper setup and probably it will try again
                            shouldGetRemote = false;
                        }
                        catch ( IOException ex )
                        {
                            // do not go remote, but we did not mark it as "remote checked" also.
                            // let the user do proper setup and probably it will try again
                            shouldGetRemote = false;
                        }
                    }
                    else
                    {
                        // we have no local copy of it, try to get it unconditionally
                        shouldGetRemote = true;
                    }

                    if ( shouldGetRemote )
                    {
                        // this will GET it unconditionally
                        try
                        {
                            remoteItem = doRetrieveRemoteItem( request );

                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug( "Item " + request.toString() + " found in remote storage." );
                            }
                        }
                        catch ( StorageException ex )
                        {
                            if ( ex instanceof RemoteStorageException
                            // NEXUS-4593 HTTP status 403 should not lead to autoblock
                                && !( ex instanceof RemoteAccessDeniedException ) )
                            {
                                autoBlockProxying( ex );
                            }

                            remoteItem = null;

                            // cleanup if any remnant is here
                            try
                            {
                                if ( localItem == null )
                                {
                                    deleteItem( false, request );
                                }
                            }
                            catch ( ItemNotFoundException ex1 )
                            {
                                // ignore
                            }
                            catch ( UnsupportedStorageOperationException ex2 )
                            {
                                // will not happen
                            }
                        }
                    }
                    else
                    {
                        remoteItem = null;
                    }
                }
                catch ( ItemNotFoundException ex )
                {
                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Item " + request.toString() + " not found in remote storage." );
                    }

                    remoteItem = null;
                }
            }

            if ( localItem == null && remoteItem == null )
            {
                // we dont have neither one, NotFoundException
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Item "
                            + request.toString()
                            + " does not exist in local storage neither in remote storage, throwing ItemNotFoundException." );
                }

                throw new ItemNotFoundException( request, this );
            }
            else if ( localItem != null && remoteItem == null )
            {
                // simple: we have local but not remote (coz we are offline or coz it is not newer)
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Item " + request.toString()
                            + " does exist in local storage and is fresh, returning local one." );
                }

                item = localItem;
            }
            else
            {
                // the fact that remoteItem != null means we _have_ to return that one
                // OR: we had no local copy
                // OR: remoteItem is for sure newer (look above)
                item = remoteItem;
            }

        }
        else
        {
            // we cannot go remote
            if ( localItem != null )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Item " + request.toString() + " does exist locally and cannot go remote, returning local one." );
                }

                item = localItem;
            }
            else
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Item " + request.toString()
                            + " does not exist locally and cannot go remote, throwing ItemNotFoundException." );
                }

                throw new ItemNotFoundException( request, this );
            }
        }

        return item;
    }

    private void sendContentValidationEvents( ResourceStoreRequest request, List<RepositoryItemValidationEvent> events,
                                              boolean isContentValid )
    {
        if ( getLogger().isDebugEnabled() && !isContentValid )
        {
            getLogger().debug( "Item " + request.toString() + " failed content integrity validation." );
        }

        for ( RepositoryItemValidationEvent event : events )
        {
            getApplicationEventMulticaster().notifyEventListeners( event );
        }
    }

    protected void markItemRemotelyChecked( final StorageItem item )
        throws IOException, ItemNotFoundException
    {
        // remote file unchanged, touch the local one to renew it's Age
        getAttributesHandler().touchItemCheckedRemotely( System.currentTimeMillis(), item );
    }

    /**
     * Validates integrity of content of <code>item</code>. Retruns <code>true</code> if item content is valid and
     * <code>false</code> if item content is corrupted. Note that this method is called doRetrieveRemoteItem, so
     * implementation must retrieve checksum files directly from remote storage <code>
     * getRemoteStorage().retrieveItem( this, context, getRemoteUrl(), checksumUid.getPath() );
     * </code>
     */
    protected boolean doValidateRemoteItemContent( ResourceStoreRequest req, String baseUrl, AbstractStorageItem item,
                                                   List<RepositoryItemValidationEvent> events )
    {
        boolean isValid = true;

        for ( Map.Entry<String, ItemContentValidator> icventry : getItemContentValidators().entrySet() )
        {
            try
            {
                boolean isValidByCurrentItemContentValidator =
                    icventry.getValue().isRemoteItemContentValid( this, req, baseUrl, item, events );

                if ( !isValidByCurrentItemContentValidator )
                {
                    getLogger().info(
                        String.format(
                            "Proxied item %s evaluated as INVALID during content validation (validator=%s, sourceUrl=%s)",
                            item.getRepositoryItemUid().toString(), icventry.getKey(), item.getRemoteUrl() ) );
                }

                isValid = isValid && isValidByCurrentItemContentValidator;
            }
            catch ( StorageException e )
            {
                getLogger().info(
                    String.format(
                        "Proxied item %s evaluated as INVALID during content validation (validator=%s, sourceUrl=%s)",
                        item.getRepositoryItemUid().toString(), icventry.getKey(), item.getRemoteUrl() ), e );

                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Checks for remote existence of local item.
     * 
     * @param localItem
     * @param request
     * @return
     * @throws RemoteAccessException
     * @throws StorageException
     */
    protected boolean doCheckRemoteItemExistence( StorageItem localItem, ResourceStoreRequest request )
        throws RemoteAccessException, RemoteStorageException
    {
        if ( localItem != null )
        {
            return getRemoteStorage().containsItem( localItem.getModified(), this, request );
        }
        else
        {
            return getRemoteStorage().containsItem( this, request );
        }
    }

    /**
     * Retrieves item with specified uid from remote storage according to the following retry-fallback-blacklist rules.
     * <li>Only retrieve item operation will use mirrors, other operations, like check availability and retrieve
     * checksum file, will always use repository canonical url.</li> <li>Only one mirror url will be considered before
     * retrieve item operation falls back to repository canonical url.</li> <li>Repository canonical url will never be
     * put on the blacklist.</li> <li>If retrieve item operation fails with ItemNotFound or AccessDenied error, the
     * operation will be retried with another url or original error will be reported if there are no more urls.</li> <li>
     * If retrieve item operation fails with generic StorageException or item content is corrupt, the operation will be
     * retried one more time from the same url. After that, the operation will be retried with another url or original
     * error will be returned if there are no more urls.</li> <li>Mirror url will be put on the blacklist if retrieve
     * item operation from the url failed with StorageException, AccessDenied or InvalidItemContent error but the item
     * was successfully retrieve from another url.</li> <li>Mirror url will be removed from blacklist after 30 minutes.</li>
     * The following matrix summarises retry/blacklist behaviour
     * <p/>
     * <p/>
     * 
     * <pre>
     * Error condition      Retry?        Blacklist?
     * 
     * InetNotFound         no            no
     * AccessDedied         no            yes
     * InvalidContent       no            no
     * Other                yes           yes
     * </pre>
     */
    protected AbstractStorageItem doRetrieveRemoteItem( ResourceStoreRequest request )
        throws ItemNotFoundException, RemoteAccessException, StorageException
    {
        final RepositoryItemUid itemUid = createUid( request.getRequestPath() );

        final RepositoryItemUidLock itemUidLock = itemUid.getLock();

        // all this remote download happens in exclusive lock
        itemUidLock.lock( Action.create );

        try
        {
            DownloadMirrorSelector selector = this.openDownloadMirrorSelector( request );

            List<Mirror> mirrors = new ArrayList<Mirror>( selector.getMirrors() );
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Mirror count:" + mirrors.size() );
            }

            mirrors.add( new Mirror( "default", getRemoteUrl(), getRemoteUrl() ) );

            List<RepositoryItemValidationEvent> events = new ArrayList<RepositoryItemValidationEvent>();

            Exception lastException = null;

            try
            {
                all_urls: for ( Mirror mirror : mirrors )
                {
                    int retryCount = 1;

                    if ( getRemoteStorageContext() != null )
                    {
                        retryCount = getRemoteStorageContext().getRemoteConnectionSettings().getRetrievalRetryCount();
                    }

                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Using mirror URL:" + mirror.getUrl() + ", retryCount=" + retryCount );
                    }

                    // Validate the mirror URL
                    try
                    {
                        getRemoteStorage().validateStorageUrl( mirror.getUrl() );
                    }
                    catch ( Exception e )
                    {
                        lastException = e;

                        selector.feedbackFailure( mirror );
                        logFailedMirror( mirror, e );

                        continue all_urls; // retry with next url
                    }

                    for ( int i = 0; i < retryCount; i++ )
                    {
                        try
                        {
                            // events.clear();

                            AbstractStorageItem remoteItem =
                                getRemoteStorage().retrieveItem( this, request, mirror.getUrl() );

                            remoteItem.getItemContext().putAll( request.getRequestContext() );

                            remoteItem = doCacheItem( remoteItem );

                            if ( doValidateRemoteItemContent( request, mirror.getUrl(), remoteItem, events ) )
                            {
                                sendContentValidationEvents( request, events, true );

                                selector.feedbackSuccess( mirror );

                                return remoteItem;
                            }
                            else
                            {
                                continue all_urls; // retry with next url
                            }
                        }
                        catch ( ItemNotFoundException e )
                        {
                            lastException = e;

                            continue all_urls; // retry with next url
                        }
                        catch ( RemoteAccessException e )
                        {
                            lastException = e;

                            selector.feedbackFailure( mirror );
                            logFailedMirror( mirror, e );

                            continue all_urls; // retry with next url
                        }
                        catch ( RemoteStorageException e )
                        {
                            lastException = e;

                            selector.feedbackFailure( mirror );
                            // debug, print all
                            if ( getLogger().isDebugEnabled() )
                            {
                                logFailedMirror( mirror, e );
                            }
                            // not debug, only print the message
                            else
                            {
                                Throwable t = ExceptionUtils.getRootCause( e );

                                if ( t == null )
                                {
                                    t = e;
                                }

                                getLogger().error(
                                    String.format(
                                        "Got RemoteStorageException in proxy repository %s while retrieving remote artifact \"%s\" from URL %s, this is %s (re)try, cause: %s: %s",
                                        RepositoryStringUtils.getHumanizedNameString( this ), request.toString(),
                                        mirror.getUrl(), String.valueOf( i + 1 ), t.getClass().getName(),
                                        t.getMessage() ) );
                            }

                            // nope, do not switch Mirror yet, obey the retries
                            // continue all_urls; // retry with next url
                        }
                        catch ( LocalStorageException e )
                        {
                            lastException = e;

                            selector.feedbackFailure( mirror );
                            // debug, print all
                            if ( getLogger().isDebugEnabled() )
                            {
                                logFailedMirror( mirror, e );
                            }
                            // not debug, only print the message
                            else
                            {
                                Throwable t = ExceptionUtils.getRootCause( e );

                                if ( t == null )
                                {
                                    t = e;
                                }

                                getLogger().error(
                                    String.format(
                                        "Got LocalStorageException in proxy repository %s while caching retrieved artifact \"%s\" got from URL %s, will attempt next mirror, cause: %s: %s",
                                        RepositoryStringUtils.getHumanizedNameString( this ), request.toString(),
                                        mirror.getUrl(), t.getClass().getName(), t.getMessage() ) );
                            }

                            // This is actually fatal error? LocalStorageException means something like IOException
                            // while writing data to disk, full disk, no perms, etc
                            // currently, we preserve the old -- probably wrong -- behaviour: on IOException Nexus will
                            // log the error
                            // but will respond with 404
                            continue all_urls; // retry with next url
                        }
                        catch ( RuntimeException e )
                        {
                            lastException = e;

                            selector.feedbackFailure( mirror );
                            logFailedMirror( mirror, e );

                            continue all_urls; // retry with next url
                        }

                        // retry with same url
                    }
                }
            }
            finally
            {
                selector.close();
            }

            // if we got here, requested item was not retrieved for some reason

            sendContentValidationEvents( request, events, false );

            try
            {
                getLocalStorage().deleteItem( this, request );
            }
            catch ( ItemNotFoundException e )
            {
                // good, we want this item deleted
            }
            catch ( UnsupportedStorageOperationException e )
            {
                getLogger().warn( "Unexpected Exception in " + RepositoryStringUtils.getHumanizedNameString( this ), e );
            }

            if ( lastException instanceof StorageException )
            {
                throw (StorageException) lastException;
            }
            else if ( lastException instanceof ItemNotFoundException )
            {
                throw (ItemNotFoundException) lastException;
            }

            // validation failed, I guess.
            throw new ItemNotFoundException( request, this );
        }
        finally
        {
            itemUidLock.unlock();
        }
    }

    private void logFailedMirror( Mirror mirror, Exception e )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Failed mirror URL:" + mirror.getUrl() );
            getLogger().debug( e.getMessage(), e );
        }
    }

    /**
     * Checks if item is old with "default" maxAge.
     * 
     * @param item the item
     * @return true, if it is old
     */
    protected boolean isOld( StorageItem item )
    {
        return isOld( getItemMaxAge(), item );
    }

    /**
     * Checks if item is old with given maxAge.
     * 
     * @param maxAge
     * @param item
     * @return
     */
    protected boolean isOld( int maxAge, StorageItem item )
    {
        return isOld( maxAge, item, isItemAgingActive() );
    }

    protected boolean isOld( int maxAge, StorageItem item, boolean shouldCalculate )
    {
        if ( !shouldCalculate )
        {
            // simply say "is old" always
            return true;
        }

        // if item is manually expired, true
        if ( item.isExpired() )
        {
            return true;
        }

        // a directory is not "aged"
        if ( StorageCollectionItem.class.isAssignableFrom( item.getClass() ) )
        {
            return false;
        }

        // if repo is non-expirable, false
        if ( maxAge < 0 )
        {
            return false;
        }
        // else check age
        else
        {
            return ( ( System.currentTimeMillis() - item.getRemoteChecked() ) > ( maxAge * 60L * 1000L ) );
        }
    }

    private class RemoteStatusUpdateCallable
        implements Callable<Object>
    {

        private ResourceStoreRequest request;

        public RemoteStatusUpdateCallable( ResourceStoreRequest request )
        {
            this.request = request;
        }

        public Object call()
            throws Exception
        {
            try
            {
                try
                {
                    if ( !getProxyMode().shouldCheckRemoteStatus() )
                    {
                        setRemoteStatus( RemoteStatus.UNAVAILABLE, new ItemNotFoundException( request ) );
                    }
                    else
                    {
                        if ( isRemoteStorageReachable( request ) )
                        {
                            autoUnBlockProxying();
                        }
                        else
                        {
                            autoBlockProxying( new ItemNotFoundException( request ) );
                        }
                    }
                }
                catch ( RemoteStorageException e )
                {
                    // autoblock only when remote problems occur
                    autoBlockProxying( e );
                }
            }
            finally
            {
                _remoteStatusChecking = false;
            }

            return null;
        }
    }

    protected boolean isRemoteStorageReachable( ResourceStoreRequest request )
        throws StorageException
    {
        return getRemoteStorage().isReachable( this, request );
    }

    // Need to allow delete for proxy repos
    @Override
    protected boolean isActionAllowedReadOnly( Action action )
    {
        return action.equals( Action.read ) || action.equals( Action.delete );
    }

    /**
     * Beside original behavior, only add to NFC when we are not in BLOCKED mode.
     * 
     * @since 2.0
     */
    @Override
    protected boolean shouldAddToNotFoundCache( final ResourceStoreRequest request )
    {
        boolean shouldAddToNFC = super.shouldAddToNotFoundCache( request );
        if ( shouldAddToNFC )
        {
            shouldAddToNFC = getProxyMode() == null || getProxyMode().shouldProxy();
            if ( !shouldAddToNFC && getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    String.format(
                        "Proxy repository '%s' is is not allowed to issue remote requests (%s), not adding path '%s' to NFC",
                        getId(), getProxyMode(), request.getRequestPath() ) );
            }
        }
        return shouldAddToNFC;
    }

}
