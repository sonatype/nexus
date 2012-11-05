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
package org.sonatype.nexus.integrationtests.nexus383;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;

import org.apache.maven.index.SearchType;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.SearchNGResponse;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.io.Closeables;

/**
 * Test Search operations.
 */
public class Nexus383SearchNGIT
    extends AbstractNexusIntegrationTest
{

    private static final String NEXUS_TEST_HARNESS_RELEASE_REPO = "nexus-test-harness-release-repo";

    private static final String NEXUS_TEST_HARNESS_REPO2 = "nexus-test-harness-repo2";

    private static final String NEXUS_TEST_HARNESS_REPO = "nexus-test-harness-repo";

    protected GroupMessageUtil groupMessageUtil;

    public Nexus383SearchNGIT()
    {

    }

    @BeforeClass
    public void prepare()
    {
        this.groupMessageUtil = new GroupMessageUtil( this, this.getXMLXStream(), MediaType.APPLICATION_XML );
    }

    @Override
    protected void deployArtifacts()
        throws Exception
    {
        RepositoryMessageUtil.updateIndexes( NEXUS_TEST_HARNESS_RELEASE_REPO, NEXUS_TEST_HARNESS_REPO2,
                                             NEXUS_TEST_HARNESS_REPO );

        TaskScheduleUtil.waitForAllTasksToStop();

        super.deployArtifacts();
    }

    @AfterMethod
    public void resetRepo()
        throws Exception
    {
        getSearchMessageUtil().allowSearch( NEXUS_TEST_HARNESS_REPO, true );
        getSearchMessageUtil().allowBrowsing( NEXUS_TEST_HARNESS_REPO, true );
        getSearchMessageUtil().allowDeploying( NEXUS_TEST_HARNESS_REPO, true );

        // config changes may result in tasks spawned like reindex(!)
        TaskScheduleUtil.waitForAllTasksToStop();
        
        getEventInspectorsUtil().waitForCalmPeriod();
    }

    // 1. deploy an known artifact. and search for it. using group Id and artifact Id.
    @Test
    public void searchFor()
        throws Exception
    {
        TaskScheduleUtil.waitForAllTasksToStop();

        // groupId
        SearchNGResponse results = getSearchMessageUtil().searchNGFor( "nexus383" );
        Assert.assertEquals( results.getData().size(), 2 );

        // 3. negative test
        results = getSearchMessageUtil().searchNGFor( "nexus-383" );
        Assert.assertTrue( results.getData().isEmpty() );

        // artifactId
        results = getSearchMessageUtil().searchNGFor( "know-artifact-1", null, SearchType.EXACT );
        Assert.assertEquals( 1, results.getData().size() );

        // artifactId
        results = getSearchMessageUtil().searchNGFor( "know-artifact-2", null, SearchType.EXACT );
        Assert.assertEquals( 1, results.getData().size() );

        // partial artifactId
        results = getSearchMessageUtil().searchNGFor( "know-artifact" );
        Assert.assertEquals( 2, results.getData().size() );

        // 3. negative test
        results = getSearchMessageUtil().searchNGFor( "unknow-artifacts" );
        Assert.assertTrue( results.getData().isEmpty() );

        // NEXUS-2724: the member changes should propagate to it's groups too
        // has it propagated to group?
        results =
            getSearchMessageUtil().searchNGForGav( "nexus383", "know-artifact-1", "1.0.0", null, null, "public", null );
        Assert.assertEquals( 1, results.getData().size() );
        results =
            getSearchMessageUtil().searchNGForGav( "nexus383", "know-artifact-2", "1.0.0", null, null, "public", null );
        Assert.assertEquals( 1, results.getData().size() );
    }

    // 2. search using SHA1
    @Test
    public void searchForSHA1()
        throws Exception
    {
        // know-artifact-1
        SearchNGResponse result = getSearchMessageUtil().searchSha1NGFor( "4ce1d96bd11b8959b32a75c1fa5b738d7b87d408" );
        Assert.assertEquals( result.getData().size(), 1, "know-artifact-1 should be found" );

        // know-artifact-2
        result = getSearchMessageUtil().searchSha1NGFor( "230377663ac3b19ad83c99b0afdb056dd580c5c8" );
        Assert.assertEquals( result.getData().size(), 1, "know-artifact-2 should be found" );

        // velo's picture
        result = getSearchMessageUtil().searchSha1NGFor( "612c17de73fdc8b9e3f6a063154d89946eb7c6f2" );
        Assert.assertEquals( result.getData().size(), 0, "velo's picture should not be found" );
    }

    @Test
    // 5. disable searching on a repo and do a search
    public void disableSearching()
        throws Exception
    {
        // Disabling default repo
        getSearchMessageUtil().allowSearch( NEXUS_TEST_HARNESS_REPO, false );

        // config change spawns a lot of events
        getEventInspectorsUtil().waitForCalmPeriod();

        // groupId
        SearchNGResponse results = getSearchMessageUtil().searchNGFor( "nexus383" );
        Assert.assertTrue( results.getData().isEmpty() );

        // artifactId
        results = getSearchMessageUtil().searchNGFor( "know-artifact-1" );
        Assert.assertTrue( results.getData().isEmpty() );

        // artifactId
        results = getSearchMessageUtil().searchNGFor( "know-artifact-2" );
        Assert.assertTrue( results.getData().isEmpty() );

        // partial artifactId
        results = getSearchMessageUtil().searchNGFor( "know-artifact" );
        Assert.assertTrue( results.getData().isEmpty() );

    }

    @Test
    // 6. disable/enable searching on a repo and do a search
    public void disableEnableSearching()
        throws Exception
    {
        // Run disable mode first
        disableSearching();

        // Enabling default repo again
        getSearchMessageUtil().allowSearch( NEXUS_TEST_HARNESS_REPO, true );
        // config change spawns a lot of events
        getEventInspectorsUtil().waitForCalmPeriod();

        // All searchs should run ok
        searchFor();
    }

    @Test
    // 7. make a repo not browseable and do a search
    public void disableBrowsing()
        throws Exception
    {
        // Enabling default repo again
        getSearchMessageUtil().allowBrowsing( NEXUS_TEST_HARNESS_REPO, false );

        // All searchs should run ok
        searchFor();
    }

    @Test
    // 8. make a repo not browseable / browseable and do a search
    public void disableEnableBrowsing()
        throws Exception
    {
        // Run disable mode first
        disableBrowsing();

        // Enabling default repo again
        getSearchMessageUtil().allowBrowsing( NEXUS_TEST_HARNESS_REPO, true );

        // All searchs should run ok
        searchFor();

    }

    @Test
    public void disableDeploying()
        throws Exception
    {
        // Enabling default repo again
        getSearchMessageUtil().allowDeploying( NEXUS_TEST_HARNESS_REPO, false );

        // All searchs should run ok
        searchFor();

        getSearchMessageUtil().allowDeploying( NEXUS_TEST_HARNESS_REPO, true );
    }

    @Test( dependsOnMethods = { "searchFor", "searchForSHA1", "disableSearching", "disableEnableSearching",
        "disableBrowsing", "disableEnableBrowsing", "disableDeploying" } )
    // 4. deploy same artifact to multiple repos, and search
    public void crossRepositorySearch()
        throws Exception
    {
        // file to deploy
        File fileToDeploy = this.getTestFile( "crossArtifact.jar" );
        File pomFile = this.getTestFile( "crossArtifact.pom" );

        MavenXpp3Reader reader = new MavenXpp3Reader();
        FileInputStream fis = new FileInputStream( pomFile );
        Model model = null;
        try
        {
            model = reader.read( fis, true );
        }
        finally
        {
            Closeables.closeQuietly( fis );
        }

        String deployUrl = model.getDistributionManagement().getRepository().getUrl();

        Gav gav =
            new Gav( model.getGroupId(), model.getArtifactId(), model.getVersion(), null, model.getPackaging(), 0,
                     new Date().getTime(), model.getName(), false, null, false, null );

        // Multi repository deploy
        getDeployUtils().deployWithWagon( "http", deployUrl, fileToDeploy, this.getRelitiveArtifactPath( gav ) );
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_REPO2 ),
                                          fileToDeploy, this.getRelitiveArtifactPath( gav ) );
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_RELEASE_REPO ),
                                          fileToDeploy, this.getRelitiveArtifactPath( gav ) );
        getDeployUtils().deployWithWagon( "http", deployUrl, pomFile, this.getRelitivePomPath( gav ) );
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_REPO2 ),
                                          pomFile, this.getRelitivePomPath( gav ) );
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_RELEASE_REPO ),
                                          pomFile, this.getRelitivePomPath( gav ) );

        // if you deploy the same item multiple times to the same repo, that is only a single item
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_RELEASE_REPO ),
                                          fileToDeploy, this.getRelitiveArtifactPath( gav ) );
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_RELEASE_REPO ),
                                          pomFile, this.getRelitivePomPath( gav ) );
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_RELEASE_REPO ),
                                          fileToDeploy, this.getRelitiveArtifactPath( gav ) );
        getDeployUtils().deployWithWagon( "http",
                                          deployUrl.replace( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_RELEASE_REPO ),
                                          pomFile, this.getRelitivePomPath( gav ) );

        RepositoryMessageUtil.updateIndexes( NEXUS_TEST_HARNESS_REPO, NEXUS_TEST_HARNESS_REPO2,
                                             NEXUS_TEST_HARNESS_RELEASE_REPO );

        TaskScheduleUtil.waitForAllTasksToStop();
        
        getEventInspectorsUtil().waitForCalmPeriod();

        // Keyword search does collapse results, so we need _1_
        // Since the top level is GAV now only
        SearchNGResponse results = getSearchMessageUtil().searchNGFor( "crossArtifact" );
        Assert.assertEquals( results.getData().size(), 1, "We need 1 cross artifacts with Quick search!" );

        // GAV search does not
        results = getSearchMessageUtil().searchNGForGav( gav );
        Assert.assertEquals( results.getData().size(), 1, "We need 1 cross artifacts with GAV search!" );
    }

}
