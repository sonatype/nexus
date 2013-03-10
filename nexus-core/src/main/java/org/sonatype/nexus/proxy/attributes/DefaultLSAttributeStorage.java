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
package org.sonatype.nexus.proxy.attributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.google.common.base.Preconditions;

/**
 * AttributeStorage implementation that uses LocalRepositoryStorage of repositories to store attributes "along" the
 * artifacts (well, not along but in same storage but hidden).
 * 
 * @author cstamas
 */
@Typed( AttributeStorage.class )
@Named( "ls" )
@Singleton
public class DefaultLSAttributeStorage
    extends AbstractAttributeStorage
    implements AttributeStorage
{
    private static final String ATTRIBUTE_PATH_PREFIX = "/.nexus/attributes";

    private final Marshaller marshaller;

    /**
     * Instantiates a new FSX stream attribute storage.
     */
    @Inject
    public DefaultLSAttributeStorage()
    {
        this( new JacksonJSONMarshaller() );
    }

    /**
     * Instantiates a new FSX stream attribute storage.
     */
    public DefaultLSAttributeStorage( final Marshaller marshaller )
    {
        this.marshaller = Preconditions.checkNotNull( marshaller );
        getLogger().info( "Default FS AttributeStorage in place, using {} marshaller.", marshaller );
    }

    public boolean deleteAttributes( final RepositoryItemUid uid )
        throws IOException
    {
        final RepositoryItemUidLock uidLock = uid.getLock();

        uidLock.lock( Action.delete );

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Deleting attributes on UID=" + uid.toString() );
            }

            try
            {
                final Repository repository = uid.getRepository();

                final ResourceStoreRequest request =
                    new ResourceStoreRequest( getAttributePath( repository, uid.getPath() ) );

                repository.getLocalStorage().deleteItem( repository, request );

                return true;
            }
            catch ( ItemNotFoundException e )
            {
                // ignore it
            }
            catch ( UnsupportedStorageOperationException e )
            {
                // ignore it
            }

            return false;
        }
        finally
        {
            uidLock.unlock();
        }
    }

    public Attributes getAttributes( final RepositoryItemUid uid )
        throws IOException
    {
        final RepositoryItemUidLock uidLock = uid.getLock();

        uidLock.lock( Action.read );

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Loading attributes on UID=" + uid.toString() );
            }

            return doGetAttributes( uid );
        }
        finally
        {
            uidLock.unlock();
        }
    }

    public void putAttributes( final RepositoryItemUid uid, Attributes attributes )
        throws IOException
    {
        final RepositoryItemUidLock uidLock = uid.getLock();

        uidLock.lock( Action.create );

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Storing attributes on UID=" + uid.toString() );
            }

            try
            {
                Attributes onDisk = doGetAttributes( uid );

                if ( onDisk != null && ( onDisk.getGeneration() > attributes.getGeneration() ) )
                {
                    // change detected, overlay the to be saved onto the newer one and swap
                    onDisk.overlayAttributes( attributes );

                    // and overlay other things too
                    onDisk.setRepositoryId( uid.getRepository().getId() );
                    onDisk.setPath( uid.getPath() );
                    onDisk.setReadable( attributes.isReadable() );
                    onDisk.setWritable( attributes.isWritable() );

                    attributes = onDisk;
                }

                attributes.incrementGeneration();

                final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                marshaller.marshal( attributes, bos );

                final Repository repository = uid.getRepository();

                final DefaultStorageFileItem attributeItem =
                    new DefaultStorageFileItem( repository, new ResourceStoreRequest( getAttributePath( repository,
                        uid.getPath() ) ), true, true, new ByteArrayContentLocator( bos.toByteArray(), "text/xml" ) );

                repository.getLocalStorage().storeItem( repository, attributeItem );
            }
            catch ( UnsupportedStorageOperationException ex )
            {
                // TODO: what here? Is local storage unsuitable for storing attributes?
                getLogger().error( "Got UnsupportedStorageOperationException during store of UID=" + uid.toString(), ex );
            }
        }
        finally
        {
            uidLock.unlock();
        }
    }

    // ==

    protected String getAttributePath( final Repository repository, final String path )
    {
        if ( path.startsWith( RepositoryItemUid.PATH_SEPARATOR ) )
        {
            return ATTRIBUTE_PATH_PREFIX + path;
        }
        else
        {
            return ATTRIBUTE_PATH_PREFIX + RepositoryItemUid.PATH_SEPARATOR + path;
        }
    }

    // ==

    /**
     * Gets the attributes.
     * 
     * @param uid the uid
     * @return the attributes
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Attributes doGetAttributes( RepositoryItemUid uid )
        throws IOException
    {
        Attributes result = null;

        InputStream attributeStream = null;

        boolean corrupt = false;

        try
        {
            final Repository repository = uid.getRepository();

            AbstractStorageItem attributeItemCandidate =
                repository.getLocalStorage().retrieveItem( repository,
                    new ResourceStoreRequest( getAttributePath( repository, uid.getPath() ) ) );

            if ( attributeItemCandidate instanceof StorageFileItem )
            {
                StorageFileItem attributeItem = (StorageFileItem) attributeItemCandidate;

                if ( attributeItem.getLength() == 0 )
                {
                    // NEXUS-4871
                    throw new InvalidInputException( "Attribute of " + uid + " is empty!" );
                }

                attributeStream = attributeItem.getContentLocator().getContent();

                result = marshaller.unmarshal( attributeStream );

                result.setRepositoryId( uid.getRepository().getId() );
                result.setPath( uid.getPath() );

                // fixing remoteChecked
                if ( result.getCheckedRemotely() == 0 || result.getCheckedRemotely() == 1 )
                {
                    result.setCheckedRemotely( System.currentTimeMillis() );
                    result.setExpired( true );
                }

                // fixing lastRequested
                if ( result.getLastRequested() == 0 )
                {
                    result.setLastRequested( System.currentTimeMillis() );
                }
            }
        }
        catch ( InvalidInputException e )
        {
            if ( getLogger().isDebugEnabled() )
            {
                // we log the stacktrace
                getLogger().info( "Attributes of " + uid + " are corrupt, deleting it.", e );
            }
            else
            {
                // just remark about this
                getLogger().info( "Attributes of " + uid + " are corrupt, deleting it." );
            }

            corrupt = true;
        }
        catch ( IOException e )
        {
            getLogger().warn( "While reading attributes of " + uid + " we got IOException:", e );

            throw e;
        }
        catch ( ItemNotFoundException e )
        {
            return null;
        }
        finally
        {
            IOUtil.close( attributeStream );
        }

        if ( corrupt )
        {
            deleteAttributes( uid );
        }

        return result;
    }
}
