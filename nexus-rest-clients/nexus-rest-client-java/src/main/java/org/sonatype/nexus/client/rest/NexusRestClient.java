package org.sonatype.nexus.client.rest;

import java.util.List;

import org.restlet.data.Method;
import org.sonatype.nexus.client.NexusClient;
import org.sonatype.nexus.client.NexusClientException;
import org.sonatype.nexus.client.NexusConnectionException;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;

public class NexusRestClient
    implements NexusClient
{

    private RestClientHelper clientHelper = null;

    public void connect( String baseUrl, String username, String password )
    {
        this.clientHelper = new RestClientHelper( baseUrl, username, password );

    }

    public RepositoryBaseResource createRepository( RepositoryBaseResource repo )
        throws NexusConnectionException, NexusClientException
    {
        RepositoryResourceResponse repoResponseRequest = new RepositoryResourceResponse();
        repoResponseRequest.setData( repo );

        Object tempObj = this.clientHelper.sendMessage( Method.POST, "repositories", null, repoResponseRequest );

        // expecting an instance of RepositoryResourceResponse
        if ( tempObj instanceof RepositoryResourceResponse )
        {
            RepositoryResourceResponse repoResponse = (RepositoryResourceResponse) tempObj;
            return repoResponse.getData();
        }
        else
        {
            throw new NexusClientException(
                                            "Response from server returned an unexpected object.  Expected: RepositoryResourceResponse, actual: "
                                                + tempObj.getClass() );
        }
    }

    public void deleteRepository( String id ) throws NexusConnectionException, NexusClientException
    {
        // TODO Auto-generated method stub

    }

    public void disconnect() throws NexusConnectionException, NexusClientException
    {
        // TODO Auto-generated method stub

    }

    public RepositoryBaseResource getRepository( String id ) throws NexusConnectionException, NexusClientException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<RepositoryListResource> getRespositories() throws NexusConnectionException, NexusClientException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RepositoryBaseResource updateRepository( RepositoryBaseResource repo ) throws NexusConnectionException, NexusClientException
    {
        RepositoryResourceResponse repoResponseRequest = new RepositoryResourceResponse();
        repoResponseRequest.setData( repo );

        Object tempObj = this.clientHelper.sendMessage( Method.PUT, "repositories", null, repoResponseRequest );

        // expecting an instance of RepositoryResourceResponse
        if ( tempObj instanceof RepositoryResourceResponse )
        {
            RepositoryResourceResponse repoResponse = (RepositoryResourceResponse) tempObj;
            return repoResponse.getData();
        }
        else
        {
            throw new NexusClientException(
                                            "Response from server returned an unexpected object.  Expected: RepositoryResourceResponse, actual: "
                                                + tempObj.getClass() );
        }
    }

}
