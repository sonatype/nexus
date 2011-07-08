package org.sonatype.nexus.rest.repositories;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.maven1.M1Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for RestContentRepositoryURLBuilder.
 */
public class RestContentRepositoryURLBuilderTest
{
    private static final String MOCK_REPO_ID = "test-id";

    private static final String MOCK_GROUP_ID = "test-group-id";

    private static final String NOT_FOUND_REPO_ID = "not-found-mock-id";

    private static final String GLOBAL_BASE_URL = "http://global/baseurl";

    private static final String MOCK_PATH_PREFIX = "mockprefix";

    private Repository repository;

    private Repository group;

    private RepositoryRegistry repositoryRegistry;

    private RepositoryTypeRegistry repositoryTypeRegistry;

    private GlobalRestApiSettings globalRestApiSettings;

    @Before
    public void setUp()
        throws Exception
    {
        repository = Mockito.mock( Repository.class );
        Mockito.doReturn( MOCK_REPO_ID ).when( repository ).getId();
        Mockito.doReturn( M2Repository.class.getName() ).when( repository ).getProviderRole();
        Mockito.doReturn( "NOT-USED" ).when( repository ).getPathPrefix();

        group = Mockito.mock( GroupRepository.class );
        Mockito.doReturn( MOCK_GROUP_ID ).when( group ).getId();
        Mockito.doReturn( M2GroupRepository.class.getName() ).when( group ).getProviderRole();
        Mockito.doReturn( "NOT-USED" ).when( group ).getPathPrefix();

        repositoryRegistry = Mockito.mock( RepositoryRegistry.class );
        Mockito.doReturn( repository ).when( repositoryRegistry ).getRepository( MOCK_REPO_ID );
        Mockito.doReturn( group ).when( repositoryRegistry ).getRepository( MOCK_GROUP_ID );
        Mockito.doThrow( new NoSuchRepositoryException( NOT_FOUND_REPO_ID ) ).when( repositoryRegistry ).getRepository(
            NOT_FOUND_REPO_ID );

        globalRestApiSettings = Mockito.mock( GlobalRestApiSettings.class );

        Set<RepositoryTypeDescriptor> typeDescriptors = new HashSet<RepositoryTypeDescriptor>();
        typeDescriptors.add( new RepositoryTypeDescriptor( M2Repository.class, "my-hint", MOCK_PATH_PREFIX ) );
        typeDescriptors.add( new RepositoryTypeDescriptor( M1Repository.class, "invalid", "invalid" ) );

        repositoryTypeRegistry = Mockito.mock( RepositoryTypeRegistry.class );
        Mockito.doReturn( typeDescriptors ).when( repositoryTypeRegistry ).getRegisteredRepositoryTypeDescriptors();
    }

    @Test
    public void testForceBaseUrlById()
        throws Exception
    {

        Mockito.doReturn( true ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( true ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( GLOBAL_BASE_URL ).when( globalRestApiSettings ).getBaseUrl();

        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings );

        Assert.assertEquals( GLOBAL_BASE_URL + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
                             urlFinder.getRepositoryUrl( MOCK_REPO_ID ) );
    }

    @Test
    public void testForceBaseUrlByIdGlobalDisabled()
        throws Exception
    {

        Mockito.doReturn( false ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( true ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( GLOBAL_BASE_URL ).when( globalRestApiSettings ).getBaseUrl();

        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings );

        Assert.assertEquals( GLOBAL_BASE_URL + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
                             urlFinder.getRepositoryUrl( MOCK_REPO_ID ) );
    }

    @Test
    public void testNoRequestBaseURLNotSet()
        throws Exception
    {
        Mockito.doReturn( true ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( false ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( null ).when( globalRestApiSettings ).getBaseUrl();

        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings );

        Assert.assertEquals( "http://base-url-not-set" + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
                             urlFinder.getRepositoryUrl( MOCK_REPO_ID ) );
    }

    @Test
    public void testBaseUrlGlobalNoRequest()
        throws Exception
    {

        Mockito.doReturn( true ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( false ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( GLOBAL_BASE_URL ).when( globalRestApiSettings ).getBaseUrl();

        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings );

        Assert.assertEquals( GLOBAL_BASE_URL + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
                             urlFinder.getRepositoryUrl( MOCK_REPO_ID ) );
    }

    @Test
    public void testBaseUrlGlobalRequest()
        throws Exception
    {
        String restletBaseURL = "http://from/request/";

        Mockito.doReturn( true ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( false ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( GLOBAL_BASE_URL ).when( globalRestApiSettings ).getBaseUrl();

        try
        {
            Request request = new Request();
            request.setRootRef( new Reference( restletBaseURL ) );
            Response response = new Response( request );
            Response.setCurrent( response );

            RestContentRepositoryURLBuilder urlFinder =
                new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry,
                                                     globalRestApiSettings );

            Assert.assertEquals( restletBaseURL + "content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
                                 urlFinder.getRepositoryUrl( MOCK_REPO_ID ) );
        }
        finally
        {
            Response.setCurrent( null );
        }
    }

    @Test( expected = NoSuchRepositoryException.class )
    public void testNotFound()
        throws NoSuchRepositoryException
    {
        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings );

        urlFinder.getRepositoryUrl( NOT_FOUND_REPO_ID );
    }

    @Test
    public void testUnknownRepositoryType()
        throws NoSuchRepositoryException
    {

        Mockito.doReturn( true ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( false ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( GLOBAL_BASE_URL ).when( globalRestApiSettings ).getBaseUrl();

        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings );

        Assert.assertEquals( GLOBAL_BASE_URL + "/content/" + "repositories" + "/" + MOCK_GROUP_ID,
                             urlFinder.getRepositoryUrl( MOCK_GROUP_ID ) );

    }

    @Test
    public void testUsingSystemProperties()
        throws Exception
    {
        Mockito.doReturn( true ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( false ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( null ).when( globalRestApiSettings ).getBaseUrl();

        String hostname = InetAddress.getLocalHost().getHostName();
        Integer port = 1234;
        String contextPath = "/context-path";

        try
        {
            System.setProperty( "plexus.application-port", port.toString() );
            System.setProperty( "plexus.webapp-context-path", contextPath );

            RestContentRepositoryURLBuilder urlFinder =
                new RestContentRepositoryURLBuilder( repositoryRegistry, repositoryTypeRegistry,
                                                     globalRestApiSettings );

            Assert.assertEquals(
                "http://" + hostname + ":" + port + contextPath + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
                urlFinder.getRepositoryUrl( MOCK_REPO_ID ) );
        }
        finally
        {
            System.clearProperty( "plexus.application-port" );
            System.clearProperty( "plexus.webapp-context-path" );
        }
    }


}
