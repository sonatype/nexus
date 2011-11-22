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
package org.sonatype.nexus.integrationtests.nexus4579;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.SnapshotRemovalTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.EventInspectorsUtil;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.MavenDeployer;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * IT testing move to trash/delete immediately behavior of the SnapshotRemovalTask.
 *
 * @see org.sonatype.nexus.proxy.wastebasket.DeleteOperation
 */
public class Nexus4579OptionalTrashForSnapshotsIT
    extends AbstractNexusIntegrationTest
{

    private File artifactFolder;

    private File repositoryPath;

    private File trashArtifactFolder;

    private File groupFolder;

    private File trashGroupIdFolder;

    private File trashPath;

    public Nexus4579OptionalTrashForSnapshotsIT()
    {
        super( "nexus-test-harness-snapshot-repo" );
    }

    @BeforeMethod( alwaysRun = true )
    public void deploySnapshotArtifacts()
        throws Exception
    {
        repositoryPath = new File( nexusWorkDir, "storage/nexus-test-harness-snapshot-repo" );

        trashPath = new File( repositoryPath, ".nexus/trash" );

        final String groupIdRelativePath = "nexus4579/";
        final String artifactRelativePath = groupIdRelativePath + "artifact/1.0-SNAPSHOT";

        groupFolder = new File( repositoryPath, groupIdRelativePath );
        artifactFolder = new File( repositoryPath, artifactRelativePath );
        trashGroupIdFolder = new File( trashPath, groupIdRelativePath );
        trashArtifactFolder = new File( trashPath, artifactRelativePath );

        File oldSnapshot = getTestFile( "repo" );

        // Copying to keep an old timestamp
        FileUtils.copyDirectory( oldSnapshot, repositoryPath );

        RepositoryMessageUtil.updateIndexes( getTestRepositoryId() );

        MavenDeployer.deployAndGetVerifier( GavUtil.newGav( "nexus4579", "artifact", "1.0-SNAPSHOT" ),
                                            getRepositoryUrl( getTestRepositoryId() ),
                                            getTestFile( "deploy/artifact/artifact.jar" ), null );
        MavenDeployer.deployAndGetVerifier( GavUtil.newGav( "nexus4579", "artifact", "1.0-SNAPSHOT", "pom" ),
                                            getRepositoryUrl( getTestRepositoryId() ),
                                            getTestFile( "deploy/artifact/pom.xml" ), null );

        TaskScheduleUtil.waitForAllTasksToStop();
        new EventInspectorsUtil( this ).waitForCalmPeriod();
    }

    @AfterMethod( alwaysRun = true )
    public void cleanTrash()
        throws IOException
    {
        // clean trash before tests to remove possible leftovers from previous tests
        FileUtils.deleteDirectory( trashPath );
    }

    @Test
    public void removeAllSnapshotsToTrash()
        throws Exception
    {
        runSnapshotRemover( "move-to-trash", false );

        assertThat( "artifact folder was not removed", artifactFolder.list(), nullValue() );
        assertThat( "removed snapshots did not go into trash", trashArtifactFolder.isDirectory(), is( true ) );
        Collection<?> jars = FileUtils.listFiles( trashArtifactFolder, new String[]{ "jar" }, false );
        assertThat( "removed snapshots did not go into trash: " + jars, jars, hasSize( 2 ) );
    }

    @Test
    public void removeAllSnapshotsDirectly()
        throws Exception
    {
        Matcher<Collection<Object>> empty = empty();

        // need local variables because generics won't compile otherwise
        Collection<Object> files = FileUtils.listFiles( groupFolder, null, true );
        assertThat( files, not( empty ) );

        runSnapshotRemover( "remove-directly", true );

        // need local variables because generics won't compile otherwise
        files = FileUtils.listFiles( groupFolder, null, true );
        assertThat( files, empty );

        assertThat( "removed snapshots and metadata did go into trash: " + Arrays.toString( trashGroupIdFolder.list() ),
                    trashGroupIdFolder.list(), nullValue() );
    }

    protected void runSnapshotRemover( String name,
                                       boolean deleteImmediately )
        throws Exception
    {
        ScheduledServicePropertyResource repositoryProp = new ScheduledServicePropertyResource();
        repositoryProp.setKey( SnapshotRemovalTaskDescriptor.REPO_OR_GROUP_FIELD_ID );
        repositoryProp.setValue( getTestRepositoryId() );

        ScheduledServicePropertyResource keepSnapshotsProp = new ScheduledServicePropertyResource();
        keepSnapshotsProp.setKey( SnapshotRemovalTaskDescriptor.MIN_TO_KEEP_FIELD_ID );
        keepSnapshotsProp.setValue( "-1" );

        ScheduledServicePropertyResource ageProp = new ScheduledServicePropertyResource();
        ageProp.setKey( SnapshotRemovalTaskDescriptor.KEEP_DAYS_FIELD_ID );
        ageProp.setValue( "-1" );

        ScheduledServicePropertyResource removeReleasedProp = new ScheduledServicePropertyResource();
        removeReleasedProp.setKey( SnapshotRemovalTaskDescriptor.REMOVE_WHEN_RELEASED_FIELD_ID );
        removeReleasedProp.setValue( Boolean.toString( true ) );

        ScheduledServicePropertyResource deleteImmediatelyProp = new ScheduledServicePropertyResource();
        deleteImmediatelyProp.setKey( SnapshotRemovalTaskDescriptor.DELETE_IMMEDIATELY );
        deleteImmediatelyProp.setValue( Boolean.toString( deleteImmediately ) );

        TaskScheduleUtil.runTask( name, SnapshotRemovalTaskDescriptor.ID, repositoryProp,
                                  keepSnapshotsProp, ageProp,
                                  removeReleasedProp, deleteImmediatelyProp );

    }
}
