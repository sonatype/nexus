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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

/**
 *
 */
public class HttpClientUtilTest
    extends TestSupport
{

    private DefaultHttpClient underTest;

    @Mock
    private RemoteStorageContext ctx;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    @Mock
    private HttpContext httpContext;

    @Mock
    private StatusLine statusLine;

    @Mock
    private RequestLine requestLine;

    @Mock
    private RemoteConnectionSettings remoteConnectionSettings;

    @Mock
    private RemoteProxySettings remoteProxySettings;

    @Mock
    private ProxyRepository proxyRepository;

    @Before
    public void before()
    {
        when( ctx.getRemoteConnectionSettings() ).thenReturn( remoteConnectionSettings );
        when( ctx.getRemoteProxySettings() ).thenReturn( remoteProxySettings );
        when( proxyRepository.getId() ).thenReturn( "central" );
        underTest = (DefaultHttpClient) HttpClientUtil.configure( proxyRepository, "ctx", ctx, logger );

        when( response.getStatusLine() ).thenReturn( statusLine );

        when( request.getRequestLine() ).thenReturn( requestLine );
    }

    @Test
    public void doNotFollowRedirectsToDirIndex()
        throws ProtocolException
    {
        when( requestLine.getMethod() ).thenReturn( "GET" );

        final RedirectStrategy redirectStrategy = underTest.getRedirectStrategy();

        // no location header
        assertThat( redirectStrategy.isRedirected( request, response, httpContext ), is( false ) );

        when( statusLine.getStatusCode() ).thenReturn( HttpStatus.SC_MOVED_TEMPORARILY );

        // redirect to file
        when( response.getFirstHeader( "location" ) ).thenReturn(
            new BasicHeader( "location", "http://localhost/dir/file" ) );
        assertThat( redirectStrategy.isRedirected( request, response, httpContext ), is( true ) );

        // redirect to dir
        when( response.getFirstHeader( "location" ) ).thenReturn( new BasicHeader( "location", "http://localhost/dir/" ) );
        assertThat( redirectStrategy.isRedirected( request, response, httpContext ), is( false ) );
    }

    private void useConnection( final PoolingClientConnectionManager connMgr, final HttpRoute route,
                                final boolean shouldBeEvicted )
        throws Exception
    {
        // ask for a connection
        final ManagedClientConnection connection = connMgr.requestConnection( route, null ).getConnection( 0, null );
        // mark it reusable (in HC, this would come from higher level, based on HTTP protocol and other
        connection.markReusable();
        // check some stats
        assertThat( connMgr.getTotalStats().getAvailable(), equalTo( 0 ) );
        assertThat( connMgr.getTotalStats().getLeased(), equalTo( 1 ) );
        // release the connection, and keep it forever (we test will our monitor thread evict it)
        connMgr.releaseConnection( connection, -1, TimeUnit.MILLISECONDS );
        // check some stats
        assertThat( connMgr.getTotalStats().getAvailable(), equalTo( 1 ) );
        assertThat( connMgr.getTotalStats().getLeased(), equalTo( 0 ) );
        // sleep 5.5 second to give eviction change to kick in (runs every 5 sec)
        Thread.sleep( 5500 );
        // check some stats
        if ( shouldBeEvicted )
        {
            assertThat( connMgr.getTotalStats().getAvailable(), equalTo( 0 ) );
        }
        else
        {
            assertThat( connMgr.getTotalStats().getAvailable(), equalTo( 1 ) );
        }
        assertThat( connMgr.getTotalStats().getLeased(), equalTo( 0 ) );
    }

    @Test
    public void testKeepAlive()
        throws Exception
    {
        final PoolingClientConnectionManager connMgr =
            (PoolingClientConnectionManager) underTest.getConnectionManager();

        // the server
        final ServerSocket ss = new ServerSocket();
        final HttpRoute route = new HttpRoute( new HttpHost( "localhost", ss.getLocalPort() ) );
        final String keepaliveKey = HttpClientUtil.PARAMETER_PREFIX + HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX;

        try
        {
            // set pool timeout to 1 second, eviction happens per every 5 seconds so it should be evicted
            System.setProperty( keepaliveKey, String.valueOf( TimeUnit.SECONDS.toMillis( 1 ) ) );
            // this setting above will make the connection to be evicted

            useConnection( connMgr, route, true );

            // set pool timeout to 1 hour, eviction happens per every 5 seconds so it should not be evicted
            System.setProperty( keepaliveKey, String.valueOf( TimeUnit.HOURS.toMillis( 1 ) ) );

            useConnection( connMgr, route, false );
        }
        finally
        {
            System.clearProperty( keepaliveKey );
            ss.close();
        }
    }

    @Test
    public void testGetInt()
    {
        final int defaultValue = 1;

        // nexus.apacheHttpClient4x.central.connectionPoolKeepalive
        final String designatedKey =
            HttpClientUtil.PARAMETER_PREFIX + "central." + HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX;
        final int designatedValue = 3;
        // nexus.apacheHttpClient4x.connectionPoolKeepalive
        final String globalKey = HttpClientUtil.PARAMETER_PREFIX + HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX;
        final int globalValue = 5;

        //
        System.clearProperty( globalKey );
        System.clearProperty( designatedKey );

        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, null, defaultValue ),
            equalTo( defaultValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "foo", defaultValue ),
            equalTo( defaultValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "central", defaultValue ),
            equalTo( defaultValue ) );

        //
        System.setProperty( globalKey, String.valueOf( globalValue ) );
        System.clearProperty( designatedKey );

        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, null, defaultValue ),
            equalTo( globalValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "foo", defaultValue ),
            equalTo( globalValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "central", defaultValue ),
            equalTo( globalValue ) );

        //
        System.setProperty( globalKey, String.valueOf( globalValue ) );
        System.setProperty( designatedKey, String.valueOf( designatedValue ) );

        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, null, defaultValue ),
            equalTo( globalValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "foo", defaultValue ),
            equalTo( globalValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "central", defaultValue ),
            equalTo( designatedValue ) );

        //
        System.clearProperty( globalKey );
        System.setProperty( designatedKey, String.valueOf( designatedValue ) );

        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, null, defaultValue ),
            equalTo( defaultValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "foo", defaultValue ),
            equalTo( defaultValue ) );
        assertThat( HttpClientUtil.getInt( HttpClientUtil.CONNECTION_POOL_KEEPALIVE_SUFFIX, "central", defaultValue ),
            equalTo( designatedValue ) );

    }
}
