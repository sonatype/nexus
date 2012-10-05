/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.repository.ClientSSLRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.NtlmRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext.BooleanFlagHolder;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.util.SystemPropertiesHelper;

import com.google.common.annotations.VisibleForTesting;

/**
 * Utilities related to HTTP client. This whole class will need to be reworked, as it started as simple
 * "static utility class", but today it;s grown out it's limit to be still that. It would probably need to be
 * componentized.
 * 
 * @since 2.0
 */
public class HttpClientUtil
{

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    /**
     * Context key of HTTP client.
     */
    private static final String CTX_KEY_CLIENT = ".client";

    /**
     * Context key of a flag present in case that remote server is an Amazon S3.
     */
    private static final String CTX_KEY_S3_FLAG = ".remoteIsAmazonS3";

    // ==
    // HTTPClient4x pool and connection eviction related settings

    /**
     * Key of optional system property for customizing the connection pool size. If not present HTTP client default is
     * used (20 connections)
     * 
     * @deprecated This key is deprecated, use {@link #CONNECTION_POOL_SIZE_SUFFIX} instead.
     */
    @Deprecated
    public static final String CONNECTION_POOL_SIZE_KEY_DEPRECATED = "httpClient.connectionPoolSize";

    /**
     * The common prefix for all parameters.
     */
    public static final String PARAMETER_PREFIX = "nexus.apacheHttpClient4x.";

    /**
     * Key for customizing connection pool size. Value should be integer equal to 0 or greater. Pool size of 0 will
     * actually prevent use of pool. Any positive number means the actual size of the pool to be created.
     */
    public static final String CONNECTION_POOL_SIZE_SUFFIX = "connectionPoolSize";

    /**
     * Default pool size: 20.
     */
    public static final int CONNECTION_POOL_SIZE_DEFAULT = 20;

    /**
     * Key for customizing connection pool keep-alive. In other words, how long open connections (sockets) are kept in
     * pool before evicted and closed. Value is milliseconds.
     */
    public static final String CONNECTION_POOL_KEEPALIVE_SUFFIX = "connectionPoolKeepalive";

    /**
     * Default pool keep-alive: 1 minute.
     */
    public static final long CONNECTION_POOL_KEEPALIVE_DEFAULT = TimeUnit.MINUTES.toMillis( 1 );

    /**
     * Key for customizing connection pool timeout. In other words, how long should a HTTP request execution be blocked
     * when pool is depleted, for a connection. Value is milliseconds.
     */
    public static final String CONNECTION_POOL_TIMEOUT_SUFFIX = "connectionPoolTimeout";

    /**
     * Default pool timeout: equals to {@link #CONNECTION_POOL_KEEPALIVE_DEFAULT}.
     */
    public static final long CONNECTION_POOL_TIMEOUT_DEFAULT = TimeUnit.MINUTES.toMillis( 1 );

    // --

    static final String CLIENT_ID_KEY = "nexus.apacheHttpClient4x.id";

