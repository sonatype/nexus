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
package org.sonatype.nexus.integrationtests.nexus4538;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.sonatype.nexus.integrationtests.RequestFacade.clockUrlDownload;
import static org.sonatype.nexus.test.utils.FileTestingUtils.populate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;

import org.apache.maven.index.artifact.Gav;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.tasks.descriptors.RebuildAttributesTaskDescriptor;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test which makes sure that simultaneous requests for the same artifact are not serialized.
 */
public class Nexus4538ConcurrentDownloadIT
    extends AbstractNexusIntegrationTest
{

    private Gav gav;

    @BeforeMethod
    public void createFiles()
        throws Exception
    {
        gav = GavUtil.newGav( "nexus4538", "artifact", "1.0" );
        File f = new File( nexusWorkDir + "/storage/" + REPO_TEST_HARNESS_REPO, getRelitiveArtifactPath( gav ) );
        populate( f, 5 );

        TaskScheduleUtil.runTask( RebuildAttributesTaskDescriptor.ID );
    }

    @Test
    public void makeSureConcurrentDownloadisNotSerialized()
        throws Exception
    {
        String baseUrl =
            AbstractNexusIntegrationTest.nexusBaseUrl + REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_REPO + "/";
        String path = getRelitiveArtifactPath( gav );
        final URL url = new URL( baseUrl + path );

        final long op = clockUrlDownload( url );

        final Long[] time = new Long[1];
        final Throwable[] errors = new Throwable[1];
        Thread t = new Thread( new Runnable()
        {
            public void run()
            {
                try
                {
                    // limit speed to make sure it is gonna lock nexus for quite some time
                    time[0] = clockUrlDownload( url, 10 );
                }
                catch ( Exception e )
                {
                    errors[0] = e;
                    time[0] = -1L;
                }
            }
        } );
        t.setUncaughtExceptionHandler( new UncaughtExceptionHandler()
        {
            public void uncaughtException( Thread t, Throwable e )
            {
                errors[0] = e;
            }
        } );

        // let java kill it if VM wanna quit
        t.setDaemon( true );

        // start download in background
        t.start();
        for ( int i = 0; i < 10; i++ )
        {
            Thread.yield();
            Thread.sleep( 1 );
        }

        // while download is happening let's check if nexus still responsive
        final long ping = clockUrlDownload( url );

        // check if ping was not blocked by download
        assertThat( "Ping took " + ping + " original pind " + op, ping, lessThan( op * 2 ) );

        if ( time[0] != null )
        {
            assertThat( "Ping took " + ping + " dl time: " + time[0], ping, lessThan( time[0] ) );
        }
        assertThat( t.getState(), not( equalTo( State.TERMINATED ) ) );

        // check if it is error free
        if ( errors[0] != null )
        {
            ByteArrayOutputStream str = new ByteArrayOutputStream();
            PrintStream s = new PrintStream( str );
            for ( Throwable e : errors )
            {
                e.printStackTrace( s );
                s.append( "\n" );
                s.append( "\n" );
            }

            assertThat( "Found some errors downloading:\n" + str.toString(), false );
        }

        stop( t );
    }

    @SuppressWarnings( "deprecation" )
    private void stop( Thread t )
    {
        // I know, I know, shouldn't be doing this
        t.stop();
    }

}
