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

package org.sonatype.nexus.proxy.maven.maven2;

import junit.framework.Assert;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.M2TestsuiteEnvironmentBuilder;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.thread.ThreadSupport;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for M2 Repository.
 */
public class M2RepositoryTest
    extends AbstractProxyTestEnvironment
{

    @Override
    protected EnvironmentBuilder getEnvironmentBuilder()
        throws Exception
    {
        ServletServer ss = (ServletServer) lookup( ServletServer.ROLE );
        return new M2TestsuiteEnvironmentBuilder( ss );
    }


    /**
     * Make a request to a maven-metadata.xml and the correpsonding maven-metadata.xml.sha1 to test against a dead lock. </BR>
     * NOTE: the timeout is set to 10 seconds, this will NOT be triggered except in the case of a dead lock
     * (or I guess on a really slow machine).
     * @throws Exception
     */
    @Test( timeout = 10000 )
    public void testMetadataProxyLocking()
        throws Exception
    {

        final List<Throwable> errors = Collections.synchronizedList( new ArrayList<Throwable>() );

        final M2Repository m2Repository = (M2Repository) getRepositoryRegistry().getRepository( "repo2" );

        RequestThread metadataRequestTread = new RequestThread( "merge-version/maven-metadata.xml", m2Repository, errors );
        RequestThread metadataSha1RequestTread = new RequestThread( "merge-version/maven-metadata.xml.sha1", m2Repository, errors );

        // execute the threads
        metadataRequestTread.start();
        metadataSha1RequestTread.start();

        // wait for them to finish
        metadataRequestTread.join();
        metadataSha1RequestTread.join();

        // check for errors, any exceptions in the threads will be logged
        if ( !errors.isEmpty() )
        {
            Assert.fail( "request threads failed: " + errors );
        }
    }

    /**
     * Downloads a file from a repository.  NOTE: currently only suitable for small text based files.
     */
    class RequestThread
        extends ThreadSupport
    {
        private final String requestPath;
        private final Repository repository;
        private final List<Throwable> errors; // to record errors for test result


        public RequestThread( String requestPath, Repository repository, List<Throwable> errors )
        {
            this.requestPath = requestPath;
            this.repository = repository;
            this.errors = errors;
            this.setName( requestPath );
        }

        @Override
        public void doRun()
            throws Exception
        {
            ResourceStoreRequest request = new ResourceStoreRequest( requestPath );
            request.getRequestContext().put( AccessManager.REQUEST_AGENT, "Java from-test" ); // starts with java to generate m2 metadata
            request.getRequestContext().put( AccessManager.REQUEST_REMOTE_ADDRESS, "127.0.0.1" ); // needs to be a remote request to gen the m2 metadata

            StorageFileItem item = (StorageFileItem) repository.retrieveItem( request );
            InputStream inputStream = null;

            // reading of the stream is not technically needed, but we could pretend we are a real client.
            try
            {
                inputStream = item.getInputStream();
                IOUtil.toString( inputStream );// just metadata, read it and ignore.
            }
            finally
            {
                IOUtil.close( inputStream );
            }
        }

        @Override
        protected void onFailure( Throwable cause )
        {
            // report error
            errors.add( cause );
        }
    }

}
