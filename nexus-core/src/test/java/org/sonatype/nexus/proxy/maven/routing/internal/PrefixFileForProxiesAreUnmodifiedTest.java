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
package org.sonatype.nexus.proxy.maven.routing.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.After;
import org.junit.Test;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.io.CharStreams;

public class PrefixFileForProxiesAreUnmodifiedTest
    extends AbstractRoutingProxyTest
{
    private static final String PROXY_REPO_ID = "proxy";

    private Server server;

    private EnvironmentBuilder environmentBuilder;

    public PrefixFileForProxiesAreUnmodifiedTest()
        throws Exception
    {
        // fluke server to not have proxy autoblock, as remote connection refused IS a valid reason to auto block
        this.server =
            Server.withPort( 0 ).serve( "/" ).withBehaviours( Behaviours.error( 404, "don't bother yourself" ) );
        server.start();
    }

    @After
    public void stopServers()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Override
    protected EnvironmentBuilder createEnvironmentBuilder()
        throws Exception
    {
        if ( environmentBuilder == null )
        {
            server =
                Server.withPort( 0 ).serve( "/.meta/prefixes.txt" ).withBehaviours(
                    Behaviours.content( prefixFile1( true ) ) );
            server.start();

            // we need one hosted repo only, so build it
            environmentBuilder = new EnvironmentBuilder()
            {
                @Override
                public void startService()
                {
                }

                @Override
                public void stopService()
                {
                }

                @Override
                public void buildEnvironment( AbstractProxyTestEnvironment env )
                    throws ConfigurationException, IOException, ComponentLookupException
                {
                    final PlexusContainer container = env.getPlexusContainer();
                    final List<String> reposes = new ArrayList<String>();
                    {
                        // adding one proxy
                        final M2Repository repo = (M2Repository) container.lookup( Repository.class, "maven2" );
                        CRepository repoConf = new DefaultCRepository();
                        repoConf.setProviderRole( Repository.class.getName() );
                        repoConf.setProviderHint( "maven2" );
                        repoConf.setId( PROXY_REPO_ID );
                        repoConf.setName( PROXY_REPO_ID );
                        repoConf.setNotFoundCacheActive( true );
                        repoConf.setLocalStorage( new CLocalStorage() );
                        repoConf.getLocalStorage().setProvider( "file" );
                        repoConf.getLocalStorage().setUrl(
                            env.getApplicationConfiguration().getWorkingDirectory( "proxy/store/" + PROXY_REPO_ID ).toURI().toURL().toString() );
                        Xpp3Dom ex = new Xpp3Dom( "externalConfiguration" );
                        repoConf.setExternalConfiguration( ex );
                        M2RepositoryConfiguration exConf = new M2RepositoryConfiguration( ex );
                        exConf.setRepositoryPolicy( RepositoryPolicy.RELEASE );
                        exConf.setChecksumPolicy( ChecksumPolicy.STRICT_IF_EXISTS );
                        repoConf.setRemoteStorage( new CRemoteStorage() );
                        repoConf.getRemoteStorage().setProvider(
                            env.getRemoteProviderHintFactory().getDefaultHttpRoleHint() );
                        repoConf.getRemoteStorage().setUrl( "http://localhost:" + server.getPort() + "/" );
                        repo.configure( repoConf );
                        // repo.setCacheManager( env.getCacheManager() );
                        reposes.add( repo.getId() );
                        env.getApplicationConfiguration().getConfigurationModel().addRepository( repoConf );
                        env.getRepositoryRegistry().addRepository( repo );
                    }
                }
            };
        }
        return environmentBuilder;
    }

    @Override
    protected boolean enableAutomaticRoutingFeature()
    {
        return true;
    }

    protected String prefixFile1( boolean withComments )
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter( sw );
        if ( withComments )
        {
            pw.println( "# This is mighty prefix file!" );
        }
        pw.println( "/org/apache/maven" );
        pw.println( "/org/sonatype" );
        if ( withComments )
        {
            pw.println( " # Added later" );
        }
        pw.println( "/eu/flatwhite" );
        return sw.toString();
    }

    @Test
    public void proxyPrefixFileIsUnchanged()
        throws Exception
    {
        // all is settled now, proxy should have prefix file pulled from remote
        final MavenProxyRepository proxyRepository =
            getRepositoryRegistry().getRepositoryWithFacet( PROXY_REPO_ID, MavenProxyRepository.class );

        final Manager routingManager = lookup( Manager.class );
        final PrefixSource proxyPrefixSource = routingManager.getPrefixSourceFor( proxyRepository );

        assertThat( "Prefix file for proxy repository should exists", proxyPrefixSource.exists() );
        assertThat( "Prefix file for proxy repository should be discovered", proxyPrefixSource.supported() );
        assertThat( "Prefix file should be instanceof FilePrefixSource", proxyPrefixSource instanceof FilePrefixSource );

        final FilePrefixSource filePrefixSource = (FilePrefixSource) proxyPrefixSource;
        final StorageFileItem fileItem = filePrefixSource.getFileItem();

        final String onDisk = CharStreams.toString( new InputStreamReader( fileItem.getInputStream() ) );

        assertThat( "The remote and local content must be equal", onDisk, equalTo( prefixFile1( true ) ) );
    }
}
