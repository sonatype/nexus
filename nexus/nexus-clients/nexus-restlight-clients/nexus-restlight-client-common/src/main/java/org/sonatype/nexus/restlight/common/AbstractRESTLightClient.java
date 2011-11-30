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
package org.sonatype.nexus.restlight.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.plexus.util.IOUtil;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;

/**
 * <p>
 * Parent class for all restlight clients which implements basic version-vocabulary loading, along with generic GET and
 * POST calls that should standardize and simplify most communication with the Nexus server.
 * </p>
 * <p>
 * Vocabularies allow the client to capture slight api changes from version to version, and load the appropriate terms
 * for the specific Nexus server we're attempting to reach. Terms may be element names, XPaths, etc. but may not
 * (currently) capture differences in the HTTP conversations that take place (i.e. we cannot capture the case where a
 * new version requires an extra API call that an older version didn't require).
 * </p>
 */
public abstract class AbstractRESTLightClient
{

    /**
     * This is the base-path for all REST API calls into a Nexus server.
     */
    public static final String SVC_BASE = "/service/local";

    /**
     * This is the Nexus REST API path for the status resource, which contains the API version.
     */
    protected static final String STATUS_PATH = SVC_BASE + "/status";

    /**
     * HTTP request parameter that conveys a particular artifact's groupId.
     */
    protected static final String GROUP_ID_PARAM = "g";

    /**
     * HTTP request parameter that conveys a particular artifact's artifactId.
     */
    protected static final String ARTIFACT_ID_PARAM = "a";

    /**
     * HTTP request parameter that conveys a particular artifact's version.
     */
    protected static final String VERSION_PARAM = "v";

    /**
     * <p>
     * Classpath resource that contains pointers to the specific vocabulary files used for each version of Nexus.
     * </p>
     * <p>
     * Each version of Nexus has its own line, and the whole file looks something like this:
     * </p>
     * <p>
     * 
     * <pre>
     * # comment
     * default=default
     * 1.3.2=default,1.3.2
     * </pre>
     * 
     * </p>
     * <p>
     * The above file will require the following classpath resources:
     * </p>
     * <ul>
     * <li>default.vocabulary.properties</li>
     * <li>1.3.2.vocabulary.properties</li>
     * </ul>
     * <p>
     * <b>NOTE:</b> At a MINIMUM, you should include the default mappings, even if the default vocabulary properties are
     * empty.
     * </p>
     */
    protected static final String VOCAB_MANIFEST = "vocabulary.lst";
    
    private Logger logger = LoggerFactory.getLogger( getClass() );

    private final String baseUrl;

    private final String user;

    private final String password;

    private HttpClient client;

    private Properties vocabulary;

    private final String vocabBasepath;
    
    private final ProxyConfig proxyConfig;
    

    /**
     * @see AbstractRESTLightClient#AbstractRESTLightClient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.sonatype.nexus.restlight.common.ProxyConfig) 
     */
    protected AbstractRESTLightClient( final String baseUrl, final String user, final String password,
                                       final String vocabBasepath )
        throws RESTLightClientException
    {
        this(baseUrl, user, password, vocabBasepath, null);
    }

    /**
     * Instantiate and connect new REST client. For now, connecting simply means retrieving the Nexus server's
     * apiVersion, so we can load the appropriate vocabulary items.
     * <p>
     * ProxyConfig is optional.
     * 
     */
    protected AbstractRESTLightClient( final String baseUrl, final String user, final String password,
                                       final String vocabBasepath, final ProxyConfig proxyConfig)
        throws RESTLightClientException
    {
        this.baseUrl = baseUrl;
        this.user = user;
        this.password = password;

        if ( vocabBasepath == null )
        {
            this.vocabBasepath = "";
        }
        else if ( !vocabBasepath.endsWith( "/" ) )
        {
            this.vocabBasepath = vocabBasepath + "/";
        }
        else
        {
            this.vocabBasepath = vocabBasepath;
        }

        this.proxyConfig = proxyConfig;
        
        connect();
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }
   
    
    /**
     * Retrieve the base URL used to connect to Nexus. This URL contains everything <b>up to, but not including</b> the
     * {@link AbstractRESTLightClient#SVC_BASE} base-path.
     */
    protected final String getBaseURL()
    {
        return baseUrl;
    }

