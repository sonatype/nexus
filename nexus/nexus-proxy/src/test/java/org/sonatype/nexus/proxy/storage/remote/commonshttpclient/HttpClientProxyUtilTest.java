package org.sonatype.nexus.proxy.storage.remote.commonshttpclient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;

public class HttpClientProxyUtilTest
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Test
    public void testHttpClientProxyUtilUseDoesNotModifyContext()
    {
        final RemoteConnectionSettings remoteConnectionSettings = new RemoteConnectionSettings()
        {
            @Override
            public void setUserAgentCustomizationString( String userAgentCustomizationString )
            {
            }

            @Override
            public void setRetrievalRetryCount( int retrievalRetryCount )
            {
            }

            @Override
            public void setQueryString( String queryString )
            {
            }

            @Override
            public void setConnectionTimeout( int connectionTimeout )
            {
            }

            @Override
            public String getUserAgentCustomizationString()
            {
                return null;
            }

            @Override
            public int getRetrievalRetryCount()
            {
                return 3;
            }

            @Override
            public String getQueryString()
            {
                return null;
            }

            @Override
            public int getConnectionTimeout()
            {
                return 12000;
            }
        };
        final RemoteProxySettings remoteProxySettings = new RemoteProxySettings()
        {
            @Override
            public void setProxyAuthentication( RemoteAuthenticationSettings proxyAuthentication )
            {
            }

            @Override
            public void setPort( int port )
            {
            }

            @Override
            public void setNonProxyHosts( Set<String> nonProxyHosts )
            {
            }

            @Override
            public void setHostname( String hostname )
            {
            }

            @Override
            public void setBlockInheritance( boolean val )
            {
            }

            @Override
            public boolean isEnabled()
            {
                return false;
            }

            @Override
            public boolean isBlockInheritance()
            {
                return false;
            }

            @Override
            public RemoteAuthenticationSettings getProxyAuthentication()
            {
                return null;
            }

            @Override
            public int getPort()
            {
                return 0;
            }

            @Override
            public Set<String> getNonProxyHosts()
            {
                return null;
            }

            @Override
            public String getHostname()
            {
                return null;
            }
        };
        final DefaultRemoteStorageContext ctx = new DefaultRemoteStorageContext( null );
        ctx.setRemoteConnectionSettings( remoteConnectionSettings );
        ctx.setRemoteProxySettings( remoteProxySettings );

        final HttpClient httpClient = new HttpClient();

        // get last changed
        long lastChanged = ctx.getLastChanged();

        // 1st invocation, should modify it
        HttpClientProxyUtil.applyProxyToHttpClient( httpClient, ctx, logger );
        assertThat( ctx.getLastChanged(), greaterThan( lastChanged ) );

        // now 2nd invocation, should not change
        lastChanged = ctx.getLastChanged();
        HttpClientProxyUtil.applyProxyToHttpClient( httpClient, ctx, logger );
        assertThat( ctx.getLastChanged(), equalTo( lastChanged ) );
    }

}
