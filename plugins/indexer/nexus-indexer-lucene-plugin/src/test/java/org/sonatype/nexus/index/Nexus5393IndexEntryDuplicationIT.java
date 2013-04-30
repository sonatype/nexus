/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.index;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.jettytestsuite.BlockingServer;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;

/**
 * UT for NEXUS-5393: Proxy repositories are getting duplicate entries for locally cached entries.
 * <p/>
 * Manual steps to reproduce:
 * 1. start nexus
 * 2. enable index download on central, wait for indexes to get downloaded
 * 3. verify indexes are okay for log4j-1.2.13 (and have one entry)
 * 4. pull log4j-1.2.13 into central (localhost:8081/nexus/content/repositories/central/log4j/log4j/1.2.13/log4j-1.2.13.pom then localhost:8081/nexus/content/repositories/central/log4j/log4j/1.2.13/log4j-1.2.13.jar using wget/curl)
 * 5. verify same assertion as in step 3.
 * 6. perform Update Indexes on Central
 * 7. verify you have two entries for log4j-1.2.13
 *
 * Validated against master on 9f9748aa9cdeefa9548b4487c2057223136c511b
 * this UT fails. Branch scanning-issues make it pass.
 */
public class Nexus5393IndexEntryDuplicationIT
    extends AbstractIndexerManagerTest
{

    private Server server;

    private File fakeCentral;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        fakeCentral = new File( getBasedir(), "target/test-classes/nexus-5393/remote-repository" );

        // create proxy server
        ServerSocket s = new ServerSocket( 0 );
        int port = s.getLocalPort();
        s.close();

        server = new BlockingServer( port );

        ResourceHandler resource_handler = new ResourceHandler()
        {
            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request,
                HttpServletResponse response )
                throws IOException, ServletException
            {
                System.out.print( "JETTY: " + target );
                super.handle( target, baseRequest, request, response );
                System.out.println( "  ::  " + response.getStatus() );
            }
        };
        resource_handler.setResourceBase( fakeCentral.getAbsolutePath() );
        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[]{ resource_handler, new DefaultHandler() } );
        server.setHandler( handlers );

        System.out.print( "JETTY Started on port: " + port );
        server.start();

        // update central to use proxy server
        central.setDownloadRemoteIndexes( false );
        central.setRemoteUrl( "http://localhost:" + port );
        central.setRepositoryPolicy( RepositoryPolicy.RELEASE );
        nexusConfiguration.saveConfiguration();

        // wait a bit for async stuff
        waitForAsync();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        server.stop();
        super.tearDown();
    }

    protected void waitForAsync()
        throws Exception
    {
        // wait a bit for async stuff
        Thread.sleep( 100 );
        wairForAsyncEventsToCalmDown();
        waitForTasksToStop();
    }


    protected void enableIndexDownload()
        throws Exception
    {
        central.setDownloadRemoteIndexes( true );
        nexusConfiguration.saveConfiguration();
        waitForAsync();
    }

    protected void ensureUniqueness()
        throws IOException
    {
        final IndexingContext context = indexerManager.getRepositoryIndexContext( "central" );
        final HashSet<String> uinfos = new HashSet<String>();
        final ArrayList<String> duplicates = new ArrayList<String>();
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try
        {
            final IndexReader r = indexSearcher.getIndexReader();
            for ( int i = 0; i < r.maxDoc(); i++ )
            {
                if ( !r.isDeleted( i ) )
                {
                    final Document d = r.document( i );
                    String uinfo = d.get( ArtifactInfo.UINFO );
                    if ( uinfo != null && !uinfos.add( uinfo ) )
                    {
                        duplicates.add( uinfo );
                    }
                }
            }
        }
        finally
        {
            context.releaseIndexSearcher( indexSearcher );
        }

        // remote proxy contains only one artifact: log4j-1.2.13: so we expect out index to have no
        // dupes and only one artifact
        if ( !duplicates.isEmpty() || uinfos.size() > 1 )
        {
            Assert.fail( "UINFOs are duplicated or we scanned some unexpected ones, duplicates=" + duplicates + ", uinfos=" + uinfos );
        }
    }

    @Test
    public void entryDuplicationTestWithUpdateIndex()
        throws Exception
    {
        // enable index download
        enableIndexDownload();
        // check uniqueness
        ensureUniqueness();
        // simulate Maven fetch, get something cached
        central.retrieveItem( new ResourceStoreRequest( "/log4j/log4j/1.2.13/log4j-1.2.13.pom" ) );
        central.retrieveItem( new ResourceStoreRequest( "/log4j/log4j/1.2.13/log4j-1.2.13.jar" ) );
        waitForAsync(); // indexing happens async
        // check uniqueness
        ensureUniqueness();
        // update indexes, that will trigger buggy scan of local storage
        indexerManager.reindexRepository( null, central.getId(), false );
        // check uniqueness
        ensureUniqueness();
    }

    @Test
    public void entryDuplicationTestWithRepairIndex()
        throws Exception
    {
        // enable index download
        enableIndexDownload();
        // check uniqueness
        ensureUniqueness();
        // simulate Maven fetch, get something cached
        central.retrieveItem( new ResourceStoreRequest( "/log4j/log4j/1.2.13/log4j-1.2.13.pom" ) );
        central.retrieveItem( new ResourceStoreRequest( "/log4j/log4j/1.2.13/log4j-1.2.13.jar" ) );
        waitForAsync(); // indexing happens async
        // check uniqueness
        ensureUniqueness();
        // repair indexes, that will trigger buggy scan of local storage
        indexerManager.reindexRepository( null, central.getId(), true );
        // check uniqueness
        ensureUniqueness();
    }
}
