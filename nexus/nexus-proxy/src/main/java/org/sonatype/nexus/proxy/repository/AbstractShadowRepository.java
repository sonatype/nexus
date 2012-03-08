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

import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;

/**
 * The Class ShadowRepository.
 * 
 * @author cstamas
 */
public abstract class AbstractShadowRepository
    extends AbstractRepository
    implements ShadowRepository
{
    @Requirement
    private RepositoryRegistry repositoryRegistry;

    protected RepositoryRegistry getRepositoryRegistry()
    {
        return repositoryRegistry;
    }

    @Override
    protected AbstractShadowRepositoryConfiguration getExternalConfiguration( boolean forModification )
    {
        return (AbstractShadowRepositoryConfiguration) super.getExternalConfiguration( forModification );
    }

    @Override
    public boolean isSynchronizeAtStartup()
    {
        return getExternalConfiguration( false ).isSynchronizeAtStartup();
    }

    @Override
    public void setSynchronizeAtStartup( final boolean val )
    {
        getExternalConfiguration( true ).setSynchronizeAtStartup( val );
    }

    @Deprecated
    @Override
    public String getMasterRepositoryId()
    {
        // NEXUS-4901: this change is to lessen the logging noise, that is otherwise harmless but ugly
        return getExternalConfiguration( false ).getMasterRepositoryId();
    }

    @Deprecated
    @Override
    public void setMasterRepositoryId( final String repositoryId )
        throws IncompatibleMasterRepositoryException, NoSuchRepositoryException
    {
        setMasterRepository( getRepositoryRegistry().getRepository( repositoryId ) );
    }

    @Override
    public Repository getMasterRepository()
    {
        try
        {
            return getRepositoryRegistry().getRepository( getExternalConfiguration( false ).getMasterRepositoryId() );
        }
        catch ( NoSuchRepositoryException e )
        {
            getLogger().warn(
                "ShadowRepository ID='" + getId() + "' cannot fetch it's master repository with ID='"
                    + getExternalConfiguration( false ).getMasterRepositoryId() + "'!", e );

            return null;
        }
    }

    @Override
    public void setMasterRepository( final Repository masterRepository )
        throws IncompatibleMasterRepositoryException
    {
        if ( getMasterRepositoryContentClass().getId().equals( masterRepository.getRepositoryContentClass().getId() ) )
        {
            getExternalConfiguration( true ).setMasterRepositoryId( masterRepository.getId() );
        }
        else
        {
            throw new IncompatibleMasterRepositoryException( this, masterRepository.getId() );
        }
    }

    /**
     * The shadow is delegating it's availability to it's master, but we can still shot down the shadow only.
     */
    @Override
    public LocalStatus getLocalStatus()
    {
        return super.getLocalStatus().shouldServiceRequest()
            && getMasterRepository().getLocalStatus().shouldServiceRequest() ? LocalStatus.IN_SERVICE
            : LocalStatus.OUT_OF_SERVICE;
    }

    @Override
    public void onRepositoryItemEvent( final RepositoryItemEvent ievt )
    {
        // is this event coming from our master?
        if ( getMasterRepository() == ievt.getRepository() )
        {
            try
            {
                if ( ievt instanceof RepositoryItemEventStore || ievt instanceof RepositoryItemEventCache )
                {
                    createLink( ievt.getItem() );
                }
                else if ( ievt instanceof RepositoryItemEventDelete )
                {
                    deleteLink( ievt.getItem() );
                }
            }
            catch ( Exception e )
            {
                getLogger().warn( "Could not sync shadow repository because of exception", e );
            }
        }
    }

    protected abstract void deleteLink( StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

    protected abstract StorageLinkItem createLink( StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException;

    protected void synchronizeLink( final StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
    {
        createLink( item );
    }

    /**
     * Synchronize with master.
     */
    @Override
    public void synchronizeWithMaster()
    {
        if ( !getLocalStatus().shouldServiceRequest() )
        {
            return;
        }

        getLogger().info( "Syncing shadow " + getId() + " with master repository " + getMasterRepository().getId() );

        final ResourceStoreRequest root = new ResourceStoreRequest( RepositoryItemUid.PATH_ROOT, true );

        expireNotFoundCaches( root );

        final AbstractFileWalkerProcessor sw = new AbstractFileWalkerProcessor()
        {
            @Override
            protected void processFileItem( WalkerContext context, StorageFileItem item )
                throws Exception
            {
                synchronizeLink( item );
            }
        };

        final DefaultWalkerContext ctx = new DefaultWalkerContext( getMasterRepository(), root );

        ctx.getProcessors().add( sw );

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
    }

    protected StorageItem doRetrieveItemFromMaster( final ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        try
        {
            return getMasterRepository().retrieveItem( request );
        }
        catch ( AccessDeniedException e )
        {
            // if client has no access to content over shadow, we just hide the fact
            throw new ItemNotFoundException( request, e );
        }
    }
}