    /**
     * Retrieve the username used to connect to the Nexus instance.
     */
    protected final String getUser()
    {
        return user;
    }

    protected void connect()
        throws RESTLightClientException
    {
        client = new HttpClient();

        UsernamePasswordCredentials creds = new UsernamePasswordCredentials( user, password );

        List<String> policies = new ArrayList<String>();
        policies.add( NxBasicScheme.POLICY_NAME );
        policies.add( "BASIC" );

        AuthPolicy.registerAuthScheme( NxBasicScheme.POLICY_NAME, NxBasicScheme.class );
        AuthPolicy.registerAuthScheme( "BASIC", BasicScheme.class );

        client.getParams().setParameter( AuthPolicy.AUTH_SCHEME_PRIORITY, policies );

        client.getState().setCredentials( AuthScope.ANY, creds );

        ProxyConfig proxy = getProxyConfig();
        if(proxy != null){
            client.getHostConfiguration().setProxy(proxy.getHost(), proxy.getPort());
            if(proxy.getUsername() != null){
                Credentials credentials = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
                AuthScope authScope = new AuthScope(proxy.getHost(), proxy.getPort());
                client.getState().setProxyCredentials(authScope, credentials);
            }
        }
        
        loadVocabulary();
    }

    /**
     * Retrieve the {@link Properties} instance that stores vocabulary items specific to the version of Nexus we're
     * currently talking to.
     */
    protected final Properties getVocabulary()
    {
        return vocabulary;
    }

    private void loadVocabulary()
        throws RESTLightClientException
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        InputStream stream = cloader.getResourceAsStream( vocabBasepath + VOCAB_MANIFEST );

        if ( stream == null )
        {
            logger.debug(
                "Cannot locate REST vocabulary variants manifest on classpath: " + vocabBasepath + VOCAB_MANIFEST
                    + ". Vocabularies will not be loaded." );
            return;
        }

        Map<String, List<String>> vocabManifest = new HashMap<String, List<String>>();
        try
        {
            Properties vProps = new Properties();
            vProps.load( stream );
            for ( Map.Entry<Object, Object> lstEntry : vProps.entrySet() )
            {
                List<String> lst = new ArrayList<String>();

                if ( lstEntry.getValue() == null )
                {
                    continue;
                }

                StringTokenizer tokens = new StringTokenizer( (String) lstEntry.getValue(), "," );
                while ( tokens.hasMoreTokens() )
                {
                    lst.add( tokens.nextToken().trim() );
                }

                vocabManifest.put( (String) lstEntry.getKey(), lst );
            }
        }
        catch ( IOException e )
        {
            throw new RESTLightClientException( "Failed to load REST vocabulary manifest from classpath: "
                + vocabBasepath + VOCAB_MANIFEST, e );
        }
        finally
        {
            IOUtil.close( stream );
        }

        String version = getApiVersion();
        if ( version.endsWith( "-SNAPSHOT" ) )
        {
            version = version.substring( 0, version.length() - "-SNAPSHOT".length() );
        }

        List<String> vocabs = getVocabilary( vocabManifest, version );
        if ( vocabs == null )
        {
            vocabs = vocabManifest.get( "default" );
        }

