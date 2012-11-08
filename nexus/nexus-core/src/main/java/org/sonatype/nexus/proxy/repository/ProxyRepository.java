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

import java.util.Map;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.mirror.DownloadMirrors;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.walker.WalkerFilter;

/**
 * A proxy repository is what it's name says :)
 * 
 * @author cstamas
 */
public interface ProxyRepository
    extends Repository
{
    /**
     * Marks the proxy cache items as expired. This methods delegates to
     * {@link #expireProxyCaches(ResourceStoreRequest, WalkerFilter)} method using {@code null} for filter.
     * 
     * @param request a path from to start descending. If null, it is taken as "root".
     * @since 2.0
     */
    void expireProxyCaches( ResourceStoreRequest request );

    /**
     * Marks the proxy cache items as expired (items stored in Local Storage that is actually proxy cache in case of
     * Proxy repository). This expiration is explicit, and puts a flag in item attribute, that is overriding the aging
     * algorithm too! Meaning, even if your maxAge is "never", but item is flagged as expired, remote check will happen.
     * Also, consider that proxy cache might be huge, this method might be a long runner.
     * 
     * @param request a path from to start descending. If null, it is taken as "root".
     * @param filter to apply or {@code null} for "all".
     * @return {@code true} if cache modified.
     * @since 2.1
     */
    boolean expireProxyCaches( ResourceStoreRequest request, WalkerFilter filter );

    /**
     * Gets remote status.
     */
    RemoteStatus getRemoteStatus( ResourceStoreRequest request, boolean forceCheck );

    Thread getRepositoryStatusCheckerThread();

    void setRepositoryStatusCheckerThread( Thread thread );

    /**
     * Returns the current remote status retain time. Does not change or step it's value.
     * 
     * @return
     */
    long getCurrentRemoteStatusRetainTime();

    /**
     * Steps and returns the new current remote status retain time. It does change the underlying NumberSequence (if
     * needed). Also, this method tops the change, and will not increase the NumberSequence over some limit.
     * 
     * @return
     */
    long getNextRemoteStatusRetainTime();

    /**
     * Gets proxy mode.
     * 
     * @return
     */
    ProxyMode getProxyMode();

    /**
     * Sets proxy mode.
     * 
     * @param val
     */
    void setProxyMode( ProxyMode val );

    /**
     * Gets the item max age in (in minutes).
     * 
     * @return the item max age in (in minutes)
     */
    int getItemMaxAge();

    /**
     * Sets the item max age in (in minutes).
     * 
     * @param itemMaxAgeInSeconds the new item max age in (in minutes).
     */
    void setItemMaxAge( int itemMaxAge );

    /**
     * Gets the content validation setting.
     * 
     * @return
     */
    public boolean isFileTypeValidation();

    /**
     * Sets the content validation setting.
     * 
     * @param doValidate
     */
    public void setFileTypeValidation( boolean doValidate );

    /**
     * Gets the RepositoryStatusCheckMode.
     * 
     * @return
     */
    RepositoryStatusCheckMode getRepositoryStatusCheckMode();

    /**
     * Sets the RepositoryStatusCheckMode.
     * 
     * @param mode
     */
    void setRepositoryStatusCheckMode( RepositoryStatusCheckMode mode );

    /**
     * Returns true if this ProxyRepository should "auto block" itself when the remote repository has transport (or
     * other) problems, like bad remoteUrl is set.
     * 
     * @return
     */
    boolean isAutoBlockActive();

    /**
     * Sets the ProxyRepository autoBlock feature active or inactive.
     * 
     * @param val
     */
    void setAutoBlockActive( boolean val );

    /**
     * Returns the remote URL of this repository, if any.
     * 
     * @return remote url of this repository, null otherwise.
     */
    String getRemoteUrl();

    /**
     * Sets the remote url.
     * 
     * @param url the new remote url
     */
    void setRemoteUrl( String url )
        throws RemoteStorageException;

    /**
     * Gets the download mirrors.
     * 
     * @return
     */
    DownloadMirrors getDownloadMirrors();

    /**
     * Gets the remote connections settings. Delegates to RemoteStorageContext.
     * 
     * @return
     */
    RemoteConnectionSettings getRemoteConnectionSettings();

    /**
     * Set remote connection settings. Delegates to RemoteStorageContext.
     * 
     * @param settings
     */
    void setRemoteConnectionSettings( RemoteConnectionSettings settings );

    /**
     * Gets remote authentication settings. Delegates to RemoteStorageContext.
     */
    RemoteAuthenticationSettings getRemoteAuthenticationSettings();

    /**
     * Sets remote authentication settings. Delegates to RemoteStorageContext.
     * 
     * @param settings
     */
    void setRemoteAuthenticationSettings( RemoteAuthenticationSettings settings );

    /**
     * Gets remote proxy settings. Delegates to RemoteStorageContext.
     * 
     * @return
     */
    RemoteProxySettings getRemoteProxySettings();

    /**
     * Sets remote proxy settings. Delegates to RemoteStorageContext.
     * 
     * @param settings
     */
    void setRemoteProxySettings( RemoteProxySettings settings );

    /**
     * Gets the proxy selector of this repository.
     * 
     * @return
     */
    ProxySelector getProxySelector();

    /**
     * Sets the proxy selector of this repository.
     */
    void setProxySelector( ProxySelector proxySelector );

    /**
     * Returns is the "aging" applied to the items in this proxy repository. If false, then this proxy will not apply
     * "aging" to items, and will always go for remote to check for change.
     * 
     * @return
     */
    boolean isItemAgingActive();

    /**
     * Sets the "aging" algorithm status.
     * 
     * @param value
     */
    void setItemAgingActive( boolean value );

    // --

    /**
     * Returns repository specific remote connection context.
     * 
     * @return null if none
     */
    RemoteStorageContext getRemoteStorageContext();

    /**
     * Returns the remoteStorage of the repository. Per repository instance may exists.
     * 
     * @return remoteStorage or null.
     */
    RemoteRepositoryStorage getRemoteStorage();

    /**
     * Sets the remote storage of the repository. May be null if this is a Local repository only. Per repository
     * instance may exists.
     * 
     * @param storage the storage
     */
    void setRemoteStorage( RemoteRepositoryStorage storage );

    /**
     * Returns the list of defined item content validators.
     * 
     * @return
     */
    Map<String, ItemContentValidator> getItemContentValidators();

    /**
     * Caches an item.
     * 
     * @param item
     * @return
     * @throws StorageException
     */
    AbstractStorageItem doCacheItem( AbstractStorageItem item )
        throws LocalStorageException;

}
