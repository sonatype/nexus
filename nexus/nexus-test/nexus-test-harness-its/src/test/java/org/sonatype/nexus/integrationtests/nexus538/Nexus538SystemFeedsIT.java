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
package org.sonatype.nexus.integrationtests.nexus538;

import java.util.List;

import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.test.utils.EventInspectorsUtil;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class Nexus538SystemFeedsIT
    extends AbstractNexusIntegrationTest
{

    @Test
    public void bootEventTest()
        throws Exception
    {
        TaskScheduleUtil.waitForAllTasksToStop();

        SyndFeed feed = FeedUtil.getFeed( "systemChanges" );
        this.validateLinksInFeeds( feed );
        Assert.assertTrue( findFeedEntry( feed, "Booting", null ) );
    }

    @Test( dependsOnMethods = { "bootEventTest" } )
    public void updateRepoTest()
        throws Exception
    {
        // change the name of the test repo
        RepositoryMessageUtil repoUtil =
            new RepositoryMessageUtil( this, this.getXMLXStream(), MediaType.APPLICATION_XML );

        RepositoryBaseResource repo = repoUtil.getRepository( this.getTestRepositoryId() );
        String oldName = repo.getName();
        String newName = repo.getName() + "-new";
        repo.setName( newName );
        repoUtil.updateRepo( repo );

        TaskScheduleUtil.waitForAllTasksToStop();
        getEventInspectorsUtil().waitForCalmPeriod();

        final SyndFeed feed = FeedUtil.getFeed( "systemChanges" );
        this.validateLinksInFeeds( feed );
        Assert.assertTrue( findFeedEntry( feed, "Configuration change", new String[] { newName, oldName } ),
            "Update repo feed not found\r\n\r\n" + feed );
    }

    @Test( dependsOnMethods = { "updateRepoTest" } )
    public void changeProxyStatusTest()
        throws Exception
    {
        // change the name of the test repo
        RepositoryMessageUtil repoUtil =
            new RepositoryMessageUtil( this, this.getXMLXStream(), MediaType.APPLICATION_XML );

        RepositoryStatusResource repo = repoUtil.getStatus( "release-proxy-repo-1" );
        repo.setProxyMode( ProxyMode.BLOCKED_AUTO.name() );
        repoUtil.updateStatus( repo );

        TaskScheduleUtil.waitForAllTasksToStop();
        getEventInspectorsUtil().waitForCalmPeriod();

        SyndFeed systemFeed = FeedUtil.getFeed( "systemChanges" );
        this.validateLinksInFeeds( systemFeed );

        SyndFeed systemStatusFeed = FeedUtil.getFeed( "systemRepositoryStatusChanges" );
        this.validateLinksInFeeds( systemStatusFeed );

        Assert.assertTrue( findFeedEntry( systemFeed, "Repository proxy mode change",
            new String[] { "release-proxy-repo-1" } ) );

        Assert.assertTrue( findFeedEntry( systemStatusFeed, "Repository proxy mode change",
            new String[] { "release-proxy-repo-1" } ) );
    }

    @SuppressWarnings( "unchecked" )
    private boolean findFeedEntry( SyndFeed feed, String title, String[] bodyPortions )
    {
        List<SyndEntry> entries = feed.getEntries();

        for ( SyndEntry entry : entries )
        {
            if ( entry.getTitle().equals( title ) )
            {
                if ( bodyPortions == null )
                {
                    return true;
                }

                boolean missingPortion = false;

                SyndContent description = entry.getDescription();
                String value = description.getValue();
                for ( int i = 0; i < bodyPortions.length; i++ )
                {
                    if ( !value.contains( bodyPortions[i] ) )
                    {
                        missingPortion = true;
                        break;
                    }
                }

                if ( !missingPortion )
                {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings( "unchecked" )
    private void validateLinksInFeeds( SyndFeed feed )
    {
        Assert.assertTrue( feed.getLink().startsWith( this.getBaseNexusUrl() ), "Feed link is wrong" );

        List<SyndEntry> entries = feed.getEntries();

        for ( SyndEntry syndEntry : entries )
        {
            Assert.assertNotNull( "Feed item link is empty.", syndEntry.getLink() );
            Assert.assertTrue( syndEntry.getLink().startsWith( this.getBaseNexusUrl() ),
                "Feed item link is wrong, is: " + syndEntry.getLink() );
        }
    }
}
