/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.integrationtests;

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
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.plexus.util.IOUtil;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.test.utils.ResponseMatchers;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.notNullValue;

/**
 * HTTP Request helper (trying to hide any mention of the actual request implementation.)
 * <p/>
 * <b>NOTE</b>: The do${HTTP_METHOD}* methods without a {@link org.hamcrest.Matcher} parameter will automatically assert that the
 * received response is successful.
 * <p/>
 * <b>IMPORTANT</b>: Any {@link org.restlet.data.Response} instances returned from methods here should have their
 * {@link org.restlet.data.Response#release()} method called in a finally block when you are done with it.
 */
public class NexusRestClient
{

    public static final String SERVICE_LOCAL = "service/local/";

    private static final Logger LOG = LoggerFactory.getLogger( NexusRestClient.class );

    private static final Client client;

    private final TestContext testContext;

    static
    {
        // Restlet client
        Context ctx = new Context();
        // this is HttpClientHelper parameter
        // This below borks IT util like TaskSchedleUtil, that expects to be able to
        // "hang" on a request for a long time...
        // ctx.getParameters().add( "readTimeout", "5000" );
        ctx.getParameters().add( "maxConnectionsPerHost", "20" );
        client = new Client( ctx, Protocol.HTTP );
    }

    public NexusRestClient( final TestContext testContext )
    {
        this.testContext = checkNotNull( testContext );
    }

    public TestContext getTestContext()
    {
        return testContext;
    }

    /**
     * Extract text from response
     *
     * @param response
     * @return
     * @throws java.io.IOException
     */
    public String extractText( final Response response )
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
     * @throws java.io.IOException
     */
    public Status extractStatus( final Response response )
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
    public void releaseResponse( final Response response )
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
     * @throws java.io.IOException
     */
    public URL toNexusURL( final String serviceURIPart )
        throws IOException
    {
        checkNotNull( serviceURIPart );
        final String serviceURI = testContext.getNexusUrl() + serviceURIPart;
        return new URL( serviceURI );

    }

    /**
     * Sends a GET request to the specified uri and returns the text of the entity. This method asserts a successful
     * response by passing {@link org.sonatype.nexus.test.utils.ResponseMatchers#isSuccessful()} to {@link #doGetForText(String, org.hamcrest.Matcher)}.
     * <p/>
     * Using this method is RECOMMENDED if you simply want the text of a response and nothing more since this method
     * ensures proper cleanup of any sockets, streams, etc., by releasing the response.
     * <p/>
     * Of course the entire response text is buffered in memory so use this wisely.
     *
     * @param serviceURIpart the non-null part of the uri to fetch that is appended to the Nexus base URI.
     * @return the complete response body text
     * @throws NullPointerException if serviceURIpart is null
     */
    public String doGetForText( final String serviceURIpart )
        throws IOException
    {
        return doGetForText( serviceURIpart, ResponseMatchers.isSuccessful() );
    }

