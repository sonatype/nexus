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
package org.sonatype.nexus.integrationtests.nexus4038;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.index.artifact.Gav;
import org.hamcrest.MatcherAssert;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.GavUtil;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class Nexus4038DeleteSystemFeedIT
    extends AbstractPrivilegeTest
{

    @Test
    public void delete()
        throws Exception
    {
        giveUserPrivilege( TEST_USER_NAME, "1000" );
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        Gav gav = GavUtil.newGav( "nexus4038", "artifact", "1.0" );
        assertTrue( Status.isSuccess( getDeployUtils().deployUsingGavWithRest( REPO_TEST_HARNESS_REPO, gav,
            getTestFile( "artifact.jar" ) ) ) );

        // timeline resolution is _one second_, so to be sure that ordering is kept he keep gaps between operations
        // bigger than one second
        Thread.sleep( 1100 );
        getEventInspectorsUtil().waitForCalmPeriod();

        TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
        TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );

        String serviceURI =
            "service/local/repositories/" + REPO_TEST_HARNESS_REPO + "/content/"
                + GavUtil.getRelitiveArtifactPath( gav ).replace( ".jar", ".pom" );
        Response response = RequestFacade.sendMessage( serviceURI, Method.DELETE );
        Status status = response.getStatus();
        Assert.assertTrue( status.isSuccess(), "Failed to delete " + gav + status );

        // timeline resolution is _one second_, so to be sure that ordering is kept he keep gaps between operations
        // bigger than one second
        Thread.sleep( 1100 );
        getEventInspectorsUtil().waitForCalmPeriod();

        SyndFeed feed = FeedUtil.getFeed( "recentlyChangedArtifacts" );

        List<SyndEntry> entries = feed.getEntries();

        Assert.assertTrue( entries.size() >= 2, "Expected more than 2 entries, but got " + entries.size() + " - "
            + entries );

        final String expected = "deleted.Action was initiated by user \"" + TEST_USER_NAME + "\"";
        boolean foundExpected = false;
        List<String> desc = new ArrayList<String>();
        for ( SyndEntry entry : entries )
        {
            final String val = entry.getDescription().getValue();
            desc.add( val );
            if(val.contains( expected )){
                foundExpected = true;
            }
        }

        // FIXME not sure why this does not compile atm on cmd line, eclipse seems happy
        // assertThat( desc, hasItem( containsString( expected ) ) );
        // HACK
        assertThat("Did not find expected string in any value", foundExpected);

    }
}
