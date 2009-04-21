package org.sonatype.nexus.integrationtests.nexus1923;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ReindexTaskDescriptor;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

public abstract class AbstractNexus1923
    extends AbstractNexusIntegrationTest
{
    protected RepositoryMessageUtil repoUtils;
        
    protected static final String HOSTED_REPO_ID = "incremental_repo";
    protected static final String PROXY_REPO_ID = "incremental_repo_proxy";
    protected static final String FIRST_ARTIFACT = "firstArtifact";
    protected static final String SECOND_ARTIFACT = "secondArtifact";
    protected static final String THIRD_ARTIFACT = "thirdArtifact";
    protected static final String FOURTH_ARTIFACT = "fourthArtifact";
    protected static final String FIFTH_ARTIFACT = "fifthArtifact";
    protected static final String HOSTED_REINDEX_TASK_NAME = "incremental_reindex";
    protected static final String PROXY_REINDEX_TASK_NAME = "incremental_reindex_proxy";
    
    public AbstractNexus1923()
        throws Exception
    {
        super();
        
        this.repoUtils =
            new RepositoryMessageUtil( 
                this.getJsonXStream(), 
                MediaType.APPLICATION_JSON,
                getRepositoryTypeRegistry() );
        
        FileUtils.deleteDirectory( getHostedRepositoryStorageDirectory() );
        FileUtils.deleteDirectory( getHostedRepositoryLocalIndexDirectory() );
        FileUtils.deleteDirectory( getHostedRepositoryRemoteIndexDirectory() );
        FileUtils.deleteDirectory( getProxyRepositoryStorageDirectory() );
        FileUtils.deleteDirectory( getProxyRepositoryLocalIndexDirectory() );
        FileUtils.deleteDirectory( getProxyRepositoryRemoteIndexDirectory() );
    }
    
    private RepositoryResource createRepository()
    {
        RepositoryResource resource = new RepositoryResource();
        
        resource.setProvider( "maven2" );
        resource.setFormat( "maven2" );
        resource.setRepoPolicy( "release" );
        resource.setChecksumPolicy( "ignore" );
        resource.setBrowseable( true );
        resource.setIndexable( true );
        
        return resource;
    }
    
    protected void createHostedRepository()
        throws Exception
    {
        RepositoryResource resource = createRepository();
        resource.setId( HOSTED_REPO_ID );
        resource.setName( HOSTED_REPO_ID );
        resource.setRepoType( "hosted" );
        resource.setAllowWrite( true );
        repoUtils.createRepository( resource );   
    }
    
    protected void createProxyRepository()
        throws Exception
    {
        RepositoryResource resource = createRepository();
        resource.setId( PROXY_REPO_ID );
        resource.setName( PROXY_REPO_ID );
        resource.setRepoType( "proxy" );
        resource.setAllowWrite( false );
        resource.setDownloadRemoteIndexes( true );
        RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();        
        remoteStorage.setRemoteStorageUrl( getBaseNexusUrl() + "content/repositories/" + HOSTED_REPO_ID );
        resource.setRemoteStorage( remoteStorage );
        repoUtils.createRepository( resource );
    }
    
    private String createReindexTask( String repositoryId, String taskName )
        throws Exception
    {
        ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
        prop.setId( "repositoryOrGroupId" );
        prop.setValue( "repo_" + repositoryId );
    
        ScheduledServiceBaseResource scheduledTask = new ScheduledServiceBaseResource();
        scheduledTask.setEnabled( true );
        scheduledTask.setId( null );
        scheduledTask.setName( taskName );
        scheduledTask.setTypeId( ReindexTaskDescriptor.ID );
        scheduledTask.setSchedule( "manual" );
        scheduledTask.addProperty( prop );
        Status status = TaskScheduleUtil.create( scheduledTask );
        
        Assert.assertTrue( status.isSuccess() );
        
        return TaskScheduleUtil.getTask( taskName ).getId();
    }
    
    protected String createHostedReindexTask()
        throws Exception
    {
        return createReindexTask( HOSTED_REPO_ID, HOSTED_REINDEX_TASK_NAME );
    }
    
    protected String createProxyReindexTask()
        throws Exception
    {
        return createReindexTask( PROXY_REPO_ID, PROXY_REINDEX_TASK_NAME );
    }
    
    private void reindexRepository( String taskId, String taskName )
        throws Exception
    {
        TaskScheduleUtil.run( taskId );
        
        TaskScheduleUtil.waitForTask( taskName, 300 );
    }
    
    protected void reindexHostedRepository( String taskId )
        throws Exception
    {
        reindexRepository( taskId, HOSTED_REINDEX_TASK_NAME );
    }
    
    protected void reindexProxyRepository( String taskId )
        throws Exception
    {
        reindexRepository( taskId, PROXY_REINDEX_TASK_NAME );
    }
    
    private File getRepositoryLocalIndexDirectory( String repositoryId )
    {
        return new File( AbstractNexusIntegrationTest.nexusWorkDir + "/indexer/" + repositoryId + "-local/" );
    }
    
    protected File getHostedRepositoryLocalIndexDirectory()
    {
        return getRepositoryLocalIndexDirectory( HOSTED_REPO_ID );
    }
    
    protected File getProxyRepositoryLocalIndexDirectory()
    {
        return getRepositoryLocalIndexDirectory( PROXY_REPO_ID );
    }
    
    private File getRepositoryRemoteIndexDirectory( String repositoryId )
    {
        return new File( AbstractNexusIntegrationTest.nexusWorkDir + "/indexer/" + repositoryId + "-remote/" );
    }
    
    protected File getHostedRepositoryRemoteIndexDirectory()
    {
        return getRepositoryRemoteIndexDirectory( HOSTED_REPO_ID );
    }
    
    protected File getProxyRepositoryRemoteIndexDirectory()
    {
        return getRepositoryRemoteIndexDirectory( PROXY_REPO_ID );
    }
    
    private File getRepositoryStorageDirectory( String repositoryId )
    {
        return new File( AbstractNexusIntegrationTest.nexusWorkDir + "/storage/" + repositoryId + "/" );
    }
    
    protected File getHostedRepositoryStorageDirectory()
    {
        return getRepositoryStorageDirectory( HOSTED_REPO_ID );
    }
    
    protected File getProxyRepositoryStorageDirectory()
    {
        return getRepositoryStorageDirectory( PROXY_REPO_ID );
    }
    
    private File getRepositoryIndex( File directory )
    {
        return new File( directory, IndexingContext.INDEX_FILE + ".gz");
    }
    
    protected File getHostedRepositoryIndex()
    {
        return getRepositoryIndex( getHostedRepositoryStorageIndexDirectory() );
    }
    
    protected File getProxyRepositoryIndex()
    {
        return getRepositoryIndex( getProxyRepositoryStorageIndexDirectory() );
    }
    
    private Properties getRepositoryIndexProperties( File baseDir )
        throws Exception
    {
        Properties props = new Properties();
        
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream( new File( baseDir , IndexingContext.INDEX_FILE + ".properties" ) );
            props.load( fis );
        }
        finally
        {
            if ( fis != null )
            {
                fis.close();
            }
        }
        
        return props;
    }
    
    protected Properties getHostedRepositoryIndexProperties()
        throws Exception
    {
        return getRepositoryIndexProperties( getHostedRepositoryStorageIndexDirectory() );
    }
    
    protected Properties getProxyRepositoryIndexProperties()
        throws Exception
    {
        return getRepositoryIndexProperties( getProxyRepositoryStorageIndexDirectory() );
    }
    
    private File getRepositoryIndexIncrement( File directory, String id )
    {
        return new File( directory, IndexingContext.INDEX_FILE + "." + id + ".gz" );
    }
    
    protected File getHostedRepositoryIndexIncrement( String id )
    {
        return getRepositoryIndexIncrement( getHostedRepositoryStorageIndexDirectory(), id ); 
    }
    
    protected File getProxyRepositoryIndexIncrement( String id )
    {
        return getRepositoryIndexIncrement( getProxyRepositoryStorageIndexDirectory(), id ); 
    }
    
    private File getRepositoryStorageIndexDirectory( String repositoryId )
    {
        return new File( AbstractNexusIntegrationTest.nexusWorkDir + "/storage/" + repositoryId + "/.index/" );
    }
    
    protected File getHostedRepositoryStorageIndexDirectory()
    {
        return getRepositoryStorageIndexDirectory( HOSTED_REPO_ID );
    }
    
    protected File getProxyRepositoryStorageIndexDirectory()
    {
        return getRepositoryStorageIndexDirectory( PROXY_REPO_ID );
    }
    
    private void validateCurrentIncrementalCounter( Properties properties, int current )
        throws Exception
    {
        Assert.assertEquals( properties.getProperty( IndexingContext.INDEX_CHUNK_COUNTER ), Integer.toString( current ) );
    }
    
    protected void validateCurrentHostedIncrementalCounter( int current )
        throws Exception
    {
        validateCurrentIncrementalCounter( getHostedRepositoryIndexProperties(), current );
    }
    
    protected void validateCurrentProxyIncrementalCounter( int current )
        throws Exception
    {
        validateCurrentIncrementalCounter( getProxyRepositoryIndexProperties(), current );
    }
}
