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
package org.sonatype.nexus.proxy.storage.remote.commonshttpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.CustomMultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RemoteAccessDeniedException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteAuthenticationNeededException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.remote.AbstractRemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * The Class CommonsHttpClientRemoteStorage.
 * 
 * @author cstamas
 */
@Component( role = RemoteRepositoryStorage.class, hint = CommonsHttpClientRemoteStorage.PROVIDER_STRING )
public class CommonsHttpClientRemoteStorage
    extends AbstractRemoteRepositoryStorage
    implements RemoteRepositoryStorage
{
    public static final String PROVIDER_STRING = "apacheHttpClient3x";

    public static final String CTX_KEY = PROVIDER_STRING;

    public static final String CTX_KEY_CLIENT = CTX_KEY + ".client";

    public static final String CTX_KEY_HTTP_CONFIGURATION = CTX_KEY + ".httpConfiguration";

    public static final String CTX_KEY_S3_FLAG = CTX_KEY + ".remoteIsAmazonS3";

    public static final String NEXUS_MISSING_ARTIFACT_HEADER = "x-nexus-missing-artifact";

    // ===============================================================================
    // RemoteStorage iface

    public String getProviderId()
    {
        return PROVIDER_STRING;
    }

    public void validateStorageUrl( String url )
        throws RemoteStorageException
    {
        try
        {
            URL u = new URL( url );

            if ( !"http".equals( u.getProtocol().toLowerCase() ) && !"https".equals( u.getProtocol().toLowerCase() ) )
            {
                throw new RemoteStorageException( "Unsupported protocol, only HTTP/HTTPS protocols are supported: "
                    + u.getProtocol().toLowerCase() );
            }
        }
        catch ( MalformedURLException e )
        {
            throw new RemoteStorageException( "Malformed URL", e );
        }
    }

    public boolean isReachable( ProxyRepository repository, ResourceStoreRequest request )
        throws RemoteAccessException, RemoteStorageException
    {
        boolean result = false;

        try
        {
            request.pushRequestPath( RepositoryItemUid.PATH_ROOT );

            try
            {
                result = checkRemoteAvailability( 0, repository, request, false );
            }
            catch ( RemoteAccessDeniedException e )
            {
                // NEXUS-3338: we have to swallow this on S3
                // NEXUS-4593: 403 is always "ok"
                result = true;
            }
        }
        finally
        {
            request.popRequestPath();
        }

        return result;
    }

    public boolean containsItem( long newerThen, ProxyRepository repository, ResourceStoreRequest request )
        throws RemoteAccessException, RemoteStorageException
    {
        return checkRemoteAvailability( newerThen, repository, request, true );
    }

    public AbstractStorageItem retrieveItem( ProxyRepository repository, ResourceStoreRequest request, String baseUrl )
        throws ItemNotFoundException, RemoteAccessException, RemoteStorageException
    {
        URL remoteURL = getAbsoluteUrlFromBase( baseUrl, request.getRequestPath() );

        HttpMethod method = null;

        method = new GetMethod( remoteURL.toString() );

        int response = executeMethod( repository, request, method, remoteURL );

        if ( response == HttpStatus.SC_OK )
        {

            if ( method.getPath().endsWith( "/" ) )
            {
                // this is a collection and not a file!
                // httpClient will follow redirections, and the getPath()
                // _should_
                // give us URL with ending "/"
                method.releaseConnection();

                throw new ItemNotFoundException(
                    "The remoteURL we got to looks like is a collection, and Nexus cannot fetch collections over plain HTTP (remoteUrl=\""
                        + remoteURL.toString() + "\")", request, repository );
            }

            GetMethod get = (GetMethod) method;

            InputStream is = null;

            try
            {
                is = get.getResponseBodyAsStream();
                if ( get.getResponseHeader( "Content-Encoding" ) != null
                    && "gzip".equals( get.getResponseHeader( "Content-Encoding" ).getValue() ) )
                {
                    is = new GZIPInputStream( is );
                }

                String mimeType = null;

                if ( method.getResponseHeader( "content-type" ) != null )
                {
                    mimeType = method.getResponseHeader( "content-type" ).getValue();
                }
                else
                {
                    mimeType = getMimeSupport().guessMimeTypeFromPath( repository.getMimeRulesSource(), request.getRequestPath() );
                }

                DefaultStorageFileItem httpItem =
                    new DefaultStorageFileItem( repository, request, true, true, new PreparedContentLocator(
                        new HttpClientInputStream( get, is ), mimeType ) );

                if ( get.getResponseContentLength() != -1 )
                {
                    // FILE
                    httpItem.setLength( get.getResponseContentLength() );
                }

                httpItem.setRemoteUrl( remoteURL.toString() );

                httpItem.setModified( makeDateFromHeader( method.getResponseHeader( "last-modified" ) ) );

                httpItem.setCreated( httpItem.getModified() );

                httpItem.getItemContext().putAll( request.getRequestContext() );

                return httpItem;
            }
            catch ( IOException ex )
            {
                method.releaseConnection();

                throw new RemoteStorageException( "IO Error during response stream handling [repositoryId=\""
                    + repository.getId() + "\", requestPath=\"" + request.getRequestPath() + "\", remoteUrl=\""
                    + remoteURL.toString() + "\"]!", ex );
            }
            catch ( RuntimeException ex )
            {
                method.releaseConnection();

                throw ex;
            }
        }
        else
        {
            method.releaseConnection();

            if ( response == HttpStatus.SC_NOT_FOUND )
            {
                throw new ItemNotFoundException(
                    "The remoteURL we requested does not exists on remote server (remoteUrl=\"" + remoteURL.toString()
                        + "\")", request, repository );
            }
            else
            {
                throw new RemoteStorageException( "The method execution returned result code " + response
                    + ". [repositoryId=\"" + repository.getId() + "\", requestPath=\"" + request.getRequestPath()
                    + "\", remoteUrl=\"" + remoteURL.toString() + "\"]" );
            }
        }
    }

    public void storeItem( ProxyRepository repository, StorageItem item )
        throws UnsupportedStorageOperationException, RemoteAccessException, RemoteStorageException
    {
        if ( !( item instanceof StorageFileItem ) )
        {
            throw new UnsupportedStorageOperationException( "Storing of non-files remotely is not supported!" );
        }

        StorageFileItem fItem = (StorageFileItem) item;

        ResourceStoreRequest request = new ResourceStoreRequest( item );

        URL remoteURL = getAbsoluteUrlFromBase( repository, request );

        PutMethod method = new PutMethod( remoteURL.toString() );

        try
        {
            method.setRequestEntity( new InputStreamRequestEntity( fItem.getInputStream(), fItem.getLength(),
                fItem.getMimeType() ) );

            int response = executeMethod( repository, request, method, remoteURL );

            if ( response != HttpStatus.SC_OK && response != HttpStatus.SC_CREATED
                && response != HttpStatus.SC_NO_CONTENT && response != HttpStatus.SC_ACCEPTED )
            {
                throw new RemoteStorageException( "Unexpected response code while executing " + method.getName()
                    + " method [repositoryId=\"" + repository.getId() + "\", requestPath=\"" + request.getRequestPath()
                    + "\", remoteUrl=\"" + remoteURL.toString() + "\"]. Expected: \"any success (2xx)\". Received: "
                    + response + " : " + HttpStatus.getStatusText( response ) );
            }
        }
        catch ( IOException e )
        {
            throw new RemoteStorageException( e.getMessage() + " [repositoryId=\"" + repository.getId()
                + "\", requestPath=\"" + request.getRequestPath() + "\", remoteUrl=\"" + remoteURL.toString() + "\"]",
                e );
        }
        finally
        {
            method.releaseConnection();
        }
    }

    public void deleteItem( ProxyRepository repository, ResourceStoreRequest request )
        throws ItemNotFoundException, UnsupportedStorageOperationException, RemoteAccessException,
        RemoteStorageException
    {
        URL remoteURL = getAbsoluteUrlFromBase( repository, request );

        DeleteMethod method = new DeleteMethod( remoteURL.toString() );

        try
        {
            int response = executeMethod( repository, request, method, remoteURL );

            if ( response != HttpStatus.SC_OK && response != HttpStatus.SC_NO_CONTENT
                && response != HttpStatus.SC_ACCEPTED )
            {
                throw new RemoteStorageException( "The response to HTTP " + method.getName()
                    + " was unexpected HTTP Code " + response + " : " + HttpStatus.getStatusText( response )
                    + " [repositoryId=\"" + repository.getId() + "\", requestPath=\"" + request.getRequestPath()
                    + "\", remoteUrl=\"" + remoteURL.toString() + "\"]" );
            }
        }
        finally
        {
            method.releaseConnection();
        }
    }

    /**
     * Gets the http client.
     * 
     * @return the http client
     */
    protected void updateContext( ProxyRepository repository, RemoteStorageContext ctx )
    {
        getLogger().info(
            "Remote storage settings change detected for ProxyRepository ID=\"" + repository.getId() + "\" (\""
                + repository.getName() + "\"), updating HttpClient..." );

        HttpClient httpClient = new HttpClient( new CustomMultiThreadedHttpConnectionManager() );

        HttpClientProxyUtil.applyProxyToHttpClient( httpClient, ctx, getLogger() );

        ctx.putContextObject( CTX_KEY_CLIENT, httpClient );

        ctx.putContextObject( CTX_KEY_HTTP_CONFIGURATION, httpClient.getHostConfiguration() );

        // NEXUS-3338: we don't know afer config change is remote S3 (url changed maybe)
        ctx.putContextObject( CTX_KEY_S3_FLAG, new BooleanFlagHolder() );
    }

    /**
     * Execute method. In case of any exception thrown by HttpClient, it will release the connection. In other cases it
     * is the duty of caller to do it, or process the input stream.
     * 
     * @param method the method
     * @return the int
     */
    protected int executeMethod( ProxyRepository repository, ResourceStoreRequest request, HttpMethod method,
                                 URL remoteUrl )
        throws RemoteAccessException, RemoteStorageException
    {
        URI methodURI = null;

        try
        {
            methodURI = method.getURI();
        }
        catch ( URIException e )
        {
            getLogger().debug( "Could not format debug log message", e );
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Invoking HTTP " + method.getName() + " method against remote location " + methodURI );
        }

        RemoteStorageContext ctx = getRemoteStorageContext( repository );

        HttpClient httpClient = (HttpClient) ctx.getContextObject( CTX_KEY_CLIENT );

        HostConfiguration httpConfiguration = (HostConfiguration) ctx.getContextObject( CTX_KEY_HTTP_CONFIGURATION );

        method.setRequestHeader( new Header( "user-agent", formatUserAgentString( ctx, repository ) ) );
        method.setRequestHeader( new Header( "accept", "*/*" ) );
        method.setRequestHeader( new Header( "accept-language", "en-us" ) );
        method.setRequestHeader( new Header( "accept-encoding", "gzip, identity" ) );
        method.setRequestHeader( new Header( "cache-control", "no-cache" ) );

        // HTTP keep alive should not be used, except when NTLM is used
        Boolean isNtlmUsed = (Boolean) ctx.getContextObject( HttpClientProxyUtil.NTLM_IS_IN_USE_KEY );

        if ( isNtlmUsed == null || !isNtlmUsed )
        {
            method.setRequestHeader( new Header( "Connection", "close" ) );
            method.setRequestHeader( new Header( "Proxy-Connection", "close" ) );
        }

        method.setFollowRedirects( true );

        if ( StringUtils.isNotBlank( ctx.getRemoteConnectionSettings().getQueryString() ) )
        {
            method.setQueryString( ctx.getRemoteConnectionSettings().getQueryString() );
        }

        int resultCode = 0;

        try
        {
            resultCode = httpClient.executeMethod( httpConfiguration, method );

            checkForRemotePeerAmazonS3Storage( repository, method );

            Header proxyReturnedErrorHeader = method.getResponseHeader( NEXUS_MISSING_ARTIFACT_HEADER );
            boolean proxyReturnedError =
                proxyReturnedErrorHeader != null && Boolean.valueOf( proxyReturnedErrorHeader.getValue() );

            if ( resultCode == HttpStatus.SC_FORBIDDEN )
            {
                throw new RemoteAccessDeniedException( repository, remoteUrl,
                    HttpStatus.getStatusText( HttpStatus.SC_FORBIDDEN ) );
            }
            else if ( resultCode == HttpStatus.SC_UNAUTHORIZED )
            {
                throw new RemoteAuthenticationNeededException( repository,
                    HttpStatus.getStatusText( HttpStatus.SC_UNAUTHORIZED ) );
            }
            else if ( resultCode == HttpStatus.SC_OK && proxyReturnedError )
            {
                throw new RemoteStorageException(
                    "Invalid artifact found, most likely a proxy redirected to an HTML error page." );
            }
        }
        catch ( RemoteStorageException e )
        {
            method.releaseConnection();

            throw e;
        }
        catch ( HttpException ex )
        {
            method.releaseConnection();

            throw new RemoteStorageException( "Protocol error while executing " + method.getName()
                + " method. [repositoryId=\"" + repository.getId() + "\", requestPath=\"" + request.getRequestPath()
                + "\", remoteUrl=\"" + methodURI + "\"]", ex );
        }
        catch ( IOException ex )
        {
            method.releaseConnection();

            throw new RemoteStorageException( "Transport error while executing " + method.getName()
                + " method [repositoryId=\"" + repository.getId() + "\", requestPath=\"" + request.getRequestPath()
                + "\", remoteUrl=\"" + methodURI + "\"]", ex );
        }

        return resultCode;
    }

    /**
     * Make date from header.
     * 
     * @param date the date
     * @return the long
     */
    protected long makeDateFromHeader( Header date )
    {
        long result = System.currentTimeMillis();
        if ( date != null )
        {
            try
            {
                result = DateUtil.parseDate( date.getValue() ).getTime();
            }
            catch ( DateParseException ex )
            {
                getLogger().warn(
                    "Could not parse date '" + date + "', using system current time as item creation time.", ex );
            }
            catch ( NullPointerException ex )
            {
                getLogger().warn( "Parsed date is null, using system current time as item creation time." );
            }
        }
        return result;
    }

    /**
     * Initially, this method is here only to share the code for "availability check" and for "contains" check.
     * Unfortunately, the "availability" check cannot be done at RemoteStorage level, since it is completely repository
     * layout unaware and is able to tell only about the existence of remote server and that the URI on it exists. This
     * "availability" check will have to be moved upper into repository, since it is aware of "what it holds".
     * Ultimately, this method will check is the remote server "present" and is responding or not. But nothing more.
     * 
     * @param newerThen
     * @param repository
     * @param context
     * @param path
     * @param relaxedCheck
     * @return
     * @throws RemoteAuthenticationNeededException
     * @throws RemoteAccessException
     * @throws RemoteStorageException
     */
    protected boolean checkRemoteAvailability( long newerThen, ProxyRepository repository,
                                               ResourceStoreRequest request, boolean isStrict )
        throws RemoteAuthenticationNeededException, RemoteAccessException, RemoteStorageException
    {
        URL remoteURL = getAbsoluteUrlFromBase( repository, request );

        HttpMethodBase method = new HeadMethod( remoteURL.toString() );

        int response = HttpStatus.SC_BAD_REQUEST;

        // artifactory hack, it pukes on HEAD so we will try with GET if HEAD fails
        boolean doGet = false;

        try
        {
            response = executeMethod( repository, request, method, remoteURL );
        }
        catch ( RemoteStorageException e )
        {
            // If HEAD failed, attempt a GET. Some repos may not support HEAD method
            doGet = true;

            getLogger().debug( "HEAD method failed, will attempt GET.  Exception: " + e.getMessage(), e );
        }
        finally
        {
            method.releaseConnection();

            // HEAD returned error, but not exception, try GET before failing
            if ( !doGet && response != HttpStatus.SC_OK )
            {
                doGet = true;

                getLogger().debug( "HEAD method failed, will attempt GET.  Status: " + response );
            }
        }

        if ( doGet )
        {
            // create a GET
            method = new GetMethod( remoteURL.toString() );

            try
            {
                // execute it
                response = executeMethod( repository, request, method, remoteURL );
            }
            finally
            {
                // and release it immediately
                method.releaseConnection();
            }
        }

        // if we are not strict and remote is S3
        if ( !isStrict && isRemotePeerAmazonS3Storage( repository ) )
        {
            // if we are relaxed, we will accept any HTTP response code below 500. This means anyway the HTTP
            // transaction succeeded. This method was never really detecting that the remoteUrl really denotes a root of
            // repository (how could we do that?)
            // this "relaxed" check will help us to "pass" S3 remote storage.
            return response >= HttpStatus.SC_OK && response <= HttpStatus.SC_INTERNAL_SERVER_ERROR;
        }
        else
        {
            // non relaxed check is strict, and will select only the OK response
            if ( response == HttpStatus.SC_OK )
            {
                // we have it
                // we have newer if this below is true
                return makeDateFromHeader( method.getResponseHeader( "last-modified" ) ) > newerThen;
            }
            else if ( ( response >= HttpStatus.SC_MULTIPLE_CHOICES && response < HttpStatus.SC_BAD_REQUEST )
                || response == HttpStatus.SC_NOT_FOUND )
            {
                return false;
            }
            else
            {
                throw new RemoteStorageException( "Unexpected response code while executing " + method.getName()
                    + " method [repositoryId=\"" + repository.getId() + "\", requestPath=\"" + request.getRequestPath()
                    + "\", remoteUrl=\"" + remoteURL.toString() + "\"]. Expected: \"SUCCESS (200)\". Received: "
                    + response + " : " + HttpStatus.getStatusText( response ) );
            }
        }
    }

    /**
     * Returns {@code true} if only and only if we are positive that remote peer (remote URL of passed in
     * ProxyRepository) points to a remote repository that is hosted by Amazon S3 Storage. This method will return false
     * as long as we don't make very 1st HTTP request to remote peer. After that 1st request, we retain the status until
     * ProxyRepository configuration changes. See {@link https://issues.sonatype.org/browse/NEXUS-3338} for more.
     * 
     * @param repository that needs to be checked.
     * @return true only if we know that ProxyRepository in question points to Amazon S3 storage.
     * @throws RemoteStorageException in case of some error.
     */
    public boolean isRemotePeerAmazonS3Storage( ProxyRepository repository )
        throws RemoteStorageException
    {
        RemoteStorageContext ctx = getRemoteStorageContext( repository );

        // it is S3 if we have CTX_KEY_S3_FLAG set, the flag value is not null, and flag value is true
        // if flag is False, we know it is not S3
        // if flag is null, we still did not contact remote, so we were not able to tell yet
        return ctx.hasContextObject( CTX_KEY_S3_FLAG )
            && ( (BooleanFlagHolder) getRemoteStorageContext( repository ).getContextObject( CTX_KEY_S3_FLAG ) ).isFlag() != null
            && ( (BooleanFlagHolder) getRemoteStorageContext( repository ).getContextObject( CTX_KEY_S3_FLAG ) ).isFlag();
    }

    protected void checkForRemotePeerAmazonS3Storage( ProxyRepository repository, HttpMethod method )
        throws RemoteStorageException
    {
        RemoteStorageContext ctx = getRemoteStorageContext( repository );

        // we already know the result, do nothing
        if ( ctx.hasContextObject( CTX_KEY_S3_FLAG )
            && ( (BooleanFlagHolder) getRemoteStorageContext( repository ).getContextObject( CTX_KEY_S3_FLAG ) ).isFlag() != null )
        {
            return;
        }

        // for now, we check the HTTP response header "Server: AmazonS3"
        Header hdr = method.getResponseHeader( "server" );

        boolean isAmazonS3 = ( hdr != null ) && ( hdr.getValue().toLowerCase().contains( "amazons3" ) );

        if ( ctx.hasContextObject( CTX_KEY_S3_FLAG ) )
        {
            ( (BooleanFlagHolder) ctx.getContextObject( CTX_KEY_S3_FLAG ) ).setFlag( isAmazonS3 );
        }

        if ( isAmazonS3 )
        {
            getLogger().warn(
                "The proxy repository \""
                    + repository.getName()
                    + "\" (ID="
                    + repository.getId()
                    + ") is backed by Amazon S3 service. This means that Nexus can't reliably detect the validity of your setup (baseUrl of proxy repository)!" );
        }
    }

    // ==

    protected static class BooleanFlagHolder
    {
        private Boolean flag = null;

        public Boolean isFlag()
        {
            return flag;
        }

        public void setFlag( Boolean flag )
        {
            this.flag = flag;
        }
    }

}
