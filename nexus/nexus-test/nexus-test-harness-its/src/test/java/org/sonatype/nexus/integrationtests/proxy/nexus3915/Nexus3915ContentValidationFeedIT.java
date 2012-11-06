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
package org.sonatype.nexus.integrationtests.proxy.nexus3915;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.maven.index.artifact.Gav;
import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class Nexus3915ContentValidationFeedIT
    extends AbstractNexusProxyIntegrationTest
{

    private Gav gav;

    @BeforeMethod
    public void createGAV()
    {
        gav = GavUtil.newGav( "nexus3915", "artifact", "1.0.0" );
    }

    @Test
    public void contentValidationFeed()
        throws Exception
    {

        // make sure it is validating the content!
        RepositoryMessageUtil repoUtil = new RepositoryMessageUtil( this, getXMLXStream(), MediaType.APPLICATION_XML );
        RepositoryProxyResource repo = (RepositoryProxyResource) repoUtil.getRepository( REPO_RELEASE_PROXY_REPO1 );
        repo.setFileTypeValidation( true );
        repoUtil.updateRepo( repo );

        String msg = null;

        try
        {
            this.downloadArtifactFromRepository( REPO_RELEASE_PROXY_REPO1, gav, "target/downloads" );
            Assert.fail( "Should fail to download artifact" );
        }
        catch ( FileNotFoundException e )
        {
            // ok!
            msg = e.getMessage();
        }

        File file = new File( nexusWorkDir, "storage/release-proxy-repo-1/nexus2922/artifact/1.0.0/artifact-1.0.0.jar" );
        Assert.assertFalse( file.exists(), file.toString() );

        Assert.assertTrue( msg.contains( "404" ), msg );

        // brokenFiles feed is a asynchronous event, so need to wait async event to finish running
        getEventInspectorsUtil().waitForCalmPeriod();

        SyndFeed feed = FeedUtil.getFeed( "brokenFiles" );

        @SuppressWarnings( "unchecked" )
        List<SyndEntry> entries = feed.getEntries();

        Assert.assertTrue( entries.size() >= 1, "Expected more then 1 entries, but got " + entries.size() + " - "
            + entries );

        validateContent( entries );

    }

    private void validateContent( List<SyndEntry> entries )
    {
        StringBuilder titles = new StringBuilder();

        String contentName = gav.getArtifactId() + "-" + gav.getVersion() + "." + gav.getExtension();

        for ( SyndEntry entry : entries )
        {
            // check if the title contains the file name (pom or jar)
            String title = entry.getDescription().getValue();
            titles.append( title );
            titles.append( ',' );

            assertThat( title, containsString( contentName ) );
            return;
        }

        Assert.fail( titles.toString() );
    }
}