    public String doGetForText( final String serviceURIpart, final Matcher<Response> responseMatcher )
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
     * @throws java.io.IOException
     */
    public String doGetForText( final String serviceURIpart, final XStreamRepresentation representation,
                                Matcher<Response> responseMatcher )
        throws IOException
    {
        checkNotNull( serviceURIpart );
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
     *
     * @param serviceURIpart
     * @return a non-null Status with no other assertions made on it.
     * @throws java.io.IOException
     */
    public Status doGetForStatus( final String serviceURIpart )
        throws IOException
    {
        return doGetForStatus( serviceURIpart, null );
    }

    /**
     * GET the status of a request at the specified uri part, asserting that the returned status is not null.
     * <p/>
     * If matcher is non-null, the matcher is applied to the status before returning and this may throw an {@link AssertionError}.
     *
     * @param serviceURIpart url part to be appended to Nexus root url
     * @return a non-null Status with no other assertions made on it.
     * @throws java.io.IOException
     * @throws AssertionError      if the matcher is non-null and it fails
     */
    public Status doGetForStatus( final String serviceURIpart, Matcher<Status> matcher )
        throws IOException
    {
        checkNotNull( serviceURIpart );
        Response response = null;
        try
        {
            response = sendMessage( serviceURIpart, Method.GET );
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
    public void doGet( final String serviceURIpart )
        throws IOException
    {
        doGet( serviceURIpart, ResponseMatchers.isSuccessful() );
    }

    public void doGet( final String serviceURIpart, Matcher<Response> matcher )
        throws IOException
    {
        checkNotNull( serviceURIpart );
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
     * <p/>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link org.restlet.data.Response#release()}
     *
     * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
     * @return the response of the request
     * @throws java.io.IOException if there is a problem communicating the response
     */
    public Response doGetRequest( final String serviceURIpart )
        throws IOException
    {
        return sendMessage( serviceURIpart, Method.GET );
    }

    public void doPut( final String serviceURIpart, final XStreamRepresentation representation,
                       Matcher<Response> matcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = sendMessage( toNexusURL( serviceURIpart ), Method.PUT, representation, matcher );
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
     * @throws java.io.IOException
     */
    public Status doPutForStatus( final String serviceURIpart, final XStreamRepresentation representation,
                                  Matcher<Response> matcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = sendMessage( toNexusURL( serviceURIpart ), Method.PUT, representation, matcher );
            return extractStatus( response );
        }
        finally
        {
            releaseResponse( response );
        }
    }

    public String doPutForText( final String serviceURIpart, final Representation representation )
        throws IOException
    {
        return doPutForText( serviceURIpart, representation, ResponseMatchers.isSuccessful() );
    }

    public String doPutForText( final String serviceURIpart, final Representation representation,
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

    public void doPost( final String serviceURIpart, final Representation representation,
                        Matcher<Response> responseMatcher )
        throws IOException
    {
        sendMessage( serviceURIpart, Method.POST, representation, responseMatcher );
        // return doPostForStatus(serviceURIpart,representation, null);
    }

    public String doPostForText( final String serviceURIpart, final Representation representation )
        throws IOException
    {
        return doPostForText( serviceURIpart, representation, ResponseMatchers.isSuccessful() );
    }

    public String doPostForText( final String serviceURIpart, final Representation representation,
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

    public Status doPostForStatus( final String serviceURIpart, final Representation representation )
        throws IOException
    {
        return doPostForStatus( serviceURIpart, representation, null );
    }

    public Status doPostForStatus( final String serviceURIpart, final Representation representation,
                                   Matcher<Response> responseMatcher )
        throws IOException
    {
        Response response = null;
        try
        {
            response = sendMessage( serviceURIpart, Method.POST, representation, responseMatcher );
            Status status = response.getStatus();
            assertThat( status, notNullValue() );
            return status;
        }
        finally
        {
            releaseResponse( response );
        }
    }

    public void doDelete( final String serviceURIpart )
        throws IOException
    {
        doDelete( serviceURIpart, ResponseMatchers.isSuccessful() );
    }

    public void doDelete( final String serviceURIpart, Matcher<Response> responseMatcher )
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

    public Status doDeleteForStatus( final String serviceURIpart, Matcher<Response> responseMatcher )
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
     * <p/>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link org.restlet.data.Response#release()}
     *
     * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
     * @param method         the method type of the request
     * @return the response of the request
     * @throws java.io.IOException if there is a problem communicating the response
     */
    public Response sendMessage( final String serviceURIpart, final Method method )
        throws IOException
    {
        return sendMessage( serviceURIpart, method, null );
    }

    /**
     * Send a message to a resource and return the response.
     * <p/>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link org.restlet.data.Response#release()}
     *
     * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
     * @param method         the method type of the request
     * @param representation the representation to map the response to, may be null
     * @return the response of the request
     * @throws java.io.IOException if there is a problem communicating the response
     */
    public Response sendMessage( final String serviceURIpart, final Method method,
                                 final Representation representation )
        throws IOException
    {
        return sendMessage( toNexusURL( serviceURIpart ), method, representation );
    }

    public Response sendMessage( final URL url, final Method method, final Representation representation )
        throws IOException
    {
        return sendMessage( url, method, representation, null );
    }

    public Response sendMessage( final String uriPart, final Method method, final Representation representation,
                                 Matcher<Response> matcher )
        throws IOException
    {
        return sendMessage( toNexusURL( uriPart ), method, representation, matcher );
    }

    /**
     * Send a message to a resource and return the response.
     * <p/>
     * Ensure you explicity clean up the response entity returned by this method by calling {@link org.restlet.data.Response#release()}
     *
     * @param url            the absolute url of the resource to request
     * @param method         the method type of the request
     * @param representation the representation to map the response to, may be null
     * @return the response of the request
     * @throws java.io.IOException if there is a problem communicating the response
     */
    public Response sendMessage( final URL url, final Method method, final Representation representation,
                                 Matcher<Response> matchers )
        throws IOException
    {

        checkNotNull( url );
        checkNotNull( method );
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

    public Response sendMessage( final Request request, final Matcher<Response> matchers )
        throws IOException
    {
        checkNotNull( request );

        // check the text context to see if this is a secure test
        if ( testContext.isSecureTest() )
        {
            // ChallengeScheme scheme = new ChallengeScheme( "HTTP_NxBasic", "NxBasic", "Nexus Basic" );
            ChallengeResponse authentication =
                new ChallengeResponse(
                    ChallengeScheme.HTTP_BASIC, testContext.getUsername(), testContext.getPassword()
                );
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
     * @param url        the url to fetch the file from
     * @param targetFile the location where to save the download
     * @return a File instance for the saved file
     * @throws java.io.IOException if there is a problem saving the file
     */
    public File downloadFile( URL url, String targetFile )
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
     * Execute a HTTPClient method in the context of a test.
     * make decisions how to execute.
     * <p/>
     * NOTE: Before being returned, {@link org.apache.commons.httpclient.HttpMethod#releaseConnection()} is called on the {@link org.apache.commons.httpclient.HttpMethod} instance,
     * therefore subsequent calls to get response body as string may return nulls.
     */
    public HttpMethod executeHTTPClientMethod( HttpMethod method )
        throws IOException
    {
        return executeHTTPClientMethod( method, true );
    }

    /**
     * Execute a HTTPClient method, optionally in the context of a test.
     * <p/>
     * NOTE: Before being returned, {@link org.apache.commons.httpclient.HttpMethod#releaseConnection()} is called on the {@link org.apache.commons.httpclient.HttpMethod} instance,
     * therefore subsequent calls to get response body as string may return nulls.
     *
     * @param method         the method to execute
     * @param useTestContext if true, execute this request in the context of a Test, false means ignore the testContext
     *                       settings
     * @return the HttpMethod instance passed into this method
     * @throws java.io.IOException
     */
    public HttpMethod executeHTTPClientMethod( final HttpMethod method, final boolean useTestContext )
        throws IOException
    {
        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( 10000 );
        httpClient.getHttpConnectionManager().getParams().setSoTimeout( 10000 );

        if ( useTestContext )
        {
            if ( testContext.isSecureTest() )
            {
                httpClient.getState().setCredentials( AuthScope.ANY,
                                                      new UsernamePasswordCredentials( testContext.getUsername(),
                                                                                       testContext.getPassword() ) );

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

    /**
     * Clocks how much time it takes to download a give url
     *
     * @param url address to download
     * @return time in milliseconds
     * @throws Exception
     */
    public long clockUrlDownload( URL url )
        throws IOException
    {
        return clockUrlDownload( url, -1 );
    }

    /**
     * Clocks how much time it takes to download a give url
     *
     * @param url        address to download
     * @param speedLimit max speed while downloading in Kbps, -1 to no speed limit
     * @return time in milliseconds
     * @throws Exception
     */
    public long clockUrlDownload( URL url, int speedLimit )
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
            MatcherAssert.assertThat( result, ResponseMatchers.isSuccessfulCode() );
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
