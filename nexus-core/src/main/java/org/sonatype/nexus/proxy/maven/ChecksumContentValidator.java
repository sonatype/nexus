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

import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.repository.ItemContentValidator;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

/**
 * Maven checksum content validator.
 * 
 * @author cstamas
 */
@Component( role = ItemContentValidator.class, hint = "ChecksumContentValidator" )
public class ChecksumContentValidator
    extends AbstractChecksumContentValidator
    implements ItemContentValidator
{
    public static final String SUFFIX_MD5 = ".md5";

    public static final String SUFFIX_SHA1 = ".sha1";

    /**
     * Key of item attribute that holds contents of remote .sha1 file. The attribute is not present if the item does not
     * have corresponding .sha1 file
     */
    public static final String ATTR_REMOTE_SHA1 = "remote.sha1";

    /**
     * Key of item attribute that is set to <code>true</code> if and only if the item does not have corresponding remote
     * .sha1 file.
     */
    public static final String ATTR_NO_REMOTE_SHA1 = "remote.no-sha1";

    /**
     * Key of item attribute that holds contents of remote .md5 file. The attribute is not present if the item does not
     * have corresponding .md5 file
     */
    public static final String ATTR_REMOTE_MD5 = "remote.md5";

    /**
     * Key of item attribute that is set to <code>true</code> if and only if the item does not have corresponding remote
     * .md5 file.
     */
    public static final String ATTR_NO_REMOTE_MD5 = "remote.no-md5";

    @Override
    protected void cleanup( ProxyRepository proxy, RemoteHashResponse remoteHash, boolean contentValid )
        throws LocalStorageException
    {
        if ( !contentValid && remoteHash.getHashItem() != null )
        {
            // TODO should we remove bad checksum if policy==WARN?
            try
            {
                String path = remoteHash.getHashItem().getRepositoryItemUid().getPath();
                proxy.getLocalStorage().deleteItem( proxy, new ResourceStoreRequest( path, true ) );
            }
            catch ( ItemNotFoundException e )
            {
                // ignore
            }
            catch ( UnsupportedStorageOperationException e )
            {
                // huh?
            }
        }
    }

    @Override
    protected ChecksumPolicy getChecksumPolicy( ProxyRepository proxy, AbstractStorageItem item )
    {
        if ( isChecksum( item.getRepositoryItemUid().getPath() ) )
        {
            // do not validate checksum files
            return null;
        }

        if ( !proxy.getRepositoryKind().isFacetAvailable( MavenProxyRepository.class ) )
        {
            // we work only with maven proxy reposes, all others are neglected
            return null;
        }

        MavenProxyRepository mpr = proxy.adaptToFacet( MavenProxyRepository.class );

        ChecksumPolicy checksumPolicy = mpr.getChecksumPolicy();

        if ( checksumPolicy == null || !checksumPolicy.shouldCheckChecksum()
            || !( item instanceof DefaultStorageFileItem ) )
        {
            // there is either no need to validate or we can't validate the item content
            return null;
        }

        return checksumPolicy;
    }

    @Override
    protected RemoteHashResponse retrieveRemoteHash( AbstractStorageItem item, ProxyRepository proxy, String baseUrl )
        throws LocalStorageException
    {
        RepositoryItemUid uid = item.getRepositoryItemUid();

        ResourceStoreRequest request = new ResourceStoreRequest( item );

        RemoteHashResponse response = null;
        try
        {
            // we prefer SHA1 ...
            request.pushRequestPath( uid.getPath() + SUFFIX_SHA1 );
            try
            {
                response = doRetrieveSHA1( proxy, request, item );
            }
            finally
            {
                request.popRequestPath();
            }
        }
        catch ( ItemNotFoundException e )
        {
            // ... but MD5 will do too
            request.pushRequestPath( uid.getPath() + SUFFIX_MD5 );
            try
            {
                response = doRetrieveMD5( proxy, request, item );
            }
            catch ( ItemNotFoundException e1 )
            {
                getLogger().debug( "Item checksums (SHA1, MD5) remotely unavailable " + uid.toString() );
            }
            finally
            {
                request.popRequestPath();
            }
        }

        return response;
    }

    private boolean isChecksum( String path )
    {
        return path.endsWith( SUFFIX_SHA1 ) || path.endsWith( SUFFIX_MD5 );
    }

    public static RemoteHashResponse doRetrieveSHA1( ProxyRepository proxy, ResourceStoreRequest hashRequest,
                                                     StorageItem artifact )
        throws LocalStorageException, ItemNotFoundException
    {
        return doRetrieveChecksumItem( proxy, hashRequest, artifact, DigestCalculatingInspector.DIGEST_SHA1_KEY,
                                       ATTR_REMOTE_SHA1, ATTR_NO_REMOTE_SHA1 );
    }

    public static RemoteHashResponse doRetrieveMD5( ProxyRepository proxy, ResourceStoreRequest hashRequest,
                                                    StorageItem artifact )
        throws LocalStorageException, ItemNotFoundException
    {
        return doRetrieveChecksumItem( proxy, hashRequest, artifact, DigestCalculatingInspector.DIGEST_MD5_KEY,
                                       ATTR_REMOTE_MD5, ATTR_NO_REMOTE_MD5 );
    }

    private static RemoteHashResponse doRetrieveChecksumItem( ProxyRepository proxy, ResourceStoreRequest request,
                                                              StorageItem artifact, String inspector, String attrname,
                                                              String noattrname )
        throws ItemNotFoundException, LocalStorageException
    {
        final RepositoryItemUid itemUid = artifact.getRepositoryItemUid();
        itemUid.getLock().lock( Action.read );
        try
        {
            final Attributes attributes = artifact.getRepositoryItemAttributes();

            if ( attributes == null )
            {
                throw new LocalStorageException( "Null item repository attributes" );
            }

            if ( Boolean.parseBoolean( attributes.get( noattrname ) ) && !request.isRequestAsExpired() )
            {
                throw new ItemNotFoundException( request );
            }

            String hash = attributes.get( attrname );
            if ( hash == null || request.isRequestAsExpired() )
            {
                try
                {
                    final StorageFileItem remoteItem =
                        (StorageFileItem) proxy.getRemoteStorage().retrieveItem( proxy, request, proxy.getRemoteUrl() );
                    hash = MUtils.readDigestFromFileItem( remoteItem ); // closes http input stream
                }
                catch ( ItemNotFoundException e )
                {
                    // fall through
                }
                catch ( RemoteAccessException e )
                {
                    // fall through
                }
                catch ( RemoteStorageException e )
                {
                    // this is (potentially) transient network or remote server problem will be cached
                    // there is no automatic retry for this hash time
                    // either expire the artifact or request the hash asExpired to retry
                }

                itemUid.getLock().lock( Action.update );
                try
                {
                    if ( hash != null )
                    {
                        attributes.put( attrname, hash );
                    }
                    else
                    {
                        attributes.put( noattrname, Boolean.toString( true ) );
                    }

                    proxy.getAttributesHandler().storeAttributes( artifact );
                }
                finally
                {
                    itemUid.getLock().unlock();
                }
            }

            if ( hash != null )
            {
                return new RemoteHashResponse( inspector, hash, newHashItem( proxy, request, artifact, hash ) );
            }
            else
            {
                throw new ItemNotFoundException( request );
            }
        }
        catch ( IOException e )
        {
            throw new LocalStorageException( e );
        }
        finally
        {
            itemUid.getLock().unlock();
        }
    }

    public static DefaultStorageFileItem newHashItem( ProxyRepository proxy, ResourceStoreRequest request,
                                                      StorageItem artifact, String hash )
    {
        StringContentLocator content = new StringContentLocator( hash );
        // XXX do we need to clone request here?
        DefaultStorageFileItem hashItem =
            new DefaultStorageFileItem( proxy, request, true /* canRead */, false/* canWrite */, content );
        hashItem.setLength( content.getLength() );
        hashItem.setModified( artifact.getModified() );
        return hashItem;
    }
}
