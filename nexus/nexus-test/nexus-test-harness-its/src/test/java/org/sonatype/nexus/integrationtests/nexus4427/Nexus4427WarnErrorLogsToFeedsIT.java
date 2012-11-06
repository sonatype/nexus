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
package org.sonatype.nexus.integrationtests.nexus4427;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.ITHelperLogUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * See NEXUS-4427: Test that WARN/ERROR logs are recorded into Nexus feeds.
 * 
 * @author adreghiciu@gmail.com
 */
public class Nexus4427WarnErrorLogsToFeedsIT
    extends AbstractNexusIntegrationTest
{

    /**
     * When an ERROR is logged a corresponding feed entry should be created.
     */
    @Test
    public void error()
        throws Exception
    {
        String message = generateMessage( "error" );
        ITHelperLogUtils.error( message );

        // logging is asynchronous so give it a bit of time
        getEventInspectorsUtil().waitForCalmPeriod();

        assertFeedContainsEntryFor( message );
    }

    /**
     * When an WARNing is logged a corresponding feed entry should be created.
     */
    @Test
    public void warn()
        throws Exception
    {
        String message = generateMessage( "warn" );
        ITHelperLogUtils.warn( message );

        // logging is asynchronous so give it a bit of time
        getEventInspectorsUtil().waitForCalmPeriod();

        assertFeedContainsEntryFor( message );
    }

    /**
     * When an DEBUG is logged there should be no corresponding entry feed.
     */
    @Test
    public void debug()
        throws Exception
    {
        String message = generateMessage( "debug" );
        ITHelperLogUtils.debug( message );

        // logging is asynchronous so give it a bit of time
        getEventInspectorsUtil().waitForCalmPeriod();

        assertFeedDoesNotContainEntryFor( message );
    }

    /**
     * When an ERROR/WARN org.sonatype.timeline.TimelineException there should be no corresponding entry feed.
     */
    @Test
    public void timelineException()
        throws Exception
    {
        String message = generateMessage( "org.sonatype.timeline.TimelineException" );
        ITHelperLogUtils.warn( message, "org.sonatype.timeline.TimelineException", "warn" );
        ITHelperLogUtils.error( message, "org.sonatype.timeline.TimelineException" , "error" );

        // logging is asynchronous so give it a bit of time
        getEventInspectorsUtil().waitForCalmPeriod();

        assertFeedDoesNotContainEntryFor( message );
    }

    /**
     * When an ERROR/WARN org.eclipse.jetty.io.EofException there should be no corresponding entry feed.
     * 
     * !!!! We use a fake EofException as the classloader of Nexus does not see anymore jetty classes.
     */
    @Test
    public void eofException()
        throws Exception
    {
        String message = generateMessage( "org.eclipse.jetty.io.EofException" );
        ITHelperLogUtils.warn( message, "org.sonatype.nexus.plugins.ithelper.jetty.EofException", "warn" ) ;
        ITHelperLogUtils.error( message, "org.sonatype.nexus.plugins.ithelper.jetty.EofException",  "error" ) ;

        // logging is asynchronous so give it a bit of time
        getEventInspectorsUtil().waitForCalmPeriod();

        assertFeedDoesNotContainEntryFor( message );
    }

    /**
     * When an ERROR/WARN logs with specific messages there should be no corresponding entry feed.
     */
    @Test
    public void messages()
        throws Exception
    {
        String message = generateMessage( "messages" );
        ITHelperLogUtils.warn( "An exception occured writing the response entity" );
        ITHelperLogUtils.error( "An exception occured writing the response entity" );
        ITHelperLogUtils.warn( "Error while handling an HTTP server call" );
        ITHelperLogUtils.error( "Error while handling an HTTP server call" );

        // logging is asynchronous so give it a bit of time
        getEventInspectorsUtil().waitForCalmPeriod();

        assertFeedDoesNotContainEntryFor( message );
    }

    private String generateMessage( String id )
    {
        return this.getClass().getName() + "-" + System.currentTimeMillis() + "(" + id + ")";
    }

    private void assertFeedContainsEntryFor( String message )
        throws Exception
    {
        SyndFeed feed = FeedUtil.getFeed( "errorWarning" );
        @SuppressWarnings( "unchecked" )
        List<SyndEntry> entries = feed.getEntries();
        for ( SyndEntry entry : entries )
        {
            SyndContent description = entry.getDescription();
            if ( description != null && description.getValue().startsWith( message ) )
            {
                return;
            }
        }
        Assert.fail( "Feed does not contain entry for " + message );
    }

    private void assertFeedDoesNotContainEntryFor( String message )
        throws Exception
    {
        SyndFeed feed = FeedUtil.getFeed( "errorWarning" );
        @SuppressWarnings( "unchecked" )
        List<SyndEntry> entries = feed.getEntries();
        for ( SyndEntry entry : entries )
        {
            SyndContent description = entry.getDescription();
            if ( description != null && description.getValue().startsWith( message ) )
            {
                Assert.fail( "Feed contains entry for " + message );
            }
        }
    }

}
