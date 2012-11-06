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
package org.sonatype.nexus.integrationtests.proxy.nexus635;

import static org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import static org.sonatype.nexus.test.utils.FileTestingUtils.compareFileSHA1s;

import java.io.File;

import org.apache.maven.index.artifact.Gav;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.MavenDeployer;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the expire cache task.
 */
public class Nexus635ExpireCacheTaskIT
    extends AbstractNexusProxyIntegrationTest
{

    private Gav GAV =
        new Gav( "nexus635", "artifact", "1.0-SNAPSHOT", null, "jar", 0, 0L, null, false, null, false, null );

    public Nexus635ExpireCacheTaskIT()
        throws Exception
    {
        super( "tasks-snapshot-repo" );
    }

    public void addSnapshotArtifactToProxy( File fileToDeploy )
        throws Exception
    {
        String repositoryUrl = "file://" + localStorageDir + "/tasks-snapshot-repo";
        MavenDeployer.deployAndGetVerifier( GAV, repositoryUrl, fileToDeploy, null );
    }

    @Test(groups = PROXY)
    public void expireCacheTask()
        throws Exception
    {
        /*
         * fetch something from a remote repo, run clearCache from root, on _remote repo_ put a newer timestamped file,
         * and rerequest again the same (the filenames will be the same, only the content/timestamp should change),
         * nexus should refetch it. BUT, this works for snapshot nexus reposes only, release reposes do not refetch!
         */
        File artifact1 = getTestFile( "artifact-1.jar" );
        addSnapshotArtifactToProxy( artifact1 );

        File firstDownload = downloadSnapshotArtifact( "tasks-snapshot-repo", GAV, new File( "target/download" ) );
        Assert.assertTrue( compareFileSHA1s( firstDownload, artifact1 ), //
                           "First time, should download artifact 1" );

        File artifact2 = getTestFile( "artifact-2.jar" );
        addSnapshotArtifactToProxy( artifact2 );
        File secondDownload = downloadSnapshotArtifact( "tasks-snapshot-repo", GAV, new File( "target/download" ) );
        Assert.assertTrue( compareFileSHA1s( secondDownload, artifact1 ),//
                           "Before ExpireCache should download artifact 1" );

        ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
        prop.setKey( "repositoryId" );
        prop.setValue( "tasks-snapshot-repo" );

        // prop = new ScheduledServicePropertyResource();
        // prop.setId( "resourceStorePath" );
        // prop.setValue( "/" );

        // This is THE important part
        TaskScheduleUtil.runTask( ExpireCacheTaskDescriptor.ID, prop );
        
        File thirdDownload = downloadSnapshotArtifact( "tasks-snapshot-repo", GAV, new File( "target/download" ) );
        Assert.assertTrue( compareFileSHA1s( thirdDownload, artifact2 ), //
                           "After ExpireCache should download artifact 2" );
    }

}
