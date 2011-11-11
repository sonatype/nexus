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
package org.sonatype.nexus.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.logging.Slf4jPlexusLogger;
import org.sonatype.nexus.proxy.attributes.AttributesHandler;
import org.sonatype.nexus.proxy.attributes.DefaultFSAttributeStorage;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteProviderHintFactory;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plexus.appevents.EventListener;

/**
 * The Class AbstractProxyTestEnvironment.
 *
 * @author cstamas
 */
public abstract class AbstractProxyTestEnvironment
    extends AbstractNexusTestEnvironment
{

    /** The logger. */
    private Logger logger = Slf4jPlexusLogger.getPlexusLogger( getClass() );

    /** The config */
    private ApplicationConfiguration applicationConfiguration;

    /** The app event hub */
    private ApplicationEventMulticaster applicationEventMulticaster;

    /** The repository registry. */
    private RepositoryRegistry repositoryRegistry;

    /** The local repository storage. */
    private AttributesHandler attributesHandler;

    /** The local repository storage. */
    private LocalRepositoryStorage localRepositoryStorage;

    /** The remote repository storage. */
    private RemoteRepositoryStorage remoteRepositoryStorage;

    /**
     * The hint provider for remote repository storage.
     */
    private RemoteProviderHintFactory remoteProviderHintFactory;

    /** The root router */
    private RepositoryRouter rootRouter;

    /** The test listener */
    private TestItemEventListener testEventListener;

    public ApplicationConfiguration getApplicationConfiguration()
    {
        return applicationConfiguration;
    }

    public ApplicationEventMulticaster getApplicationEventMulticaster()
    {
        return applicationEventMulticaster;
    }

    /**
     * Gets the repository registry.
     *
     * @return the repository registry
     */
    public RepositoryRegistry getRepositoryRegistry()
    {
        return repositoryRegistry;
    }

    /**
     * Sets the repository registry.
     *
     * @param repositoryRegistry the new repository registry
     */
    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }

    /**
     * Gets the local repository storage.
     *
     * @return the local repository storage
     */
    public LocalRepositoryStorage getLocalRepositoryStorage()
    {
        return localRepositoryStorage;
    }

    /**
     * Sets the local repository storage.
     *
     * @param localRepositoryStorage the new local repository storage
     */
    public void setLocalRepositoryStorage( LocalRepositoryStorage localRepositoryStorage )
    {
        this.localRepositoryStorage = localRepositoryStorage;
    }

    /**
     * Gets the remote repository storage.
     *
     * @return the remote repository storage
     */
    public RemoteRepositoryStorage getRemoteRepositoryStorage()
    {
        return remoteRepositoryStorage;
    }

    /**
     * Sets the remote repository storage.
     *
     * @param remoteRepositoryStorage the new remote repository storage
     */
    public void setRemoteRepositoryStorage( RemoteRepositoryStorage remoteRepositoryStorage )
    {
        this.remoteRepositoryStorage = remoteRepositoryStorage;
    }

    public RemoteProviderHintFactory getRemoteProviderHintFactory()
    {
        return remoteProviderHintFactory;
    }

    public void setRemoteProviderHintFactory( RemoteProviderHintFactory remoteProviderHintFactory )
    {
        this.remoteProviderHintFactory = remoteProviderHintFactory;
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * Gets the root router.
     *
     * @return
     */
    public RepositoryRouter getRootRouter()
    {
        return rootRouter;
    }

    /**
     * Gets the test event listener.
     *
     * @return
     */
    public TestItemEventListener getTestEventListener()
    {
        return testEventListener;
    }

    /*
     * (non-Javadoc)
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        applicationConfiguration = lookup( ApplicationConfiguration.class );

        applicationEventMulticaster = lookup( ApplicationEventMulticaster.class );

        repositoryRegistry = lookup( RepositoryRegistry.class );

        testEventListener = new TestItemEventListener();

        applicationEventMulticaster.addEventListener( testEventListener );

        attributesHandler = lookup( AttributesHandler.class );

        if ( attributesHandler.getAttributeStorage() instanceof DefaultFSAttributeStorage )
        {
            ( (DefaultFSAttributeStorage) attributesHandler.getAttributeStorage() ).setWorkingDirectory( getApplicationConfiguration().getWorkingDirectory(
                "proxy/attributes" ) );
        }

        localRepositoryStorage = lookup( LocalRepositoryStorage.class, "file" );

        remoteProviderHintFactory = lookup( RemoteProviderHintFactory.class );

        remoteRepositoryStorage = lookup( RemoteRepositoryStorage.class, remoteProviderHintFactory.getDefaultHttpRoleHint() );

        rootRouter = lookup( RepositoryRouter.class );

        getEnvironmentBuilder().buildEnvironment( this );

        applicationEventMulticaster.notifyEventListeners( new ConfigurationChangeEvent( applicationConfiguration, null,
            null ) );

        applicationEventMulticaster.notifyEventListeners( new NexusStartedEvent( null ) );

        getEnvironmentBuilder().startService();
    }

    /*
     * (non-Javadoc)
     * @see org.codehaus.plexus.PlexusTestCase#tearDown()
     */
    @Override
    public void tearDown()
        throws Exception
    {
        getEnvironmentBuilder().stopService();

        super.tearDown();
    }

    /**
     * Gets the environment builder.
     *
     * @return the environment builder
     */
    protected abstract EnvironmentBuilder getEnvironmentBuilder()
        throws Exception;

    /**
     * Check for file and match contents.
     *
     * @param item the item
     * @return true, if successful
     */
    protected void checkForFileAndMatchContents( StorageItem item )
        throws Exception
    {
        // file exists
        assertTrue( new File( getBasedir(), "target/test-classes/"
            + item.getRepositoryItemUid().getRepository().getId() + item.getRepositoryItemUid().getPath() ).exists() );
        // match content
        checkForFileAndMatchContents( item, new File( getBasedir(), "target/test-classes/"
            + item.getRepositoryItemUid().getRepository().getId() + item.getRepositoryItemUid().getPath() ) );
    }

    protected void checkForFileAndMatchContents( StorageItem item, StorageFileItem expected )
        throws Exception
    {
        assertStorageFileItem( item );

        StorageFileItem fileItem = (StorageFileItem) item;

        assertTrue( "content equals", contentEquals( fileItem.getInputStream(), expected.getInputStream() ) );
    }

    /**
     * Check for file and match contents.
     *
     * @param item the item
     * @param expected the wanted content
     * @throws Exception the exception
     */
    protected void checkForFileAndMatchContents( StorageItem item, File expected )
        throws Exception
    {
        assertStorageFileItem( item );

        StorageFileItem fileItem = (StorageFileItem) item;

        assertTrue( "content equals", contentEquals( fileItem.getInputStream(), new FileInputStream( expected ) ) );
    }

    protected void assertStorageFileItem( StorageItem item )
    {
        // is file
        assertTrue( item instanceof StorageFileItem );

        // is non-virtual
        assertFalse( item.isVirtual() );

        // have UID
        assertTrue( item.getRepositoryItemUid() != null );

        StorageFileItem file = (StorageFileItem) item;

        // is reusable
        assertTrue( file.isReusableStream() );
    }

    protected File getFile( Repository repository, String path )
        throws IOException
    {
        return new File( getApplicationConfiguration().getWorkingDirectory(), "proxy/store/" + repository.getId()
            + path );
    }

    protected File getRemoteFile( Repository repository, String path )
        throws IOException
    {
        return new File( getBasedir(), "target/test-classes/" + repository.getId() + path );
    }

    protected void saveItemToFile( StorageFileItem item, File file )
        throws IOException
    {
        FileOutputStream fos = null;
        InputStream is = null;
        try
        {
            is = item.getInputStream();
            fos = new FileOutputStream( file );
            IOUtil.copy( is, fos );
            fos.flush();
        }
        finally
        {
            IOUtil.close( is );
            IOUtil.close( fos );
        }

    }

    public PlexusContainer getPlexusContainer()
    {
        return this.getContainer();
    }

    protected class TestItemEventListener
        implements EventListener
    {
        private List<Event> events = new ArrayList<Event>();

        public List<Event> getEvents()
        {
            return events;
        }

        public Event getFirstEvent()
        {
            if ( events.size() > 0 )
            {
                return events.get( 0 );
            }
            else
            {
                return null;
            }
        }

        public Event getLastEvent()
        {
            if ( events.size() > 0 )
            {
                return events.get( events.size() - 1 );
            }
            else
            {
                return null;
            }
        }

        public void reset()
        {
            events.clear();
        }

        public void onEvent( Event evt )
        {
            if ( RepositoryItemEvent.class.isAssignableFrom( evt.getClass() ) )
            {
                events.add( evt );
            }
        }
    }

    protected class TestRepositoryEventListener
        implements EventListener
    {
        private List<Event> events = new ArrayList<Event>();

        public List<Event> getEvents()
        {
            return events;
        }

        public Event getFirstEvent()
        {
            if ( events.size() > 0 )
            {
                return events.get( 0 );
            }
            else
            {
                return null;
            }
        }

        public Event getLastEvent()
        {
            if ( events.size() > 0 )
            {
                return events.get( events.size() - 1 );
            }
            else
            {
                return null;
            }
        }

        public void reset()
        {
            events.clear();
        }

        public void onEvent( Event evt )
        {
            if ( !RepositoryItemEvent.class.isAssignableFrom( evt.getClass() ) )
            {
                events.add( evt );
            }
        }
    }

    protected Metadata readMetadata( File mdf )
        throws Exception
    {
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();
        InputStreamReader isr = null;
        Metadata md = null;
        try
        {
            isr = new InputStreamReader( new FileInputStream( mdf ) );
            md = metadataReader.read( isr );
        }
        finally
        {
            IOUtil.close( isr );
        }
        return md;
    }

    protected String contentAsString( StorageItem item )
        throws IOException
    {
        InputStream is = ( (StorageFileItem) item ).getInputStream();
        try
        {
            return IOUtil.toString( is, "UTF-8", 1024 );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    protected File createTempFile( String prefix, String sufix )
        throws IOException
    {
        final File tmpDir = new File( getWorkHomeDir(), "ftemp" );
        tmpDir.mkdirs();

        return File.createTempFile( prefix, sufix, tmpDir );
    }
}