        if ( vocabs != null )
        {
            Properties loadedVocabulary = new Properties();
            for ( String vocab : vocabs )
            {
                Properties props = new Properties();

                String vocabResource = vocabBasepath + vocab + ".vocabulary.properties";

                stream = cloader.getResourceAsStream( vocabResource );
                if ( stream == null )
                {
                    throw new RESTLightClientException( "Failed to load REST vocabulary from classpath: "
                        + vocabResource );
                }

                try
                {
                    props.load( stream );
                }
                catch ( IOException e )
                {
                    throw new RESTLightClientException( "Failed to load REST vocabulary from classpath: "
                        + vocabResource, e );
                }

                loadedVocabulary.putAll( props );
            }

            this.vocabulary = loadedVocabulary;
        }
        else
        {
            logger.debug(
                "Cannot locate REST vocabulary variants in manifest file: " + vocabBasepath + VOCAB_MANIFEST
                    + ". No vocabulary will be loaded." );
        }
    }

    protected List<String> getVocabilary( Map<String, List<String>> vocabManifest, String version )
    {
        if ( vocabManifest.containsKey( version ) )
        {
            return vocabManifest.get( version );
        }

        try
        {
            GenericVersionScheme scheme = new GenericVersionScheme();
            Version gVersion = scheme.parseVersion( version );

            String matchVocabilary = "default";
            for ( String key : vocabManifest.keySet() )
            {
                if ( "default".equals( key ) )
                {
                    continue;
                }

                if ( gVersion.compareTo( scheme.parseVersion( key ) ) >= 0 )
                {
                    matchVocabilary = key;
                }

            }

            return vocabManifest.get( matchVocabilary );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            return null;
        }

    }

    /**
     * Retrieve the Nexus REST API version currently in use.
     * 
     * @throws RESTLightClientException When the /status/data/apiVersion/text() XPath fails to select a version from the
     *             response.
     */
    public String getApiVersion()
        throws RESTLightClientException
    {
        Document doc = get( STATUS_PATH );
        String xpath = "/status/data/apiVersion/text()";
        XPath xp;
        try
        {
            xp = XPath.newInstance( xpath );
        }
        catch ( JDOMException e )
        {
            throw new RESTLightClientException( "Failed to build xpath: '" + xpath + "'.", e );
        }

        Text result;
        try
        {
            result = (Text) xp.selectSingleNode( doc.getRootElement() );
        }
        catch ( JDOMException e )
        {
            throw new RESTLightClientException( "XPath selection failed: '" + xpath + "' (Root node: "
                + doc.getRootElement().getName() + ").", e );
        }

        return result.getText();
    }

    /**
     * Inject the appropriate HTTP request parameters to capture the given artifact coordinate in the provided
     * parameters map.
     */
    protected void mapCoord( final String groupId, final String artifactId, final String version,
                             final Map<String, String> params )
    {
        params.put( GROUP_ID_PARAM, groupId );
        params.put( ARTIFACT_ID_PARAM, artifactId );
        params.put( VERSION_PARAM, version );
        params.put( "t", "maven2" );
    }

    /**
     * Submit a GET request to the absolute URL given, and parse the response as an XML {@link Document} (JDOM)
     * instance.
     */
    protected Document getAbsolute( final String url )
        throws RESTLightClientException
    {
        return get( url, null, true );
    }

    /**
     * Submit a GET request to the absolute URL given, and parse the response as an XML {@link Document} (JDOM)
     * instance. Use the given requestParams map to inject into the HTTP GET method.
     */
    protected Document getAbsolute( final String url, final Map<String, ? extends Object> requestParams )
        throws RESTLightClientException
    {
        return get( url, requestParams, true );
    }

    /**
     * Submit a GET request to the URL <b>path</b> given (relative to the Nexus base-URL given in the constructor), and
     * parse the response as an XML {@link Document} (JDOM) instance.
     */
    protected Document get( final String path )
        throws RESTLightClientException
    {
        return get( path, null, false );
    }

    /**
     * Submit a GET request to the URL <b>path</b> given (relative to the Nexus base-URL given in the constructor), and
     * parse the response as an XML {@link Document} (JDOM) instance. Use the given requestParams map to inject into the
     * HTTP GET method.
     */
    protected Document get( final String path, final Map<String, ? extends Object> requestParams )
        throws RESTLightClientException
    {
        return get( path, requestParams, false );
    }

    /**
     * <p>
     * Low-level GET implementation.
     * </p>
     * <p>
     * Submit a GET request to the URL given (absolute or relative-to-base-URL depends on urlIsAbsolute flag), and parse
     * the response as an XML {@link Document} (JDOM) instance. Use the given requestParams map to inject into the HTTP
     * GET method.
     * </p>
     */
    @SuppressWarnings( "unchecked" )
    protected Document get( final String url, final Map<String, ? extends Object> requestParams,
                            final boolean urlIsAbsolute )
        throws RESTLightClientException
    {
        GetMethod method = urlIsAbsolute ? new GetMethod( url ) : new GetMethod( formatUrl( baseUrl, url ) );
        method.addRequestHeader( "Content-Type", "application/xml" );

        addRequestParams( method, requestParams );

        try
        {
            client.executeMethod( method );
        }
        catch ( HttpException e )
        {
            throw new RESTLightClientException( "GET request execution failed. Reason: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RESTLightClientException( "GET request execution failed. Reason: " + e.getMessage(), e );
        }

        int status = method.getStatusCode();
        String statusText = method.getStatusText();

        if ( status != 200 )
        {
            throw validationError( method );
        }

        String body = null;
        try
        {
            body = method.getResponseBodyAsString();
            return new SAXBuilder().build( new StringReader( body ) );
        }
        catch ( JDOMException e )
        {
            throw new RESTLightClientException( "Failed to parse response body as XML for GET request. Content: \n"
                + body + "\nStatus: " + statusText, e );
        }
        catch ( IOException e )
        {
            throw new RESTLightClientException( "Could not retrieve body as a String from GET request.", e );
        }
        finally
        {
            method.releaseConnection();
        }
    }

    protected RESTLightClientException validationError( HttpMethod method )
    {
        String errorBody = null;
        try
        {
            errorBody = method.getResponseBodyAsString();
        }
        catch ( IOException e )
        {
            errorBody = "Could not retrieve error body, message lost";
        }

        return new RESTLightClientException( method.getName() + " request failed; HTTP status: " + method.getStatusCode() + ", " + method.getStatusText() + "\nHTTP body: " + errorBody );
    }

    private String formatUrl( final String baseUrl, final String path )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( baseUrl );
        if ( !baseUrl.endsWith( "/" ) && !path.startsWith( "/" ) )
        {
            sb.append( "/" );
            sb.append( path );
        }
        else if ( baseUrl.endsWith( "/" ) && path.startsWith( "/" ) )
        {
            if ( path.length() > 1 )
            {
                sb.append( path.substring( 1 ) );
            }
        }
        else
        {
            sb.append( path );
        }

        return sb.toString();
    }

    /**
     * Submit a POST request to the relative URL-path given (relative to the base-URL used to construct the client),
     * ignore the response body. Use the given requestParams map to inject into the HTTP POST method.
     */
    protected void post( final String path, final Map<String, ? extends Object> requestParams, final Document body )
        throws RESTLightClientException
    {
        doPost( path, requestParams, body, false );
    }

    /**
     * Submit a POST request to the relative URL-path given (relative to the base-URL used to construct the client), and
     * parse the response as an XML {@link Document} (JDOM) instance. Use the given requestParams map to inject into the
     * HTTP POST method.
     */
    protected Document postWithResponse( final String path, final Map<String, ? extends Object> requestParams,
                                         final Document body )
        throws RESTLightClientException
    {
        return doPost( path, requestParams, body, true );
    }

    /**
     * /**
     * <p>
     * Low-level POST implementation to a relative URL (relative to the base-URL given in the client constructor).
     * </p>
     * <p>
     * Submit a POST request to the relative URL given, and parse the response as an XML {@link Document} (JDOM)
     * instance <b>if</b> expectResponseBody == true. Use the given requestParams map to inject into the HTTP POST
     * method.
     * </p>
     * 
     * @return null if expectResponseBody == false, the result {@link Document} otherwise.
     * @throws RESTLightClientException
     */
    protected Document doPost( final String path, final Map<String, ? extends Object> requestParams,
                               final Document body, final boolean expectResponseBody )
        throws RESTLightClientException
    {
        logger.debug( "Posting to: '" + path + "'" );

        PostMethod method = new PostMethod( formatUrl( baseUrl, path ) );
        method.addRequestHeader( "Content-Type", "application/xml" );
        method.addRequestHeader( "Accept", "application/xml" );

        if ( body != null && body.getRootElement() != null )
        {
            try
            {
                method.setRequestEntity( new StringRequestEntity(
                    new XMLOutputter( Format.getCompactFormat() ).outputString( body ), "text/plain", "UTF-8" ) );
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new RESTLightClientException( "Failed to construct POST request. Reason: " + e.getMessage(), e );
            }
        }

        addRequestParams( method, requestParams );

        try
        {
            client.executeMethod( method );
        }
        catch ( HttpException e )
        {
            throw new RESTLightClientException( "POST request execution failed. Reason: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RESTLightClientException( "POST request execution failed. Reason: " + e.getMessage(), e );
        }

        int status = method.getStatusCode();
        String statusText = method.getStatusText();

        if ( status < 200 || status > 299 )
        {
            throw validationError( method );
        }

        if ( expectResponseBody )
        {
            try
            {
                return new SAXBuilder().build( method.getResponseBodyAsStream() );
            }
            catch ( JDOMException e )
            {
                throw new RESTLightClientException( "Failed to parse response body as XML for POST request.", e );
            }
            catch ( IOException e )
            {
                throw new RESTLightClientException( "Could not retrieve body as a String from POST request.", e );
            }
            finally
            {
                method.releaseConnection();
            }
        }
        else
        {
            method.releaseConnection();

            return null;
        }
    }

    /**
     * Submit a PUT request to the relative URL-path given (relative to the base-URL used to construct the client),
     * ignore the response body. Use the given requestParams map to inject into the HTTP PUT method.
     */
    protected void put( final String path, final Map<String, ? extends Object> requestParams, final Document body )
        throws RESTLightClientException
    {
        doPut( path, requestParams, body, false );
    }

    /**
     * Submit a PUT request to the relative URL-path given (relative to the base-URL used to construct the client), and
     * parse the response as an XML {@link Document} (JDOM) instance. Use the given requestParams map to inject into the
     * HTTP PUT method.
     */
    protected Document putWithResponse( final String path, final Map<String, ? extends Object> requestParams,
                                        final Document body )
        throws RESTLightClientException
    {
        return doPut( path, requestParams, body, true );
    }

    protected Document doPut( final String path, final Map<String, ? extends Object> requestParams,
                              final Document body, final boolean expectResponseBody )
        throws RESTLightClientException
    {
        logger.debug( "Putting to: '" + path + "'" );

        PutMethod method = new PutMethod( baseUrl + path );

        method.addRequestHeader( "Content-Type", "application/xml" );

        if ( body != null && body.getRootElement() != null )
        {
            try
            {
                method.setRequestEntity( new StringRequestEntity(
                    new XMLOutputter( Format.getCompactFormat() ).outputString( body ), "text/plain", "UTF-8" ) );
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new RESTLightClientException( "Failed to construct POST request. Reason: " + e.getMessage(), e );
            }
        }

        addRequestParams( method, requestParams );

        try
        {
            client.executeMethod( method );
        }
        catch ( HttpException e )
        {
            throw new RESTLightClientException( "PUT request execution failed. Reason: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RESTLightClientException( "PUT request execution failed. Reason: " + e.getMessage(), e );
        }

        int status = method.getStatusCode();

        String statusText = method.getStatusText();

        if ( status < 200 || status > 299 )
        {
            String errorBody = "";

            try
            {
                errorBody = method.getResponseBodyAsString();
            }
            catch ( IOException ioe )
            {
            }

            throw new RESTLightClientException( "PUT request failed; HTTP status: " + status + ", " + statusText
                + "\nHTTP body: " + errorBody );
        }

        if ( expectResponseBody )
        {
            try
            {
                return new SAXBuilder().build( method.getResponseBodyAsStream() );
            }
            catch ( JDOMException e )
            {
                throw new RESTLightClientException( "Failed to parse response body as XML for PUT request.", e );
            }
            catch ( IOException e )
            {
                throw new RESTLightClientException( "Could not retrieve body as a String from PUT request.", e );
            }
            finally
            {
                method.releaseConnection();
            }
        }
        else
        {
            method.releaseConnection();

            return null;
        }
    }

    @SuppressWarnings( "unchecked" )
    private void addRequestParams( final HttpMethodBase method, final Map<String, ? extends Object> requestParams )
    {
        if ( requestParams == null )
        {
            return;
        }

        List<NameValuePair> q = new ArrayList<NameValuePair>();
        for ( Map.Entry<String, ? extends Object> entry : requestParams.entrySet() )
        {
            if ( entry.getValue() instanceof Collection )
            {
                Collection<Object> values = (Collection<Object>) entry.getValue();

                if ( values != null && !values.isEmpty() )
                {
                    for ( Object val : values )
                    {
                        if ( val != null )
                        {
                            method.addRequestHeader( entry.getKey(), String.valueOf( val ) );
                        }
                    }
                }
            }
            else
            {
                method.addRequestHeader( entry.getKey(), String.valueOf( entry.getValue() ) );
            }

            q.add( new NameValuePair( entry.getKey(), String.valueOf( entry.getValue() ) ) );
        }

        method.setQueryString( q.toArray( new NameValuePair[0] ) );
    }

    /**
     * Submit a DELETE request to the relative URL-path given (relative to the base-URL used to construct the client),
     * ignore the response body. Use the given requestParams map to inject into the HTTP DELETE method.
     */
    protected void delete( final String path, final Map<String, ? extends Object> requestParams )
        throws RESTLightClientException
    {
        doDelete( path, requestParams, false );
    }

    /**
     * Submit a DELETE request to the relative URL-path given (relative to the base-URL used to construct the client),
     * and parse the response as an XML {@link Document} (JDOM) instance. Use the given requestParams map to inject into
     * the HTTP DELETE method.
     */
    protected Document deleteWithResponse( final String path, final Map<String, ? extends Object> requestParams,
                                           final Document body )
        throws RESTLightClientException
    {
        return doDelete( path, requestParams, true );
    }

    /**
     * Submit a DELETE request to the URL <b>path</b> given (relative to the Nexus base-URL given in the constructor),
     * and parse the response as an XML {@link Document} (JDOM) instance <b>if</b> expectResponseBody == true. Use the
     * given requestParams map to inject into the HTTP DELETE method.
     */
    protected Document doDelete( final String url, final Map<String, ? extends Object> requestParams,
                                 final boolean expectResponseBody )
        throws RESTLightClientException
    {
        return doDelete( url, requestParams, false, expectResponseBody );
    }

    /**
     * <p>
     * Low-level DELETE implementation.
     * </p>
     * <p>
     * Submit a DELETE request to the URL given (absolute or relative-to-base-URL depends on urlIsAbsolute flag), and
     * parse the response as an XML {@link Document} (JDOM) instance <b>if</b> expectResponseBody == true. Use the given
     * requestParams map to inject into the HTTP DELETE method.
     * </p>
     */
    protected Document doDelete( final String url, final Map<String, ? extends Object> requestParams,
                                 final boolean urlIsAbsolute, final boolean expectResponseBody )
        throws RESTLightClientException
    {
        DeleteMethod method = urlIsAbsolute ? new DeleteMethod( url ) : new DeleteMethod( baseUrl + url );

        addRequestParams( method, requestParams );

        try
        {
            client.executeMethod( method );
        }
        catch ( HttpException e )
        {
            throw new RESTLightClientException( "DELETE request execution failed. Reason: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RESTLightClientException( "DELETE request execution failed. Reason: " + e.getMessage(), e );
        }

        int status = method.getStatusCode();

        String statusText = method.getStatusText();

        if ( status != 200 )
        {
            throw validationError( method );
        }

        if ( expectResponseBody )
        {
            try
            {
                return new SAXBuilder().build( method.getResponseBodyAsStream() );
            }
            catch ( JDOMException e )
            {
                throw new RESTLightClientException( "Failed to parse response body as XML for DELETE request.", e );
            }
            catch ( IOException e )
            {
                throw new RESTLightClientException( "Could not retrieve body as a String from DELETE request.", e );
            }
            finally
            {
                method.releaseConnection();
            }
        }
        else
        {
            method.releaseConnection();

            return null;
        }
    }

}
