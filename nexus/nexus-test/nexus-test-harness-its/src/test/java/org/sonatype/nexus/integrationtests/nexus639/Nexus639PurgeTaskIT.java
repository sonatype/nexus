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
package org.sonatype.nexus.integrationtests.nexus639;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.timeline.tasks.PurgeTimelineTaskDescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * Test if the Purge Timeline Task works.
 */
public class Nexus639PurgeTaskIT
    extends AbstractNexusIntegrationTest
{

    @Test
    public void doPurgeTaskTest()
        throws Exception
    {
        // an artifact was deployed already, so test the deploy feed has something.

        SyndFeed feed = FeedUtil.getFeed( "recentlyDeployedArtifacts" );
        List<SyndEntry> entries = feed.getEntries();

        Assert.assertTrue( entries.size() > 0, "Expected artifacts in the recentlyDeployed feed." );

        // run the purge task for everything
        ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
        repo.setKey( "purgeOlderThan" );
        repo.setValue( "0" );
        TaskScheduleUtil.runTask( "purge", PurgeTimelineTaskDescriptor.ID, repo );

        // validate the feeds contain nothing.

        feed = FeedUtil.getFeed( "recentlyDeployedArtifacts" );
        entries = feed.getEntries();

        Assert.assertTrue( entries.size() == 0, "Expected ZERO artifacts in the recentlyDeployed feed." );
    }

}
