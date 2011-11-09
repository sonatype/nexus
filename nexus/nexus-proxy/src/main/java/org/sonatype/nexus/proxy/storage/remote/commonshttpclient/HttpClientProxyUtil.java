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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.httpclient.CustomMultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.nexus.proxy.repository.ClientSSLRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.NtlmRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.util.SystemPropertiesHelper;

public class HttpClientProxyUtil
{
    public static final String CONNECTION_POOL_SIZE_KEY = "httpClient.connectionPoolSize";

    public static final String NTLM_IS_IN_USE_KEY = "httpClient.ntlmIsInUse";

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger( HttpClientProxyUtil.class );

    public static void applyProxyToHttpClient( HttpClient httpClient, RemoteStorageContext ctx, Logger logger )
    {
        httpClient.setHttpConnectionManager( new CustomMultiThreadedHttpConnectionManager() );

        // getting the timeout from RemoteStorageContext. The value we get depends on per-repo and global settings.
        // The value will "cascade" from repo level to global level, see imple of it.
        int timeout = ctx.getRemoteConnectionSettings().getConnectionTimeout();

        // getting the connection pool size, using a little trick to allow us "backdoor" to tune it using system
        // properties, but defaulting it to the same we had before (httpClient defaults)
        int connectionPoolSize =
            SystemPropertiesHelper.getInteger( CONNECTION_POOL_SIZE_KEY,
                MultiThreadedHttpConnectionManager.DEFAULT_MAX_TOTAL_CONNECTIONS );

        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( timeout );
        httpClient.getHttpConnectionManager().getParams().setSoTimeout( timeout );
        //httpClient.getHttpConnectionManager().getParams().setTcpNoDelay( true );
        httpClient.getHttpConnectionManager().getParams().setMaxTotalConnections( connectionPoolSize );
        // NOTE: connPool is _per_ repo, hence all of those will connect to same host (unless mirrors are used)
        // so, we are violating intentionally the RFC and we let the whole pool size to chase same host
        httpClient.getHttpConnectionManager().getParams().setMaxConnectionsPerHost(
            HostConfiguration.ANY_HOST_CONFIGURATION, connectionPoolSize );

        // Setting auth if needed
        HostConfiguration httpConfiguration = httpClient.getHostConfiguration();

        // BASIC and DIGEST auth only
        RemoteAuthenticationSettings ras = ctx.getRemoteAuthenticationSettings();

        boolean isSimpleAuthUsed = false;
        boolean isNtlmUsed = false;

        if ( ras != null )
        {
            List<String> authPrefs = new ArrayList<String>( 2 );
            authPrefs.add( AuthPolicy.DIGEST );
            authPrefs.add( AuthPolicy.BASIC );

            if ( ras instanceof ClientSSLRemoteAuthenticationSettings )
            {
                // ClientSSLRemoteAuthenticationSettings cras = (ClientSSLRemoteAuthenticationSettings) ras;

                // TODO - implement this
            }
            else if ( ras instanceof NtlmRemoteAuthenticationSettings )
            {
                NtlmRemoteAuthenticationSettings nras = (NtlmRemoteAuthenticationSettings) ras;

                // Using NTLM auth, adding it as first in policies
                authPrefs.add( 0, AuthPolicy.NTLM );

                log( Level.INFO, "... authentication setup for NTLM domain \"" + nras.getNtlmDomain() + "\"", logger );

                httpConfiguration.setHost( nras.getNtlmHost() );

                httpClient.getState().setCredentials(
                    AuthScope.ANY,
                    new NTCredentials( nras.getUsername(), nras.getPassword(), nras.getNtlmHost(), nras.getNtlmDomain() ) );

                isNtlmUsed = true;
            }
            else if ( ras instanceof UsernamePasswordRemoteAuthenticationSettings )
            {
                UsernamePasswordRemoteAuthenticationSettings uras = (UsernamePasswordRemoteAuthenticationSettings) ras;

                // Using Username/Pwd auth, will not add NTLM
                log( Level.INFO, "... authentication setup for remote storage with username \"" + uras.getUsername()
                    + "\"", logger );

                httpClient.getState().setCredentials( AuthScope.ANY,
                    new UsernamePasswordCredentials( uras.getUsername(), uras.getPassword() ) );

                isSimpleAuthUsed = true;
            }

            httpClient.getParams().setParameter( AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs );
        }

        RemoteProxySettings rps = ctx.getRemoteProxySettings();

        boolean isProxyUsed = false;

        if ( rps.isEnabled() )
        {
            isProxyUsed = true;

            log( Level.INFO, "... proxy setup with host \"" + rps.getHostname() + "\"", logger );

            httpConfiguration.setProxy( rps.getHostname(), rps.getPort() );

            // check if we have non-proxy hosts
            if ( rps.getNonProxyHosts() != null && !rps.getNonProxyHosts().isEmpty() )
            {
                Set<Pattern> nonProxyHostPatterns = new HashSet<Pattern>( rps.getNonProxyHosts().size() );
                for ( String nonProxyHostRegex : rps.getNonProxyHosts() )
                {
                    try
                    {
                        nonProxyHostPatterns.add( Pattern.compile( nonProxyHostRegex, Pattern.CASE_INSENSITIVE ) );
                    }
                    catch ( PatternSyntaxException e )
                    {
                        LOG.warn( "Invalid non proxy host regex: " + nonProxyHostRegex, e );
                    }
                }
                httpConfiguration.getParams().setParameter(
                    CustomMultiThreadedHttpConnectionManager.NON_PROXY_HOSTS_PATTERNS_KEY, nonProxyHostPatterns );
            }

            if ( rps.getProxyAuthentication() != null )
            {
                ras = rps.getProxyAuthentication();

                List<String> authPrefs = new ArrayList<String>( 2 );
                authPrefs.add( AuthPolicy.DIGEST );
                authPrefs.add( AuthPolicy.BASIC );

                if ( ras instanceof ClientSSLRemoteAuthenticationSettings )
                {
                    // ClientSSLRemoteAuthenticationSettings cras = (ClientSSLRemoteAuthenticationSettings) ras;

                    // TODO - implement this
                }
                else if ( ras instanceof NtlmRemoteAuthenticationSettings )
                {
                    NtlmRemoteAuthenticationSettings nras = (NtlmRemoteAuthenticationSettings) ras;

                    // Using NTLM auth, adding it as first in policies
                    authPrefs.add( 0, AuthPolicy.NTLM );

                    if ( ctx.getRemoteAuthenticationSettings() != null
                        && ( ctx.getRemoteAuthenticationSettings() instanceof NtlmRemoteAuthenticationSettings ) )
                    {
                        log( Level.WARNING, "... Apache Commons HttpClient 3.x is unable to use NTLM auth scheme\n"
                            + " for BOTH server side and proxy side authentication!\n"
                            + " You MUST reconfigure server side auth and use BASIC/DIGEST scheme\n"
                            + " if you have to use NTLM proxy, otherwise it will not work!\n"
                            + " *** SERVER SIDE AUTH OVERRIDDEN", logger );
                    }

                    log( Level.WARNING, "... proxy authentication setup for NTLM domain \"" + nras.getNtlmDomain()
                        + "\"", logger );

                    httpConfiguration.setHost( nras.getNtlmHost() );

                    httpClient.getState().setProxyCredentials(
                        AuthScope.ANY,
                        new NTCredentials( nras.getUsername(), nras.getPassword(), nras.getNtlmHost(),
                            nras.getNtlmDomain() ) );

                    isNtlmUsed = true;
                }
                else if ( ras instanceof UsernamePasswordRemoteAuthenticationSettings )
                {
                    UsernamePasswordRemoteAuthenticationSettings uras =
                        (UsernamePasswordRemoteAuthenticationSettings) ras;

                    // Using Username/Pwd auth, will not add NTLM
                    log( Level.INFO,
                        "... proxy authentication setup for remote storage with username \"" + uras.getUsername()
                            + "\"", logger );

                    httpClient.getState().setProxyCredentials( AuthScope.ANY,
                        new UsernamePasswordCredentials( uras.getUsername(), uras.getPassword() ) );
                }

                httpClient.getParams().setParameter( AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs );
            }
        }

        // set preemptive only for simplest scenario:
        // no proxy and BASIC auth is used
        if ( isSimpleAuthUsed && !isProxyUsed )
        {
            log( Level.INFO, "... simple scenario: simple authentication used with no proxy in between target and us, will use preemptive authentication", logger );
            
            // we have authentication, let's do it preemptive
            httpClient.getParams().setAuthenticationPreemptive( true );
        }

        // mark the fact that NTLM is in use
        if ( isNtlmUsed )
        {
            ctx.putContextObject( NTLM_IS_IN_USE_KEY, Boolean.TRUE );
        }
        else
        {
            ctx.putContextObject( NTLM_IS_IN_USE_KEY, Boolean.FALSE );
        }
    }

    /**
     * Coding around plexus logger as this class is NOT a component and should not be using this type of logging.
     * 
     * @param level
     * @param message
     * @param logger
     */
    private static void log( Level level, String message, Logger logger )
    {
        if ( logger != null )
        {
            if ( level.equals( Level.SEVERE ) )
            {
                logger.error( message );
            }
            else if ( level.equals( Level.WARNING ) )
            {
                logger.warn( message );
            }
            else if ( level.equals( Level.INFO ) )
            {
                logger.info( message );
            }
            else
            {
                logger.debug( message );
            }
        }
        else
        {
            if ( level.equals( Level.SEVERE ) )
            {
                LOG.error( message );
            }
            else if ( level.equals( Level.WARNING ) )
            {
                LOG.warn( message );
            }
            else if ( level.equals( Level.INFO ) )
            {
                LOG.info( message );
            }
            else
            {
                LOG.debug( message );
            }
        }

    }

}
