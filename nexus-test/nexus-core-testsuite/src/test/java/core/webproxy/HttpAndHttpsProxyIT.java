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
package core.webproxy;

import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.firstAvailableTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.systemTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.testParameters;
import static org.sonatype.sisu.goodies.common.Varargs.$;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.ServerConfiguration;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettingsDTO;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.sisu.bl.support.port.PortReservationService;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

/**
 * ITs related to which proxy is used (global or repository level defined ones), depending on url scheme (http / https).
 *
 * @since 2.5
 */
@NexusStartAndStopStrategy( NexusStartAndStopStrategy.Strategy.EACH_TEST )
public class HttpAndHttpsProxyIT
    extends NexusRunningParametrizedITSupport
{

    @Inject
    private PortReservationService portReservationService;

    private int globalHttpProxyPort;

    private int globalHttpsProxyPort;

    private ProxyServerWithHttpsTunneling globalHttpProxy;

    private ProxyServerWithHttpsTunneling globalHttpsProxy;

    private Server httpRemoteServer;

    private Server httpsRemoteServer;

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return firstAvailableTestParameters(
            systemTestParameters(),
            testParameters( $( "${it.nexus.bundle.groupId}:${it.nexus.bundle.artifactId}:zip:bundle" ) )
        ).load();
    }

    public HttpAndHttpsProxyIT( final String nexusBundleCoordinates )
    {
        super( nexusBundleCoordinates );
    }

    @Override
    protected NexusBundleConfiguration configureNexus( final NexusBundleConfiguration configuration )
    {
        return super.configureNexus( configuration )
            .setSystemProperty( "javax.net.ssl.trustStore", testData().resolveFile( "trustStore" ).getAbsolutePath() )
            .setSystemProperty( "javax.net.ssl.trustStorePassword", "changeit" );
    }

    @Before
    public void initWebProxiesAndRemoteServer()
        throws Exception
    {
        globalHttpProxy = new ProxyServerWithHttpsTunneling();
        globalHttpProxy.setPort( globalHttpProxyPort = portReservationService.reservePort() );
        globalHttpProxy.initialize();

        globalHttpsProxy = new ProxyServerWithHttpsTunneling();
        globalHttpsProxy.setPort( globalHttpsProxyPort = portReservationService.reservePort() );
        globalHttpsProxy.initialize();

        httpRemoteServer = Server
            .withPort( 0 )
            .serve( "/*" ).withBehaviours( Behaviours.get( testData().resolveFile( "remote-repo" ) ) )
            .start();

        httpsRemoteServer = Server
            .withPort( 0 )
            .withKeystore( "keystore", "password" )
            .serve( "/*" ).withBehaviours( Behaviours.get( testData().resolveFile( "remote-repo" ) ) )
            .start();
    }

    @After
    public void stopWebProxies()
        throws Exception
    {
        if ( globalHttpProxy != null )
        {
            globalHttpProxy.stop();
            portReservationService.cancelPort( globalHttpProxyPort );
        }
        if ( globalHttpsProxy != null )
        {
            globalHttpsProxy.stop();
            portReservationService.cancelPort( globalHttpsProxyPort );
        }
        if ( httpRemoteServer != null )
        {
            httpRemoteServer.stop();
        }
        if ( httpsRemoteServer != null )
        {
            httpsRemoteServer.stop();
        }
    }

    /**
     * Given:
     * - no global HTTP proxy
     * - no global HTTPS proxy
     * - no repo proxy
     * <p/>
     * Verify that no proxy is used for an HTTP url.
     */
    @Test
    public void httpUrl()
        throws Exception
    {
        disableGlobalHttpProxy();
        disableGlobalHttpsProxy();
        final MavenProxyRepository repository = createMavenProxyRepository( httpRemoteServer );
        downloadArtifact( repository.id() );
    }

    /**
     * Given:
     * - no global HTTP proxy
     * - no global HTTPS proxy
     * - no repo proxy
     * <p/>
     * Verify that no proxy is used for an HTTPS url.
     */
    @Test
    public void httpsUrl()
        throws Exception
    {
        disableGlobalHttpProxy();
        disableGlobalHttpsProxy();
        final MavenProxyRepository repository = createMavenProxyRepository( httpsRemoteServer );
        downloadArtifact( repository.id() );
    }

    /**
     * Given:
     * - global HTTP proxy
     * - no global HTTPS proxy
     * - no repo proxy
     * <p/>
     * Verify that global HTTP proxy is used for an HTTP url.
     */
    @Test
    public void gHttp_httpUrl()
        throws Exception
    {
        globalHttpProxy.start();
        enableGlobalHttpProxy();
        disableGlobalHttpsProxy();
        final MavenProxyRepository repository = createMavenProxyRepository( httpRemoteServer );
        downloadArtifact( repository.id() );
    }

    /**
     * Given:
     * - global HTTP proxy
     * - no global HTTPS proxy
     * - no repo proxy
     * <p/>
     * Verify that global HTTP proxy is used for an HTTPS url.
     */
    @Test
    public void gHttp_httpsUrl()
        throws Exception
    {
        globalHttpProxy.start();
        enableGlobalHttpProxy();
        disableGlobalHttpsProxy();
        final MavenProxyRepository repository = createMavenProxyRepository( httpsRemoteServer );
        downloadArtifact( repository.id() );
    }

    /**
     * Given:
     * - global HTTP proxy
     * - global HTTPS proxy
     * - no repo proxy
     * <p/>
     * Verify that global HTTP proxy is used for an HTTP url.
     */
    @Test
    public void gHttp_gHttps_httpUrl()
        throws Exception
    {
        globalHttpProxy.start();
        enableGlobalHttpProxy();
        enableGlobalHttpsProxy();
        final MavenProxyRepository repository = createMavenProxyRepository( httpRemoteServer );
        downloadArtifact( repository.id() );
    }

    /**
     * Given:
     * - global HTTP proxy
     * - global HTTPS proxy
     * - no repo proxy
     * <p/>
     * Verify that global HTTPS proxy is used for an HTTPS url.
     */
    @Test
    public void gHttp_gHttps_httpsUrl()
        throws Exception
    {
        globalHttpsProxy.start();
        enableGlobalHttpProxy();
        enableGlobalHttpsProxy();
        final MavenProxyRepository repository = createMavenProxyRepository( httpsRemoteServer );
        downloadArtifact( repository.id() );
    }

    private void enableGlobalHttpProxy()
        throws IOException
    {
        final RemoteProxySettingsDTO settings = config().remoteProxySettings().settings();

        settings.setHttpProxySettings( new RemoteHttpProxySettingsDTO() );
        settings.getHttpProxySettings().setProxyHostname( "localhost" );
        settings.getHttpProxySettings().setProxyPort( globalHttpProxy.getPort() );

        config().remoteProxySettings().save();
    }

    private void disableGlobalHttpProxy()
        throws IOException
    {
        config().remoteProxySettings().disableHttpProxy();
    }

    private void enableGlobalHttpsProxy()
        throws IOException
    {
        final RemoteProxySettingsDTO settings = config().remoteProxySettings().settings();

        settings.setHttpsProxySettings( new RemoteHttpProxySettingsDTO() );
        settings.getHttpsProxySettings().setProxyHostname( "localhost" );
        settings.getHttpsProxySettings().setProxyPort( globalHttpsProxy.getPort() );

        config().remoteProxySettings().save();
    }

    private void disableGlobalHttpsProxy()
        throws IOException
    {
        config().remoteProxySettings().disableHttpsProxy();
    }

    private void downloadArtifact( final String repositoryId )
        throws IOException
    {
        content().download(
            repositoryLocation( repositoryId, "com/someorg/artifact/1.0/artifact-1.0.pom" ),
            new File( testIndex().getDirectory( "downloads" ), "artifact-1.0.pom" )
        );
    }

    private MavenProxyRepository createMavenProxyRepository( final Server remoteServer )
        throws MalformedURLException
    {
        return repositories()
            .create( MavenProxyRepository.class, repositoryIdForTest() )
            .asProxyOf( remoteServer.getUrl().toExternalForm() )
            .save();
    }

    public ServerConfiguration config()
    {
        return client().getSubsystem( ServerConfiguration.class );
    }

    public Repositories repositories()
    {
        return client().getSubsystem( Repositories.class );
    }

    public Content content()
    {
        return client().getSubsystem( Content.class );
    }

}
