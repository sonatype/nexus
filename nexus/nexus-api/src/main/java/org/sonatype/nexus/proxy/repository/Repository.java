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

import java.util.Collection;
import java.util.Map;

import org.sonatype.nexus.configuration.Configurable;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.attributes.AttributesHandler;
import org.sonatype.nexus.proxy.cache.PathCache;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeManager;
import org.sonatype.nexus.proxy.mirror.PublishedMirrors;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.target.TargetSet;
import org.sonatype.nexus.scheduling.RepositoryTaskFilter;

/**
 * Repository interface used by Proximity. It is an extension of ResourceStore iface, allowing to make direct
 * RepositoryItemUid based requests which bypasses AccessManager. Also, defines some properties.
 * 
 * @author cstamas
 */
@RepositoryType( pathPrefix = "repositories" )
public interface Repository
    extends ResourceStore, Configurable
{
    /**
     * Returns the repository's "provider" role. These are getters only, and application is NOT able to change these
     * values runtime! Note: this is a FQN of a class, that is used to "register" the component with container, and the
     * class might reside in core but also in a plugin (separate child classloader!).
     * 
     * @return
     */
    String getProviderRole();

    /**
     * Returns the repository's "provider" hint. These are getters only, and application is NOT able to change these
     * values runtime!
     * 
     * @return
     */
    String getProviderHint();

    /**
     * Returns the ID of the resourceStore.
     * 
     * @return the id
     */
    String getId();

    /**
     * Sets the ID of the resourceStore. It must be unique type-wide (Router vs Repository).
     * 
     * @param id the ID of the repo.
     */
    void setId( String id );

    /**
     * Gets repository human name.
     * 
     * @return
     */
    String getName();

    /**
     * Sets repository human name.
     * 
     * @param name
     */
    void setName( String name );

    /**
     * Used by router only, to specify a valid path prefix to a repository (previously was used getId() for this).
     * 
     * @return
     */
    String getPathPrefix();

    /**
     * Used by router only, to specify a valid path prefix to a repository (previously was used getId() for this).
     * 
     * @param prefix
     */
    void setPathPrefix( String prefix );

    /**
     * This is the "type"/kind of the repository. It tells some minimal info about the repo working (not content,
     * neither implementation).
     * 
     * @return
     */
    RepositoryKind getRepositoryKind();

    /**
     * This is the "class" of the repository content. It is used in grouping, only same content reposes may be grouped.
     * 
     * @return
     */
    ContentClass getRepositoryContentClass();

    /**
     * Returns the task filter for this repository.
     * 
     * @return
     */
    RepositoryTaskFilter getRepositoryTaskFilter();

    /**
     * Gets the target set for request.
     * 
     * @param uid
     * @return
     */
    TargetSet getTargetsForRequest( ResourceStoreRequest request );

    /**
     * Checks is there at all any target for the given request.
     * 
     * @param uid
     * @return
     */
    boolean hasAnyTargetsForRequest( ResourceStoreRequest request );

    /**
     * Creates an UID within this Repository.
     */
    RepositoryItemUid createUid( String path );

    /**
     * Returns the repository ItemUidAttributeManager.
     * 
     * @return
     */
    RepositoryItemUidAttributeManager getRepositoryItemUidAttributeManager();

    /**
     * Will return the proper Action that will occur on "write" operation: create (if nothing exists on the given path)
     * or update (if overwrite will happen since the path already exists).
     * 
     * @param action
     * @return
     * @throws StorageException when some storage (IO) problem happens.
     */
    Action getResultingActionOnWrite( ResourceStoreRequest rsr )
        throws LocalStorageException;

    /**
     * Is the target repository compatible to this one
     * 
     * @param repository
     * @return
     */
    boolean isCompatible( Repository repository );

    /**
     * Returns the facet of Repository, if available, otherwise it returns null.
     * 
     * @param <T>
     * @param t
     * @return the facet requested, otherwise null.
     */
    <F> F adaptToFacet( Class<F> t );

    // ==================================================
    // NFC et al

    /**
     * Gets the not found cache time to live (in minutes).
     * 
     * @return the not found cache time to live (in minutes)
     */
    int getNotFoundCacheTimeToLive();

    /**
     * Sets the not found cache time to live (in minutes).
     * 
     * @param notFoundCacheTimeToLiveSeconds the new not found cache time to live (in minutes).
     */
    void setNotFoundCacheTimeToLive( int notFoundCacheTimeToLive );

    /**
     * Gets the not found cache.
     * 
     * @return the not found cache
     */
    PathCache getNotFoundCache();

    /**
     * Sets the not found cache.
     * 
     * @param notFoundcache the new not found cache
     */
    void setNotFoundCache( PathCache notFoundcache );

    /**
     * Maintains NFC.
     * 
     * @param path
     * @throws ItemNotFoundException
     */
    void maintainNotFoundCache( ResourceStoreRequest request )
        throws ItemNotFoundException;

    /**
     * Adds path to NFC.
     * 
     * @param path
     * @deprecated use the method with request parameter
     */
    @Deprecated
    void addToNotFoundCache( String path );

    /**
     * Removes path from NFC.
     * 
     * @param path
     * @deprecated use the method with request parameter
     */
    @Deprecated
    void removeFromNotFoundCache( String path );

    /**
     * Adds path to NFC.
     * 
     * @param path
     */
    void addToNotFoundCache( ResourceStoreRequest request );

    /**
     * Removes path from NFC.
     * 
     * @param path
     */
    void removeFromNotFoundCache( ResourceStoreRequest request );

    /**
     * Is NFC active? (true by default)
     * 
     * @return
     */
    boolean isNotFoundCacheActive();

    /**
     * Sets is NFC active.
     * 
     * @param notFoundCacheActive
     */
    void setNotFoundCacheActive( boolean notFoundCacheActive );

    // ==================================================
    // LocalStorage et al

    /**
     * Returns the Repository specific MIME rules source.
     * 
     * @return
     * @since 2.0
     */
    MimeRulesSource getMimeRulesSource();

    /**
     * Returns the attribute handler used by repository.
     */
    AttributesHandler getAttributesHandler();

    /**
     * Sets attribute handler used by repository.
     * 
     * @param attributesHandler
     */
    void setAttributesHandler( AttributesHandler attributesHandler );

    /**
     * Returns the local URL of this repository, if any.
     * 
     * @return local url of this repository, null otherwise.
     */
    String getLocalUrl();

    /**
     * Sets the local url.
     * 
     * @param url the new local url
     */
    void setLocalUrl( String url )
        throws StorageException;

    /**
     * Gets local status.
     */
    LocalStatus getLocalStatus();

    /**
     * Sets local status.
     * 
     * @param val the val
     */
    void setLocalStatus( LocalStatus val );

    /**
     * Returns repository specific local storage context.
     * 
     * @return null if none
     */
    LocalStorageContext getLocalStorageContext();

    /**
     * Returns the local storage of the repository. Per repository instance may exists.
     * 
     * @return localStorage or null.
     */
    LocalRepositoryStorage getLocalStorage();

    /**
     * Sets the local storage of the repository. May be null if this is an aggregating repos without caching function.
     * Per repository instance may exists.
     * 
     * @param storage the storage
     */
    void setLocalStorage( LocalRepositoryStorage storage );

    /**
     * Gets the published mirrors.
     * 
     * @return
     */
    PublishedMirrors getPublishedMirrors();

    // ==================================================
    // Behaviour

    /**
     * Returns the list of defined request processors.
     * 
     * @return
     */
    Map<String, RequestProcessor> getRequestProcessors();

    /**
     * If is user managed, the nexus core and nexus core UI handles the store. Thus, for reposes, users are allowed to
     * edit/drop the repository.
     * 
     * @return
     */
    boolean isUserManaged();

    /**
     * Sets is the store user managed.
     * 
     * @param val
     */
    void setUserManaged( boolean val );

    /**
     * Tells whether the resource store is exposed as Nexus content or not.
     * 
     * @return
     */
    boolean isExposed();

    /**
     * Sets the exposed flag.
     * 
     * @param val
     */
    void setExposed( boolean val );

    /**
     * Is Repository listable?.
     * 
     * @return true if is listable, otherwise false.
     */
    boolean isBrowseable();

    /**
     * Sets the listable property of repository. If true, its content will be returned by listItems method, otherwise
     * not. The retrieveItem will still function and return the requested item.
     * 
     * @param val the val
     */
    void setBrowseable( boolean val );

    /**
     * Specifies if the repo is write, readonly, or single deploy.
     * 
     * @return the write policy for this repository.
     */
    RepositoryWritePolicy getWritePolicy();

    /**
     * Sets the write policy for the repo. See {@link RepositoryWritePolicy}. This does not affect the in-repository
     * caching using LocalStorage. It just says, that from the "outer" perspective, this repo behaves like read-only
     * repository, and deployment is disabled for example.
     * 
     * @param val the val
     */
    void setWritePolicy( RepositoryWritePolicy writePolicy );

    /**
     * Is Repository indexable?.
     * 
     * @return true if is indexable, otherwise false.
     */
    boolean isIndexable();

    /**
     * Sets the indexable property of repository. If true, its content will be indexed by Indexer, otherwise not.
     * 
     * @param val the val
     */
    void setIndexable( boolean val );

    /**
     * Is Repository searchable?.
     * 
     * @return true if is searchable, otherwise false.
     */
    boolean isSearchable();

    /**
     * Sets the searchable property of repository. If true, its content will be searched by Indexer (when doing
     * "global", non targeted searches), otherwise not.
     * 
     * @param val the val
     */
    void setSearchable( boolean val );

    // ==================================================
    // Maintenance

    /**
     * Expires all the caches used by this repository implementation from path and below. What kind of caches are
     * tackled depends on the actual implementation behind this interface (NFC, proxy cache or something third). To gain
     * more control, you can call corresponding methods manually too. Currently, this method equals to a single call to
     * {@link #expireNotFoundCaches(ResourceStoreRequest)} on hosted repositories, and on a sequential calls of
     * {@link ProxyRepository#expireProxyCaches(ResourceStoreRequest)} and
     * {@link #expireNotFoundCaches(ResourceStoreRequest)} on proxy repositories. Moreover, on group repositories, this
     * call is propagated to it's member repositories!
     * 
     * @param path a path from to start descending. If null, it is taken as "root".
     */
    void expireCaches( ResourceStoreRequest request );

    /**
     * Purges the NFC caches from path and below.
     * 
     * @param path
     */
    void expireNotFoundCaches( ResourceStoreRequest request );

    /**
     * Evicts items that were last used before timestamp.
     * 
     * @param timestamp
     */
    Collection<String> evictUnusedItems( ResourceStoreRequest request, long timestamp );

    /**
     * Forces the recreation of attributes on this repository.
     * 
     * @param initialData the initial data
     * @return true, if recreate attributes
     */
    boolean recreateAttributes( ResourceStoreRequest request, Map<String, String> initialData );

    /**
     * Returns the repository level AccessManager. Per repository instance may exists.
     * 
     * @return the access manager
     */
    AccessManager getAccessManager();

    /**
     * Sets the repository level AccessManager. Per repository instance may exists.
     * 
     * @param accessManager the access manager
     */
    void setAccessManager( AccessManager accessManager );

    // ==================================================
    // Alternative (and unprotected) Content access
    // THESE ARE DEPRECATED! They used as circumvention for tasks running without valid JSec subject

    @Deprecated
    StorageItem retrieveItem( boolean fromTask, ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException;

    @Deprecated
    void copyItem( boolean fromTask, ResourceStoreRequest from, ResourceStoreRequest to )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

    @Deprecated
    void moveItem( boolean fromTask, ResourceStoreRequest from, ResourceStoreRequest to )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

    @Deprecated
    void deleteItem( boolean fromTask, ResourceStoreRequest request )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

    @Deprecated
    Collection<StorageItem> list( boolean fromTask, ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException;

    // Alternative content access
    // These will stay!

    void storeItem( boolean fromTask, StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException;

    Collection<StorageItem> list( boolean fromTask, StorageCollectionItem item )
        throws IllegalOperationException, ItemNotFoundException, StorageException;
}
