package org.sonatype.nexus.plugins.migration.nexus2501;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.plugin.migration.artifactory.dto.MigrationSummaryDTO;
import org.sonatype.nexus.plugins.migration.AbstractMigrationIntegrationTest;
import org.sonatype.nexus.rest.model.UserResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

public class Nexus2501UseRolesAssignTest
    extends AbstractMigrationIntegrationTest
{

    @Test
    public void checkUsersRoles()
        throws Exception
    {
        MigrationSummaryDTO migrationSummary = prepareMigration( getTestFile( "20090818.120005.zip" ) );
        commitMigration( migrationSummary );

        TaskScheduleUtil.waitForTasks( 40 );
        Thread.sleep( 2000 );

        Assert.assertNotNull( roleUtil.getRole( "exclusive-group" ) );
        Assert.assertNotNull( roleUtil.getRole( "inclusive-group" ) );

        UserResource foobar = userUtil.getUser( "foobar" );
        Assert.assertTrue( foobar.getRoles().contains( "exclusive-group" ) );

        UserResource barfoo = userUtil.getUser( "barfoo" );
        Assert.assertTrue( barfoo.getRoles().contains( "inclusive-group" ) );

    }
}
