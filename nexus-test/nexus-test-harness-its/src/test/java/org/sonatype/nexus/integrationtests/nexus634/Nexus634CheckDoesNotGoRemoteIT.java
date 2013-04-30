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
package org.sonatype.nexus.integrationtests.nexus634;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.sonatype.jettytestsuite.BlockingServer;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;

/**
 * Tests SnapshotRemoverTask to not go remote when checking for release existence.
 * 
 * @author cstamas
 */
public class Nexus634CheckDoesNotGoRemoteIT
    extends AbstractSnapshotRemoverIT
{
    protected String localStorageDir = null;

    protected Integer proxyPort;

    protected Server server = null;

    protected TouchTrackingHandler touchTrackingHandler;

    protected RepositoryMessageUtil repositoryMessageUtil;

    public Nexus634CheckDoesNotGoRemoteIT()
        throws Exception
    {
        super();

        this.localStorageDir = TestProperties.getString( "proxy.repo.base.dir" );
        this.proxyPort = TestProperties.getInteger( "proxy.server.port" );
        this.repositoryMessageUtil = new RepositoryMessageUtil( this, getXMLXStream(), MediaType.APPLICATION_XML );
    }

    @Before
    public void deploySnapshotArtifacts()
        throws Exception
    {
        super.deploySnapshotArtifacts();

        File remoteSnapshot = getTestFile( "remote-repo" );

        // Copying to keep an old timestamp
        FileUtils.copyDirectory( remoteSnapshot, repositoryPath );

        // update indexes?
        // RepositoryMessageUtil.updateIndexes( "nexus-test-harness-snapshot-repo" );
    }

    @Before
    public void startProxy()
        throws Exception
    {
        touchTrackingHandler = new TouchTrackingHandler();
        server = new BlockingServer( proxyPort );
        server.setHandler( touchTrackingHandler );
        server.start();
    }

    @After
    public void stopProxy()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
            server = null;
            touchTrackingHandler = null;
        }
    }

    @Test
    public void keepNewSnapshots()
        throws Exception
    {
        // set proxy reposes to point here
        RepositoryProxyResource proxy =
            (RepositoryProxyResource) repositoryMessageUtil.getRepository( REPO_RELEASE_PROXY_REPO1 );
        proxy.getRemoteStorage().setRemoteStorageUrl( "http://localhost:" + proxyPort + "/" );
        repositoryMessageUtil.updateRepo( proxy );

        // expire caches
        ScheduledServicePropertyResource repoOrGroupProp = new ScheduledServicePropertyResource();
        repoOrGroupProp.setKey( "repositoryId" );
        repoOrGroupProp.setValue( REPO_RELEASE_PROXY_REPO1 );
        TaskScheduleUtil.runTask( ExpireCacheTaskDescriptor.ID, repoOrGroupProp );

        // run snapshot remover
        runSnapshotRemover( "nexus-test-harness-snapshot-repo", 0, 0, true );

        // check is proxy touched
        Assert.assertEquals( "Proxy should not be touched! It was asked for " + touchTrackingHandler.getTouchedTargets(), touchTrackingHandler.getTouchedTargets().size(),
            0 );
    }
}
