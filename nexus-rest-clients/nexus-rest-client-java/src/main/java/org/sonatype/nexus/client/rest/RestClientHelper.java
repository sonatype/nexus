package org.sonatype.nexus.client.rest;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.sonatype.nexus.client.NexusConnectionException;
import org.sonatype.nexus.rest.model.NexusResponse;
import org.sonatype.nexus.rest.xstream.XStreamInitializer;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;

public class RestClientHelper
{
    private Client restClient = new Client( Protocol.HTTP );
    
    private ChallengeResponse challenge;
    
    private String baseUrl;

    private static final String SERVICE_URL_PART = "/service/local/";

    private Logger logger = Logger.getLogger( getClass() );

    public RestClientHelper( String baseUrl, String username, String password )
    {
        ChallengeScheme scheme = ChallengeScheme.HTTP_BASIC;
        ChallengeResponse authentication = new ChallengeResponse( scheme, username, password );
        this.challenge = authentication;
        this.baseUrl = baseUrl;
    }
    
    private String buildUrl( Method method, String service, String id )
    {
     // build the url
        StringBuffer urlBuffer = new StringBuffer( this.baseUrl );

        // make sure we have a trailing / in the baseurl
        if ( !urlBuffer.toString().endsWith( "/" ) )
        {
            urlBuffer.append( "/" );
        }

        urlBuffer.append( SERVICE_URL_PART ).append( service );

        // if this is a POST we don't know the Id, otherwise we do.
        if ( method == Method.POST )
        {
            urlBuffer.append( "/" ).append( id );
        }
        return urlBuffer.toString();
    }
    
    public void delete( String service, String id )
    {
        String url = this.buildUrl( Method.DELETE, service, id );
    }

    public Object sendMessage( Method method, String service, String id, NexusResponse nexusResponse ) throws NexusConnectionException
    {

        return this.sendMessage( method, this.buildUrl( method, service, id ), nexusResponse );
    }

    public Object sendMessage( Method method, String url, NexusResponse nexusResponse )
        throws NexusConnectionException
    {
        XStream xstream = XStreamInitializer.initialize( new XStream() );
        XStreamRepresentation representation = new XStreamRepresentation( xstream, "", MediaType.APPLICATION_XML );

        // now set the payload
        representation.setPayload( nexusResponse );

        Request request = new Request();
        request.setResourceRef( url );
        request.setMethod( method );
        request.setEntity( representation );
        request.setChallengeResponse( this.challenge );

        Response response = this.restClient.handle( request );

        // always expect a success
        if ( response.getStatus().isSuccess() )
        {
            String errorMessage = "Error in response from server: " + response.getStatus() + ".";
            try
            {
                errorMessage += "\nResponse:\n" + response.getEntity().getText();
            }
            catch ( IOException e )
            {
                logger.warn( "Error getting the response text: " + e.getMessage(), e );
            }

            throw new NexusConnectionException( errorMessage );
        }

        String responseString;
        try
        {
            responseString = response.getEntity().getText();
        }
        catch ( IOException e )
        {
            throw new NexusConnectionException( "Error getting response text: " + e.getMessage(), e );
        }
        return xstream.fromXML( responseString );
    }

//    /**
//     * The resource objects do not extend from a common base, most of the objects have a field of 'id', but some are
//     * tricky, like the user resource, which is 'username'.
//     * 
//     * @param obj
//     * @return
//     */
//    private String getObjectId( Object obj )
//    {
//        Field idField = null;
//        try
//        {
//            idField = obj.getClass().getDeclaredField( "id" );
//        }
//        catch ( SecurityException e )
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        catch ( NoSuchFieldException e )
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        idField.setAccessible( true );
//        String value = "";
//        try
//        {
//            value = idField.get( obj ).toString();
//        }
//        catch ( IllegalArgumentException e )
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        catch ( IllegalAccessException e )
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        return value;
//    }

}
