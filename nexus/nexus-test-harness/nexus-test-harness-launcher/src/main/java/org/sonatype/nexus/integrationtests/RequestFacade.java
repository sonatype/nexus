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
package org.sonatype.nexus.integrationtests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.test.utils.ResponseMatchers.isSuccessful;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.codehaus.plexus.util.IOUtil;
import org.hamcrest.Matcher;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.sonatype.nexus.test.utils.ResponseMatchers;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * HTTP Request helper (trying to hide any mention of the actual request implementation.)
 * <p>
 * <b>NOTE</b>: The do${HTTP_METHOD}* methods without a {@link Matcher} parameter will automatically assert that the
 * received response is successful.
 * <p>
 * <b>IMPORTANT</b>: Any {@link Response} instances returned from methods here should have their
 * {@link Response#release()} method called in a finally block when you are done with it.
 */
public class RequestFacade
{

    public static final String SERVICE_LOCAL = "service/local/";

    private static final Logger LOG = LoggerFactory.getLogger( RequestFacade.class );

    private static final Client client;

    static
    {
        // Restlet client
        Context ctx = new Context();
        // this is HttpClientHelper parameter
        // This below borks IT util like TaskSchedleUtil, that expects to be able to
        // "hang" on a request for a long time...
        // ctx.getParameters().add( "readTimeout", "5000" );
        ctx.getParameters().add( "maxConnectionsPerHost", "20");
        client = new Client( ctx, Protocol.HTTP );
    }

    /**
     * Extract text from response
     *
     * @param response
     * @return
     * @throws IOException
     */
    private static String extractText( final Response response )
        throws IOException
    {
        final Representation entity = response.getEntity();
        assertThat( entity, notNullValue() );
        final String responseText = entity.getText();
        return responseText;
    }

    /**
     * Extract status from response
     *
     * @param response
     * @return
     * @throws IOException
     */
    private static Status extractStatus( final Response response )
        throws IOException
    {
        Status status = response.getStatus();
        assertThat( status, notNullValue() );
        return status;
    }

    /**
     * Null safe method to release a Response ( its streams and sockets )
     *
     * @param response release the response if not null
     */
    public static void releaseResponse( final Response response )
    {
        if ( response != null )
        {
            response.release();
        }
    }

    /**
     * Convert a serviceURIPart to a URL
     *
     * @param serviceURIPart
     * @return
     * @throws IOException
     */
    public static URL toNexusURL( final String serviceURIPart )
        throws IOException
    {
        Preconditions.checkNotNull( serviceURIPart );
        final String serviceURI = AbstractNexusIntegrationTest.nexusBaseUrl + serviceURIPart;
        return new URL( serviceURI );

    }

    /**
     * Sends a GET request to the specified uri and returns the text of the entity. This method asserts a successful
     * response by passing {@link ResponseMatchers#isSuccessful()} to {@link #doGetForText(String, Matcher)}.
     * <p>
     * Using this method is RECOMMENDED if you simply want the text of a response and nothing more since this method
     * ensures proper cleanup of any sockets, streams, etc., by releasing the response.
     * <p>
     * Of course the entire response text is buffered in memory so use this wisely.
     *
     * @param serviceURIpart the non-null part of the uri to fetch that is appended to the Nexus base URI.
     * @return the complete response body text
     * @throws NullPointerException if serviceURIpart is null
     */
    public static String doGetForText( final String serviceURIpart )
        throws IOException
    {
        return doGetForText( serviceURIpart, isSuccessful() );
    }

    public static String doGetForText( final String serviceURIpart, final org.hamcrest.Matcher<Response> responseMatcher )
        throws IOException
    {
        return doGetForText( serviceURIpart, null, responseMatcher );
    }

    /**
     * Gets the response text, asserting that the entity is not null, and also applying any specified assertions on the
     * response instance.
     *
     * @param serviceURIpart
     * @param responseMatcher
     * @return
     * @throws IOException
     */
    public static String doGetForText( final String serviceURIpart, final XStreamRepresentation representation,
                                       org.hamcrest.Matcher<Response> responseMatcher )
        throws IOException
    {
        Preconditions.checkNotNull( serviceURIpart );
        Response response = null;
        try
        {
            // deliberately passing null for matcher since we do the check after getting the text below.
            response = sendMessage( toNexusURL( serviceURIpart ), Method.GET, representation, null );
            final Representation entity = response.getEntity();
            assertThat( entity, notNullValue() );
            final String responseText = entity.getText();
            if ( responseMatcher != null )
            {
                assertThat( response, responseMatcher );
            }
            return responseText;
        }
        finally
        {
            releaseResponse( response );
        }
    }

    /**
     * Make a request to the service uri specified and return the Status
     * @param serviceURIpart
     * @return a non-null Status with no other assertions made on it.
     * @throws IOException
     */
    public static Status doGetForStatus( final String serviceURIpart )
        throws IOException
    {
        return doGetForStatus( serviceURIpart, null);
    }

    /**
     * GET the status of a request at the specified uri part, asserting that the returned status is not null.
     * <p>
     * If matcher is non-null, the matcher is applied to the status before returning and this may throw an {@link AssertionError}.
     *
     * @param serviceURIpart url part to be appended to Nexus root url
     * @return a non-null Status with no other assertions made on it.
     * @throws IOException
     * @throws AssertionError if the matcher is non-null and it fails
     */
    public static Status doGetForStatus( final String serviceURIpart, Matcher<Status> matcher )
        throws IOException
    {
        Preconditions.checkNotNull( serviceURIpart );
        Response response = null;
        try
        {
            response = RequestFacade.sendMessage( serviceURIpart, Method.GET );
            Status status = response.getStatus();
            assertThat( status, notNullValue() );
            if ( matcher != null )
            {
                assertThat( status, matcher );
            }

            return status;
        }
        finally
        {
            releaseResponse( response );
        }
    }

    /**
     * Execute the GET request and assert a successful response.
     */
    public static void doGet( final String serviceURIpart )
        throws IOException
    {
        doGet( serviceURIpart, ResponseMatchers.isSuccessful() );
    }

    public static void doGet( final String serviceURIpart, Matcher<Response> matcher )
        throws IOException
    {
        Preconditions.checkNotNull( serviceURIpart );
        Response response = doGetRequest( serviceURIpart );
        try
        {
            assertThat( response, matcher );
        }
        finally
        {
            releaseResponse( response );
        }
    }

    /**
     * FIXME this is used everywhere Send a message to a resource as a GET request and return the response.
     * <p>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
     *
     * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
     * @return the response of the request
     * @throws IOException if there is a problem communicating the response
     */
    public static Response doGetRequest( final String serviceURIpart )
        throws IOException
    {
        return sendMessage( serviceURIpart, Method.GET );
    }

    public static void doPut( final String serviceURIpart, final XStreamRepresentation representation,
                              Matcher<Response> matcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = RequestFacade.sendMessage( toNexusURL( serviceURIpart ), Method.PUT, representation, matcher );
        }
        finally
        {
            releaseResponse( response );
        }
    }

    /**
     * PUT a representation to the specified URI
     *
     * @param serviceURIpart
     * @param representation
     * @return
     * @throws IOException
     */
    public static Status doPutForStatus( final String serviceURIpart, final XStreamRepresentation representation,
                                         Matcher<Response> matcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = RequestFacade.sendMessage( toNexusURL( serviceURIpart ), Method.PUT, representation, matcher );
            return extractStatus( response );
        }
        finally
        {
            releaseResponse( response );
        }
    }

    public static String doPutForText( final String serviceURIpart, final Representation representation )
        throws IOException
    {
        return doPutForText( serviceURIpart, representation, isSuccessful() );
    }

    public static String doPutForText( final String serviceURIpart, final Representation representation,
                                       Matcher<Response> responseMatcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = sendMessage( toNexusURL( serviceURIpart ), Method.PUT, representation, responseMatcher );
            return extractText( response );
        }
        finally
        {
            releaseResponse( response );
        }
    }

    public static void doPost( final String serviceURIpart, final Representation representation,
                               Matcher<Response> responseMatcher )
        throws IOException
    {
        RequestFacade.sendMessage( serviceURIpart, Method.POST, representation, responseMatcher );
        // return doPostForStatus(serviceURIpart,representation, null);
    }

    public static String doPostForText( final String serviceURIpart, final Representation representation )
        throws IOException
    {
        return doPostForText( serviceURIpart, representation, isSuccessful() );
    }

    public static String doPostForText( final String serviceURIpart, final Representation representation,
                                        Matcher<Response> responseMatcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = sendMessage( toNexusURL( serviceURIpart ), Method.POST, representation, responseMatcher );
            return extractText( response );
        }
        finally
        {
            releaseResponse( response );
        }
    }

    public static Status doPostForStatus( final String serviceURIpart, final Representation representation )
        throws IOException
    {
        return doPostForStatus( serviceURIpart, representation, null );
    }

    public static Status doPostForStatus( final String serviceURIpart, final Representation representation,
                                          Matcher<Response> responseMatcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = RequestFacade.sendMessage( serviceURIpart, Method.POST, representation, responseMatcher );
            Status status = response.getStatus();
            assertThat( status, notNullValue() );
            return status;
        }
        finally
        {
            releaseResponse( response );
        }
    }

    public static void doDelete( final String serviceURIpart )
        throws IOException
    {
        doDelete( serviceURIpart, isSuccessful() );
    }

    public static void doDelete( final String serviceURIpart, Matcher<Response> responseMatcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = sendMessage( toNexusURL( serviceURIpart ), Method.DELETE, null, responseMatcher );
        }
        finally
        {
            releaseResponse( response );
        }
    }

    public static Status doDeleteForStatus( final String serviceURIpart, Matcher<Response> responseMatcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = sendMessage( toNexusURL( serviceURIpart ), Method.DELETE, null, responseMatcher );
            Status status = response.getStatus();
            assertThat( status, notNullValue() );
            return status;
        }
        finally
        {
            releaseResponse( response );
        }
    }

    /**
     * Send a message to a resource and return the response.
     * <p>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
     *
     * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
     * @param method the method type of the request
     * @return the response of the request
     * @throws IOException if there is a problem communicating the response
     */
    public static Response sendMessage( final String serviceURIpart, final Method method )
        throws IOException
    {
        return sendMessage( serviceURIpart, method, null );
    }

    /**
     * Send a message to a resource and return the response.
     * <p>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
     *
     * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
     * @param method the method type of the request
     * @param representation the representation to map the response to, may be null
     * @return the response of the request
     * @throws IOException if there is a problem communicating the response
     */
    public static Response sendMessage( final String serviceURIpart, final Method method,
                                        final Representation representation )
        throws IOException
    {
        return sendMessage( toNexusURL( serviceURIpart ), method, representation );
    }

    public static Response sendMessage( final URL url, final Method method, final Representation representation )
        throws IOException
    {
        return sendMessage( url, method, representation, null );
    }

    public static Response sendMessage( final String uriPart, final Method method, final Representation representation,
                                        Matcher<Response> matcher )
        throws IOException
    {
        return sendMessage( toNexusURL( uriPart ), method, representation, matcher );
    }

    /**
     * Send a message to a resource and return the response.
     * <p>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
     *
     * @param url the absolute url of the resource to request
     * @param method the method type of the request
     * @param representation the representation to map the response to, may be null
     * @return the response of the request
     * @throws IOException if there is a problem communicating the response
     */
    public static Response sendMessage( final URL url, final Method method, final Representation representation,
                                        org.hamcrest.Matcher<Response> matchers )
        throws IOException
    {

        Preconditions.checkNotNull( url );
        Preconditions.checkNotNull( method );
        // represenation may be null
        // matchers may be null
        final Request request = new Request();
        request.setResourceRef( url.toString() );
        request.setMethod( method );

        if ( !Method.GET.equals( method ) && !Method.DELETE.equals( method ) )
        {
            request.setEntity( representation );
        }

        // change the MediaType if this is a GET, default to application/xml
        if ( Method.GET.equals( method ) )
        {
            if ( representation != null )
            {
                request.getClientInfo().getAcceptedMediaTypes().add(
                    new Preference<MediaType>( representation.getMediaType() ) );
            }
        }

        return sendMessage( request, matchers );
    }

    public static Response sendMessage( final Request request, final org.hamcrest.Matcher<Response> matchers )
        throws IOException
    {
        Preconditions.checkNotNull( request );

        // check the text context to see if this is a secure test
        TestContext context = TestContainer.getInstance().getTestContext();
        if ( context.isSecureTest() )
        {
            // ChallengeScheme scheme = new ChallengeScheme( "HTTP_NxBasic", "NxBasic", "Nexus Basic" );
            ChallengeResponse authentication =
                new ChallengeResponse( ChallengeScheme.HTTP_BASIC, context.getUsername(), context.getPassword() );
            request.setChallengeResponse( authentication );
        }

        LOG.debug( "sendMessage: " + request.getMethod().getName() + " " + request.getResourceRef().toString() );
        Response response = client.handle( request );
        if ( matchers != null )
        {
            try
            {
                assertThat( response, matchers );
            }
            catch ( AssertionError e )
            {
                releaseResponse( response );
                throw e;
            }
        }

        return response;
    }

    /**
     * Download a file at a url and save it to the target file location specified by targetFile.
     *
     * @param url the url to fetch the file from
     * @param targetFile the location where to save the download
     * @return a File instance for the saved file
     * @throws IOException if there is a problem saving the file
     */
    public static File downloadFile( URL url, String targetFile )
        throws IOException
    {
        OutputStream out = null;
        InputStream in = null;
        File downloadedFile = new File( targetFile );
        Response response = null;
        try
        {
            response = sendMessage( url, Method.GET, null );

            if ( !response.getStatus().isSuccess() )
            {
                throw new FileNotFoundException( response.getStatus() + " - " + url );
            }

            // if this is null then someone was getting really creative with the tests, but hey, we will let them...
            if ( downloadedFile.getParentFile() != null )
            {
                downloadedFile.getParentFile().mkdirs();
            }

            in = response.getEntity().getStream();
            out = new BufferedOutputStream( new FileOutputStream( downloadedFile ) );

            IOUtil.copy( in, out, 1024 );
        }
        finally
        {
            IOUtil.close( in );
            IOUtil.close( out );
            releaseResponse( response );
        }
        return downloadedFile;
    }

    /**
     * Execute a HTTPClient method in the context of a test. ie it will use {@link TestContainer#getTestContext()} to
     * make decisions how to execute.
     * <p>
     * NOTE: Before being returned, {@link HttpMethod#releaseConnection()} is called on the {@link HttpMethod} instance,
     * therefore subsequent calls to get response body as string may return nulls.
     */
    public static HttpMethod executeHTTPClientMethod( HttpMethod method )
        throws HttpException, IOException
    {
        return executeHTTPClientMethod( method, true );
    }

    /**
     * Execute a HTTPClient method, optionally in the context of a test. ie {@link TestContainer#getTestContext()}
     * <p>
     * NOTE: Before being returned, {@link HttpMethod#releaseConnection()} is called on the {@link HttpMethod} instance,
     * therefore subsequent calls to get response body as string may return nulls.
     *
     * @param method the method to execute
     * @param useTestContext if true, execute this request in the context of a Test, false means ignore the testContext
     *            settings
     * @return the HttpMethod instance passed into this method
     * @throws HttpException
     * @throws IOException
     */
    public static HttpMethod executeHTTPClientMethod( final HttpMethod method, final boolean useTestContext )
        throws HttpException, IOException
    {
        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( 5000 );
        httpClient.getHttpConnectionManager().getParams().setSoTimeout( 5000 );

        if ( useTestContext )
        {
            // check the text context to see if this is a secure test
            TestContext context = TestContainer.getInstance().getTestContext();
            if ( context.isSecureTest() )
            {
                httpClient.getState().setCredentials( AuthScope.ANY,
                    new UsernamePasswordCredentials( context.getUsername(), context.getPassword() ) );

                List<String> authPrefs = new ArrayList<String>( 1 );
                authPrefs.add( AuthPolicy.BASIC );
                httpClient.getParams().setParameter( AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs );
                httpClient.getParams().setAuthenticationPreemptive( true );
            }
        }

        try
        {
            httpClient.executeMethod( method );
            method.getResponseBodyAsString(); // forced consumption of response I guess
            return method;
        }
        finally
        {
            method.releaseConnection();

            // force socket cleanup
            HttpConnectionManager mgr = httpClient.getHttpConnectionManager();

            if ( mgr instanceof SimpleHttpConnectionManager )
            {
                ( (SimpleHttpConnectionManager) mgr ).shutdown();

            }
        }
    }

    public static AuthenticationInfo getWagonAuthenticationInfo()
    {
        AuthenticationInfo authInfo = null;
        // check the text context to see if this is a secure test
        TestContext context = TestContainer.getInstance().getTestContext();
        if ( context.isSecureTest() )
        {
            authInfo = new AuthenticationInfo();
            authInfo.setUserName( context.getUsername() );
            authInfo.setPassword( context.getPassword() );
        }
        return authInfo;
    }

    /**
     * Clocks how much time it takes to download a give url
     * 
     * @param url address to download
     * @return time in milliseconds
     * @throws Exception
     */
    public static long clockUrlDownload( URL url )
        throws IOException
    {
        return clockUrlDownload( url, -1 );
    }

    /**
     * Clocks how much time it takes to download a give url
     * 
     * @param url address to download
     * @param speedLimit max speed while downloading in Kbps, -1 to no speed limit
     * @return time in milliseconds
     * @throws Exception
     */
    public static long clockUrlDownload( URL url, int speedLimit )
        throws IOException
    {
        long t = System.currentTimeMillis();

        GetMethod get = null;
        InputStream in = null;
        try
        {
            HttpClient client = new HttpClient();
            get = new GetMethod( url.toString() );
            int result = client.executeMethod( get );
            assertThat( result, ResponseMatchers.isSuccessfulCode() );
            in = get.getResponseBodyAsStream();
            byte[] b;
            if ( speedLimit != -1 )
            {
                b = new byte[speedLimit * 1024];
            }
            else
            {
                b = new byte[1024];
            }
            while ( in.read( b ) != -1 )
            {
                if ( speedLimit != -1 )
                {
                    try
                    {
                        Thread.sleep( 1000 );
                    }
                    catch ( InterruptedException e )
                    {
                        // ignore
                    }
                }
            }
        }
        finally
        {
            IOUtil.close( in );
            if ( get != null )
            {
                get.releaseConnection();
            }
        }

        return System.currentTimeMillis() - t;
    }
}