    static final String CLIENT_DESIGNATOR_KEY = "nexus.apacheHttpClient4x.designator";

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger( HttpClientUtil.class );

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * Creates and prepares an http client instance by using configuration present in {@link RemoteStorageContext}.
     * <p/>
     * This implies:<br/>
     * * setting up connection pool using number of connections specified by system property
     * {@link #CONNECTION_POOL_SIZE_KEY}<br/>
     * * setting timeout as configured for repository<br/>
     * * (if necessary) configure authentication<br/>
     * * (if necessary) configure proxy as configured for repository. This method should be used only by
     * {@link RemoteRepositoryStorage} implementations using HttpClient 4.x!
     * 
     * @return the created http client
     * @param proxyRepository the Proxy repository on who's behalf we are creating HTTP client instance.
     * @param ctxPrefix context keys prefix
     * @param ctx remote repository context
     * @param logger logger
     * @throws IllegalStateException when creation of client fails (usually lack of TLS/SSL support in JVM)
     */
    static HttpClient configure( final ProxyRepository proxyRepository, final String ctxPrefix,
                                 final RemoteStorageContext ctx, final Logger logger )
        throws IllegalStateException
    {
        final DefaultHttpClient httpClient = configure( proxyRepository.getId(), ctx );

        // NEXUS-5125 do not redirect to index pages
        httpClient.setRedirectStrategy( new DefaultRedirectStrategy()
        {
            @Override
            public boolean isRedirected( final HttpRequest request, final HttpResponse response,
                                         final HttpContext context )
                throws ProtocolException
            {
                return super.isRedirected( request, response, context )
                    && !response.getFirstHeader( "location" ).getValue().endsWith( "/" );
            }
        } );

        // put client into context
        ctx.putContextObject( ctxPrefix + CTX_KEY_CLIENT, httpClient );
        // NEXUS-3338: we don't know after config change is remote S3 (url changed maybe)
        ctx.putContextObject( ctxPrefix + CTX_KEY_S3_FLAG, new BooleanFlagHolder() );

        return httpClient;
    }

    /**
     * Creates and configures a HTTP Client. When you are done using it, you should {@link #release(HttpClient)} it.
     * Note: as this method is NOT {@link RemoteRepositoryStorage} specific at all, but is rather "generic" HTTP Client
     * factory, it should be considered to move this out from here, to some other package!
     * 
     * @param designator the designator or {@code null}
     * @param ctx the {@link RemoteStorageContext} to use to fetch auth and proxy info. See
     *            {@link ApplicationConfiguration#getGlobalRemoteStorageContext()} if you want a HTTP client instance
     *            that obeys Nexus configuration for example.
     * @return HttpClient instance preconfigured for use.
     * @throws IllegalStateException when client was not created due to (usually TLS/SSL related) some problem.
     * @since 2.2
     */
    public static DefaultHttpClient configure( final String designator, final RemoteStorageContext ctx )
        throws IllegalStateException
    {
        final DefaultHttpClient httpClient =
            new DefaultHttpClient( createConnectionManager( designator ), createHttpParams( designator,
                ctx.getRemoteConnectionSettings() ) )
            {
                @Override
                protected BasicHttpProcessor createHttpProcessor()
                {
                    final BasicHttpProcessor result = super.createHttpProcessor();
                    result.addResponseInterceptor( new ResponseContentEncoding() );
                    return result;
                }
            };
        // needed for internal bookkeeping
        httpClient.getParams().setParameter( CLIENT_ID_KEY, UUID.randomUUID().toString() );
        httpClient.getParams().setParameter( CLIENT_DESIGNATOR_KEY, designator );
        configureAuthentication( httpClient, ctx.getRemoteAuthenticationSettings(), null );
        configureProxy( httpClient, ctx.getRemoteProxySettings() );

        ConnectionPoolEvictingThread.INSTANCE.register( httpClient );

        logAction( "Created", httpClient );

        return httpClient;
    }

    /**
     * Releases the current HTTP client (if any) and removes context objects associated with Proxy repositories. This
     * method should be used only by {@link RemoteRepositoryStorage} implementations using HttpClient 4.x!
     * 
     * @param ctxPrefix context keys prefix
     * @param ctx remote repository context
     */
    static void release( final String ctxPrefix, final RemoteStorageContext ctx )
    {
        if ( ctx.hasContextObject( ctxPrefix + CTX_KEY_CLIENT ) )
        {
            HttpClient httpClient = (HttpClient) ctx.getContextObject( ctxPrefix + CTX_KEY_CLIENT );
            release( httpClient );
            ctx.removeContextObject( ctxPrefix + CTX_KEY_CLIENT );
        }
        ctx.removeContextObject( ctxPrefix + CTX_KEY_S3_FLAG );
    }

    /**
     * Releases the passed in HTTP client. Should be called if the client was created using
     * {@link #configure(String, RemoteStorageContext)} method of this class. Note: as this method is NOT
     * {@link RemoteRepositoryStorage} specific at all, but is rather "generic" HTTP Client factory, it should be
     * considered to move this out from here, to some other package!
     * 
     * @param httpClient
     * @since 2.2
     */
    public static void release( final HttpClient httpClient )
    {
        logAction( "Releasing ", httpClient );
        ConnectionPoolEvictingThread.INSTANCE.unregister( httpClient );
        httpClient.getConnectionManager().shutdown();
    }

    // ==

    /**
     * Returns the HTTP client for context.
     * 
     * @param ctxPrefix context keys prefix
     * @param ctx remote repository context
     * @return HTTP client or {@code null} if not yet configured
     */
    static HttpClient getHttpClient( final String ctxPrefix, final RemoteStorageContext ctx )
    {
        return (HttpClient) ctx.getContextObject( ctxPrefix + CTX_KEY_CLIENT );
    }

    /**
     * Exposes the Amazon S3 flag key.
     * 
     * @param ctxPrefix context keys prefix
     * @returnAmazon S3 flag key
     */
    static String getS3FlagKey( final String ctxPrefix )
    {
        return ctxPrefix + CTX_KEY_S3_FLAG;
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    private static void configureAuthentication( final DefaultHttpClient httpClient,
                                                 final RemoteAuthenticationSettings ras, final HttpHost proxyHost )
    {
        if ( ras != null )
        {
            String authScope = "target";
            if ( proxyHost != null )
            {
                authScope = proxyHost.toHostString() + " proxy";
            }

            List<String> authorisationPreference = new ArrayList<String>( 2 );
            authorisationPreference.add( AuthPolicy.DIGEST );
            authorisationPreference.add( AuthPolicy.BASIC );
            Credentials credentials = null;
            if ( ras instanceof ClientSSLRemoteAuthenticationSettings )
            {
                throw new IllegalArgumentException( "SSL client authentication not yet supported!" );
            }
            else if ( ras instanceof NtlmRemoteAuthenticationSettings )
            {
                final NtlmRemoteAuthenticationSettings nras = (NtlmRemoteAuthenticationSettings) ras;
                // Using NTLM auth, adding it as first in policies
                authorisationPreference.add( 0, AuthPolicy.NTLM );
                LOGGER.info( "... {} authentication setup for NTLM domain '{}'", authScope, nras.getNtlmDomain() );
                credentials =
                    new NTCredentials( nras.getUsername(), nras.getPassword(), nras.getNtlmHost(), nras.getNtlmDomain() );
            }
            else if ( ras instanceof UsernamePasswordRemoteAuthenticationSettings )
            {
                final UsernamePasswordRemoteAuthenticationSettings uras =
                    (UsernamePasswordRemoteAuthenticationSettings) ras;
                LOGGER.info( "... {} authentication setup for remote storage with username '{}'", authScope,
                    uras.getUsername() );
                credentials = new UsernamePasswordCredentials( uras.getUsername(), uras.getPassword() );
            }

            if ( credentials != null )
            {
                if ( proxyHost != null )
                {
                    httpClient.getCredentialsProvider().setCredentials( new AuthScope( proxyHost ), credentials );
                    httpClient.getParams().setParameter( AuthPNames.PROXY_AUTH_PREF, authorisationPreference );
                }
                else
                {
                    httpClient.getCredentialsProvider().setCredentials( AuthScope.ANY, credentials );
                    httpClient.getParams().setParameter( AuthPNames.TARGET_AUTH_PREF, authorisationPreference );
                }
            }
        }
    }

    private static void configureProxy( final DefaultHttpClient httpClient,
                                        final RemoteProxySettings remoteProxySettings )
    {
        if ( remoteProxySettings.isEnabled() )
        {
            LOGGER.info( "... proxy setup with host '{}'", remoteProxySettings.getHostname() );

            final HttpHost proxy = new HttpHost( remoteProxySettings.getHostname(), remoteProxySettings.getPort() );
            httpClient.getParams().setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );

            // check if we have non-proxy hosts
            if ( remoteProxySettings.getNonProxyHosts() != null && !remoteProxySettings.getNonProxyHosts().isEmpty() )
            {
                final Set<Pattern> nonProxyHostPatterns =
                    new HashSet<Pattern>( remoteProxySettings.getNonProxyHosts().size() );
                for ( String nonProxyHostRegex : remoteProxySettings.getNonProxyHosts() )
                {
                    try
                    {
                        nonProxyHostPatterns.add( Pattern.compile( nonProxyHostRegex, Pattern.CASE_INSENSITIVE ) );
                    }
                    catch ( PatternSyntaxException e )
                    {
                        LOGGER.warn( "Invalid non proxy host regex: {}", nonProxyHostRegex, e );
                    }
                }
                httpClient.setRoutePlanner( new NonProxyHostsAwareHttpRoutePlanner(
                    httpClient.getConnectionManager().getSchemeRegistry(), nonProxyHostPatterns ) );
            }

            configureAuthentication( httpClient, remoteProxySettings.getProxyAuthentication(), proxy );
        }
    }

    private static HttpParams createHttpParams( final String designator,
                                                final RemoteConnectionSettings remoteConnectionSettings )
    {
        HttpParams params = new SyncBasicHttpParams();
        params.setParameter( HttpProtocolParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1 );
        params.setBooleanParameter( HttpProtocolParams.USE_EXPECT_CONTINUE, false );
        params.setBooleanParameter( HttpConnectionParams.STALE_CONNECTION_CHECK, false );
        params.setIntParameter( HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024 );

        // pool timeout: max how long to wait for connection (without this, pool would block indefinitely)
        long poolTimeout = getLong( CONNECTION_POOL_TIMEOUT_SUFFIX, designator, CONNECTION_POOL_TIMEOUT_DEFAULT );
        params.setLongParameter( ClientPNames.CONN_MANAGER_TIMEOUT, poolTimeout );

        // connection and socket timeouts come from Nexus config
        // getting the timeout from RemoteStorageContext. The value we get depends on per-repo and global settings.
        // The value will "cascade" from repo level to global level, see implementation.
        params.setIntParameter( HttpConnectionParams.CONNECTION_TIMEOUT,
            remoteConnectionSettings.getConnectionTimeout() );
        params.setIntParameter( HttpConnectionParams.SO_TIMEOUT, remoteConnectionSettings.getConnectionTimeout() );
        return params;
    }

    private static PoolingClientConnectionManager createConnectionManager( final String designator )
        throws IllegalStateException
    {
        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register( new Scheme( "http", 80, PlainSocketFactory.getSocketFactory() ) );
        schemeRegistry.register( new Scheme( "https", 443, SSLSocketFactory.getSocketFactory() ) );
        final PoolingClientConnectionManager connManager = new PoolingClientConnectionManager( schemeRegistry );

        // pool size: we have a new (documented) key, and an old (deprecated, undocumented but used). If new present,
        // will be used, otherwise the deprecated will be looked up, and if not found, the default is used.
        // NOTE: pool size is per-repository, hence all of those will connect to same host (unless mirrors are used)
        // so, we are violating intentionally the RFC and we let the whole pool size to chase same host by setting
        // maxPerRoute to same value as maxTotal
        final int poolSize =
            getInt( CONNECTION_POOL_SIZE_SUFFIX, designator,
                SystemPropertiesHelper.getInteger( CONNECTION_POOL_SIZE_KEY_DEPRECATED, CONNECTION_POOL_SIZE_DEFAULT ) );
        connManager.setMaxTotal( poolSize );
        connManager.setDefaultMaxPerRoute( poolSize );
        return connManager;
    }

    static void evictConnectionManagerPool( final HttpClient client )
    {
        // this might be null as well
        final String designator = (String) client.getParams().getParameter( CLIENT_DESIGNATOR_KEY );
        evictConnectionManagerPool( designator, client.getConnectionManager() );
    }

    private static void evictConnectionManagerPool( final String designator, final ClientConnectionManager connManager )
    {
        // as default DefaultConnectionKeepAliveStrategy is used, and it obeys to remote peer (if it says anything at
        // all) we do eviction properly, using both methods. First call will evict those expired, after time
        // that remote peer told us. If remote peer did not say anything, they are pooled for indefinite time
        // and second call will handle them.
        // NOTE: Having the 1st call we are simply polite to the remote peers that explicitly state for how
        // long they want to keep-alive the connection, AND that value is less than our keep-alive
        connManager.closeExpiredConnections();
        // NOTE: Having the 2nd call we are simply "capping" the possible maximum time for how long to keep idle open
        // sockets. If you consider servers mentioned above (that state timeout for keep-alive), this also
        // protect us against abusive ones, that for example could say "keep socket open for one year" ;)
        final long keepAlive =
            getLong( CONNECTION_POOL_KEEPALIVE_SUFFIX, designator, CONNECTION_POOL_KEEPALIVE_DEFAULT );
        connManager.closeIdleConnections( keepAlive, TimeUnit.MILLISECONDS );
    }

    // parameter fetching

    @VisibleForTesting
    static int getInt( final String suffix, final String designator, final int defaultValue )
    {
        // prefix[id]suffix
        // nexus.apacheHttpClient4x.id.connectionPoolKeepalive
        // nexus.apacheHttpClient4x.connectionPoolKeepalive
        // default value
        if ( designator == null )
        {
            return SystemPropertiesHelper.getInteger( PARAMETER_PREFIX, suffix, null, defaultValue );
        }
        else
        {
            return SystemPropertiesHelper.getInteger( PARAMETER_PREFIX, suffix, designator + ".", defaultValue );
        }
    }

    @VisibleForTesting
    static long getLong( final String suffix, final String designator, final long defaultValue )
    {
        // prefix[id]suffix
        // nexus.apacheHttpClient4x.id.connectionPoolKeepalive
        // nexus.apacheHttpClient4x.connectionPoolKeepalive
        // default value
        if ( designator == null )
        {
            return SystemPropertiesHelper.getLong( PARAMETER_PREFIX, suffix, null, defaultValue );
        }
        else
        {
            return SystemPropertiesHelper.getLong( PARAMETER_PREFIX, suffix, designator + ".", defaultValue );
        }
    }

    // ==

    protected static String getHttpClientDescription( final HttpClient client )
    {
        final String id = String.valueOf( client.getParams().getParameter( CLIENT_ID_KEY ) );
        final String designator = (String) client.getParams().getParameter( CLIENT_DESIGNATOR_KEY );

        return "HttpClient4x ID=\"" + id + "\" (designator=\"" + designator + "\")";
    }

    protected static void logAction( final String action, final HttpClient client )
    {
        if ( !LOGGER.isDebugEnabled() )
        {
            return;
        }
        LOGGER.debug( "{} {}", action, getHttpClientDescription( client ) );
    }
}
