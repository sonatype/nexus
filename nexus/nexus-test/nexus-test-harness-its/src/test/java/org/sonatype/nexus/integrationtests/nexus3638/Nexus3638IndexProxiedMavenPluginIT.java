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
package org.sonatype.nexus.integrationtests.nexus3638;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.index.artifact.Gav;
import org.hamcrest.MatcherAssert;
import org.sonatype.jettytestsuite.ControlledServer;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Nexus3638IndexProxiedMavenPluginIT
    extends AbstractNexusIntegrationTest
{

    private ControlledServer server;

    @BeforeMethod
    public void start()
        throws Exception
    {
        server = lookup( ControlledServer.class );
        server.addServer( "nexus3638", getTestFile( "repo" ), 10 );
        server.start();
    }

    @AfterMethod
    public void stop()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Test
    public void downloadSnapshotPlugin()
        throws Throwable
    {
        final Gav gav = GavUtil.newGav( "org.apache.maven.plugins", "maven-invoker-plugin", "1.6-SNAPSHOT" );

        downloadFile(
            new URL(
                "http://localhost:"
                    + TestProperties.getInteger( "webproxy-server-port" )
                    + "/nexus3638/org/apache/maven/plugins/maven-invoker-plugin/1.6-SNAPSHOT/maven-invoker-plugin-1.6-20100922.124315-3.jar" ),
            "target/downloads/nexus3638" );

        Thread[] threads = new Thread[10];
        final Throwable[] errors = new Throwable[threads.length];
        for ( int i = 0; i < threads.length; i++ )
        {
            Thread thread = new Downloader( this, gav, i, errors );

            threads[i] = thread;
        }

        // launch them all
        for ( Thread thread : threads )
        {
            thread.start();
        }

        // w8 all to finish
        for ( Thread thread : threads )
        {
            thread.join();
        }

        // check for errors
        for ( Throwable throwable : errors )
        {
            if ( throwable != null )
            {
                throw throwable;
            }
        }

        // make sure it does have enough time to index the artifact!
        TaskScheduleUtil.waitForAllTasksToStop();

        List<NexusArtifact> items =
            getSearchMessageUtil().searchForGav( gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), "nexus3638" );
        Assert.assertFalse( items.isEmpty() );
        Assert.assertEquals( "maven-plugin", items.get( 0 ).getPackaging() );

        final File nexusLog = getNexusLogFile();

        if ( nexusLog != null )
        {
            String logContent = FileUtils.readFileToString( nexusLog );
            // NEXUS-3707
            MatcherAssert.assertThat( logContent,
                not( containsString( "Rename operation failed after -1 retries in -1 ms intervals" ) ) );
            MatcherAssert.assertThat( logContent, not( containsString( "java.util.zip.ZipException" ) ) );
        }
    }
}
