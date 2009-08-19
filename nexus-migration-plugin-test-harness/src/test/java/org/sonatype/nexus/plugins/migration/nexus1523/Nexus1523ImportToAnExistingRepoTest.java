package org.sonatype.nexus.plugins.migration.nexus1523;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonatype.nexus.plugin.migration.artifactory.dto.MigrationSummaryDTO;
import org.sonatype.nexus.plugins.migration.AbstractMigrationIntegrationTest;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

public class Nexus1523ImportToAnExistingRepoTest
    extends AbstractMigrationIntegrationTest
{

    @BeforeClass
    public static void deleteLog()
        throws IOException
    {
        File logFile = new File( "./target/logs/migration.log" );
        if ( logFile.exists() )
        {
            FileUtils.forceDelete( logFile );
        }
    }

    @Test
    public void importToAnExistingRepo()
        throws Exception
    {
        MigrationSummaryDTO migrationSummary = prepareMigration( getTestFile( "artifactoryBackup.zip" ) );
        commitMigration( migrationSummary );
        TaskScheduleUtil.waitForTasks( 40 );
        Thread.sleep( 2000 );

        File logFile = new File( "./target/logs/migration.log" );
        Assert.assertTrue( "Migration log file not found", logFile.isFile() );

        String log = IOUtil.toString( new FileReader( logFile ) );
        Assert.assertFalse( "Error during migration", log.toLowerCase().contains( "error" ) );

        File importedArtifact =
            new File( nexusWorkDir, "/storage/main-local/nexus1523/import-artifact/1.0/import-artifact-1.0.jar" );
        Assert.assertTrue( "Imported artifact do not exists!", importedArtifact.isFile() );
    }
}
