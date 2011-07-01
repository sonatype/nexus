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
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Tests for RestContentRepositoryURLBuilder.
 */
public class RestContentRepositoryURLBuilderTest
{
    private static final String MOCK_REPO_ID = "test-id";

    private static final String NOT_FOUND_REPO_ID = "not-found-mock-id";

    private static final String GLOBAL_BASE_URL = "http://global/baseurl";

    private static final String MOCK_PATH_PREFIX = "mockprefix";

    private Repository repository;

    private RepositoryRegistry repositoryRegistry;

    private GlobalRestApiSettings globalRestApiSettings;

    @Before
    public void setUp()
        throws Exception
    {
        repository = Mockito.mock( Repository.class );
        Mockito.doReturn( MOCK_REPO_ID ).when( repository ).getId();
        Mockito.doReturn( MOCK_PATH_PREFIX ).when( repository ).getPathPrefix();

        repositoryRegistry = Mockito.mock( RepositoryRegistry.class );
        Mockito.doReturn( repository ).when( repositoryRegistry ).getRepository( MOCK_REPO_ID );
        Mockito.doThrow( new NoSuchRepositoryException( NOT_FOUND_REPO_ID ) ).when( repositoryRegistry ).getRepository(
            NOT_FOUND_REPO_ID );

        globalRestApiSettings = Mockito.mock( GlobalRestApiSettings.class );
    }

    @Test
    public void testForceBaseUrlById()
        throws Exception
    {

        Mockito.doReturn( true ).when( globalRestApiSettings ).isEnabled();
        Mockito.doReturn( true ).when( globalRestApiSettings ).isForceBaseUrl();
        Mockito.doReturn( GLOBAL_BASE_URL ).when( globalRestApiSettings ).getBaseUrl();

        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, globalRestApiSettings );

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
            new RestContentRepositoryURLBuilder( repositoryRegistry, globalRestApiSettings );

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
            new RestContentRepositoryURLBuilder( repositoryRegistry, globalRestApiSettings );

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
            new RestContentRepositoryURLBuilder( repositoryRegistry, globalRestApiSettings );

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

        Request request = new Request();
        request.setRootRef( new Reference( restletBaseURL ) );
        Response response = new Response( request );
        Response.setCurrent( response );

        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, globalRestApiSettings );

        Assert.assertEquals( restletBaseURL + "content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
                             urlFinder.getRepositoryUrl( MOCK_REPO_ID ) );
    }

    @Test( expected = NoSuchRepositoryException.class )
    public void testNotFound() throws NoSuchRepositoryException
    {
        RestContentRepositoryURLBuilder urlFinder =
            new RestContentRepositoryURLBuilder( repositoryRegistry, globalRestApiSettings );

        urlFinder.getRepositoryUrl( NOT_FOUND_REPO_ID );
    }

}
