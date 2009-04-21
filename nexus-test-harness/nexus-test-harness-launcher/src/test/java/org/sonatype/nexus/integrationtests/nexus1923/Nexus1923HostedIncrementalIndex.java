package org.sonatype.nexus.integrationtests.nexus1923;

import java.io.File;

import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

public class Nexus1923HostedIncrementalIndex
    extends AbstractNexus1923
{    
    public Nexus1923HostedIncrementalIndex()
        throws Exception
    {
        super();
    }
    
    @Test
    public void validateIncrementalIndexesCreated()
        throws Exception
    {
        File repoStorageDirectory = getHostedRepositoryStorageDirectory();
        
        //First create our repository
        createHostedRepository();
        
        //Create the reindex task
        String reindexId = createHostedReindexTask();
        
        //Put an artifact in the storage
        FileUtils.copyDirectoryStructure( getTestFile( FIRST_ARTIFACT ), 
            repoStorageDirectory );
        
        //Now reindex the repo
        reindexHostedRepository( reindexId );
        
        //Now make sure there is an index file, and no incremental files
        Assert.assertTrue( getHostedRepositoryIndex().exists() );
        Assert.assertFalse( getHostedRepositoryIndexIncrement( "1" ).exists() );
        validateCurrentHostedIncrementalCounter( 0 );
        
        //Put an artifact in the storage
        FileUtils.copyDirectoryStructure( getTestFile( SECOND_ARTIFACT ), 
            repoStorageDirectory );
        
        //Now reindex the repo
        reindexHostedRepository( reindexId );
        
        //Now make sure there is an index file, and 1 incremental file
        Assert.assertTrue( getHostedRepositoryIndex().exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "1" ).exists() );
        Assert.assertFalse( getHostedRepositoryIndexIncrement( "2" ).exists() );
        validateCurrentHostedIncrementalCounter( 1 );
        
        //Put an artifact in the storage
        FileUtils.copyDirectoryStructure( getTestFile( THIRD_ARTIFACT ), 
            repoStorageDirectory );
        
        //Now reindex the repo
        reindexHostedRepository( reindexId );
        
        //Now make sure there is an index file, and 2 incremental file
        Assert.assertTrue( getHostedRepositoryIndex().exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "1" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "2" ).exists() );
        Assert.assertFalse( getHostedRepositoryIndexIncrement( "3" ).exists() );
        validateCurrentHostedIncrementalCounter( 2 );
        
        //Put an artifact in the storage
        FileUtils.copyDirectoryStructure( getTestFile( FOURTH_ARTIFACT ), 
            repoStorageDirectory );
        
        //Now reindex the repo
        reindexHostedRepository( reindexId );
        
        //Now make sure there is an index file, and 3 incremental file
        Assert.assertTrue( getHostedRepositoryIndex().exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "1" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "2" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "3" ).exists() );
        Assert.assertFalse( getHostedRepositoryIndexIncrement( "4" ).exists() );
        validateCurrentHostedIncrementalCounter( 3 );
        
        //Put an artifact in the storage
        FileUtils.copyDirectoryStructure( getTestFile( FIFTH_ARTIFACT ), 
            repoStorageDirectory );
        
        //Now reindex the repo
        reindexHostedRepository( reindexId );
        
        //Now make sure there is an index file, and 4 incremental file
        Assert.assertTrue( getHostedRepositoryIndex().exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "1" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "2" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "3" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "4" ).exists() );
        Assert.assertFalse( getHostedRepositoryIndexIncrement( "5" ).exists() );
        validateCurrentHostedIncrementalCounter( 4 );
        
        //Now reindex the repo again, and make sure nothing new is created
        reindexHostedRepository( reindexId );
        
        //Now make sure there is an index file, and 4 incremental file
        Assert.assertTrue( getHostedRepositoryIndex().exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "1" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "2" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "3" ).exists() );
        Assert.assertTrue( getHostedRepositoryIndexIncrement( "4" ).exists() );
        Assert.assertFalse( getHostedRepositoryIndexIncrement( "5" ).exists() );
        validateCurrentHostedIncrementalCounter( 4 );
    }
}
