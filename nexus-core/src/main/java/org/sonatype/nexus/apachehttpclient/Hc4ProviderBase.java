/*
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
package org.sonatype.nexus.apachehttpclient;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.repository.ClientSSLRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.NtlmRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import com.google.common.base.Preconditions;

/**
 * Support class for implementation of {@link Hc4Provider}.
 *
 * @author cstamas
 * @since 2.2
 */
@Singleton
@Named
public class Hc4ProviderBase
    extends AbstractLoggingComponent
{

    /**
     * Key for customizing default (and max) keep alive duration when remote server does not state anything, or states
     * some unreal high value. Value is milliseconds.
     */
    private static final String KEEP_ALIVE_MAX_DURATION_KEY = "nexus.apacheHttpClient4x.keepAliveMaxDuration";

    /**
     * Default keep alive max duration: 30 seconds.
     */
    private static final long KEEP_ALIVE_MAX_DURATION_DEFAULT = TimeUnit.SECONDS.toMillis( 30 );

    /**
     * UA builder component.
     */
    private final UserAgentBuilder userAgentBuilder;

    /**
     * @param userAgentBuilder UA builder component, must not be {@code null}.
     */
    @Inject
    public Hc4ProviderBase( final UserAgentBuilder userAgentBuilder )
    {
        this.userAgentBuilder = Preconditions.checkNotNull( userAgentBuilder );
    }

    // configuration

    // ==

    public DefaultHttpClient createHttpClient( final RemoteStorageContext context,
                                               final ClientConnectionManager clientConnectionManager )
    {
        final DefaultHttpClient httpClient = new DefaultHttpClientImpl(
            clientConnectionManager, createHttpParams( context )
        );
        configureAuthentication( httpClient, context.getRemoteAuthenticationSettings(), null );
        configureProxy( httpClient, context.getRemoteProxySettings() );
        // obey the given retries count and apply it to client.
        final int retries =
            context.getRemoteConnectionSettings() != null
                ? context.getRemoteConnectionSettings().getRetrievalRetryCount()
                : 0;
        httpClient.setHttpRequestRetryHandler( new StandardHttpRequestRetryHandler( retries, false ) );
        httpClient.setKeepAliveStrategy( new NexusConnectionKeepAliveStrategy( getKeepAliveMaxDuration() ) );
        return httpClient;
    }

    protected HttpParams createHttpParams( final RemoteStorageContext context )
    {
        HttpParams params = new SyncBasicHttpParams();
        params.setParameter( HttpProtocolParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1 );
        params.setBooleanParameter( HttpProtocolParams.USE_EXPECT_CONTINUE, false );
        params.setBooleanParameter( HttpConnectionParams.STALE_CONNECTION_CHECK, false );
        params.setIntParameter( HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024 );
        params.setIntParameter( HttpConnectionParams.CONNECTION_TIMEOUT, getConnectionTimeout( context ) );
        params.setIntParameter( HttpConnectionParams.SO_TIMEOUT, getSoTimeout( context ) );
        params.setParameter( HttpProtocolParams.USER_AGENT, userAgentBuilder.formatGenericUserAgentString() );
        return params;
    }

    /**
     * Returns the maximum Keep-Alive duration in milliseconds.
     */
    protected long getKeepAliveMaxDuration()
    {
        return SystemPropertiesHelper.getLong( KEEP_ALIVE_MAX_DURATION_KEY, KEEP_ALIVE_MAX_DURATION_DEFAULT );
    }

    /**
     * Returns the connection timeout in milliseconds. The timeout until connection is established.
     */
    protected int getConnectionTimeout( final RemoteStorageContext context )
    {
        if ( context.getRemoteConnectionSettings() != null )
        {
            return context.getRemoteConnectionSettings().getConnectionTimeout();
        }
        else
        {
            // see DefaultRemoteConnectionSetting
            return 1000;
        }
    }

    /**
     * Returns the SO_SOCKET timeout in milliseconds. The timeout for waiting for data on established connection.
     */
    protected int getSoTimeout( final RemoteStorageContext context )
    {
        // this parameter is actually set from #getConnectionTimeout
        return getConnectionTimeout( context );
    }

    // ==

    protected void configureAuthentication( final DefaultHttpClient httpClient,
                                            final RemoteAuthenticationSettings ras,
                                            final HttpHost proxyHost )
    {
        if ( ras != null )
        {
            String authScope = "target";
            if ( proxyHost != null )
            {
                authScope = proxyHost.toHostString() + " proxy";
            }

            List<String> authorisationPreference = Lists.newArrayListWithExpectedSize(3);
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
                getLogger().debug("{} authentication setup for NTLM domain '{}'", authScope, nras.getNtlmDomain());
                credentials = new NTCredentials(
                    nras.getUsername(), nras.getPassword(), nras.getNtlmHost(), nras.getNtlmDomain()
                );
            }
            else if ( ras instanceof UsernamePasswordRemoteAuthenticationSettings )
            {
                final UsernamePasswordRemoteAuthenticationSettings uras =
                    (UsernamePasswordRemoteAuthenticationSettings) ras;
                getLogger().debug("{} authentication setup for remote storage with username '{}'", authScope,
                    uras.getUsername());
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

    protected void configureProxy( final DefaultHttpClient httpClient, final RemoteProxySettings remoteProxySettings )
    {
        if ( remoteProxySettings.isEnabled() )
        {
            getLogger().debug("proxy setup with host '{}'", remoteProxySettings.getHostname());

            final HttpHost proxy = new HttpHost( remoteProxySettings.getHostname(), remoteProxySettings.getPort() );
            httpClient.getParams().setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );

            // check if we have non-proxy hosts
            if ( remoteProxySettings.getNonProxyHosts() != null && !remoteProxySettings.getNonProxyHosts().isEmpty() )
            {
                final Set<Pattern> nonProxyHostPatterns = Sets.newHashSetWithExpectedSize(
                    remoteProxySettings.getNonProxyHosts().size()
                );
                for ( String nonProxyHostRegex : remoteProxySettings.getNonProxyHosts() )
                {
                    try
                    {
                        nonProxyHostPatterns.add( Pattern.compile( nonProxyHostRegex, Pattern.CASE_INSENSITIVE ) );
                    }
                    catch ( PatternSyntaxException e )
                    {
                        getLogger().warn( "Invalid non proxy host regex: {}", nonProxyHostRegex, e );
                    }
                }
                httpClient.setRoutePlanner(
                    new NonProxyHostsAwareHttpRoutePlanner(
                        httpClient.getConnectionManager().getSchemeRegistry(), nonProxyHostPatterns
                    )
                );
            }

            configureAuthentication( httpClient, remoteProxySettings.getProxyAuthentication(), proxy );
        }
    }

    /**
     * Sub-classed here to customize the http processor and to keep a sane logger name.
     */
    private static class DefaultHttpClientImpl
        extends DefaultHttpClient
    {

        private DefaultHttpClientImpl( final ClientConnectionManager conman, final HttpParams params )
        {
            super( conman, params );
        }

        @Override
        protected BasicHttpProcessor createHttpProcessor()
        {
            final BasicHttpProcessor result = super.createHttpProcessor();
            result.addResponseInterceptor( new ResponseContentEncoding() );
            return result;
        }
    }

}
