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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.jetty.util.B64Code;
import org.junit.After;
import org.junit.Test;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;
import org.sonatype.tests.http.server.jetty.behaviour.Record;

/**
 * Test that verifies that Nexus is getting an artifact from auth protected repository by single one HTTP GET request.
 * In other words, that repository transport uses preemptive authentication (and not doubles rountrips with 401, 200
 * responses).
 * 
 * @author cstamas
 */
public class PreemptiveAuthTest
    extends AbstractProxyTestEnvironment
{
    private static final String PROXY1_REPO_ID = "proxy1";

    private static final String USERNAME = "theuser";

    private static final String PASSWORD = "secret";

    private EnvironmentBuilder environmentBuilder;

    private Record record;

    private Server server1;

    @After
    public void stopServers()
        throws Exception
    {
        if ( server1 != null )
        {
            server1.stop();
        }
    }

    @Override
    protected EnvironmentBuilder getEnvironmentBuilder()
        throws Exception
    {
        if ( environmentBuilder == null )
        {
            // set up and start protected server as we will need it's port below to set up proxy
            // basic auth protected repo1 and a recorder
            record = new Record();
            server1 =
                Server.withPort( 0 ).serve( "/*" ).withBehaviours( record, new BasicAuth( USERNAME, PASSWORD ),
                    Behaviours.get( getTestFile( "target/test-classes/repo1" ) ) ).start();

            // we need one proxy repo only, so build it
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
                    {
                        // adding one proxy
                        final M2Repository repo = (M2Repository) container.lookup( Repository.class, "maven2" );
                        CRepository repoConf = new DefaultCRepository();
                        repoConf.setProviderRole( Repository.class.getName() );
                        repoConf.setProviderHint( "maven2" );
                        repoConf.setId( PROXY1_REPO_ID );
                        repoConf.setName( PROXY1_REPO_ID );
                        repoConf.setNotFoundCacheActive( true );
                        repoConf.setLocalStorage( new CLocalStorage() );
                        repoConf.getLocalStorage().setProvider( "file" );
                        repoConf.getLocalStorage().setUrl(
                            env.getApplicationConfiguration().getWorkingDirectory( "proxy/store/" + PROXY1_REPO_ID ).toURI().toURL().toString() );
                        Xpp3Dom ex = new Xpp3Dom( "externalConfiguration" );
                        repoConf.setExternalConfiguration( ex );
                        M2RepositoryConfiguration exConf = new M2RepositoryConfiguration( ex );
                        exConf.setRepositoryPolicy( RepositoryPolicy.RELEASE );
                        // checksum policy is IGNORE to not have checksum requests implicitly happen
                        exConf.setChecksumPolicy( ChecksumPolicy.IGNORE );
                        repoConf.setRemoteStorage( new CRemoteStorage() );
                        repoConf.getRemoteStorage().setProvider(
                            env.getRemoteProviderHintFactory().getDefaultHttpRoleHint() );
                        repoConf.getRemoteStorage().setUrl( "http://localhost:" + server1.getPort() + "/" );
                        repoConf.getRemoteStorage().setAuthentication( new CRemoteAuthentication() );
                        repoConf.getRemoteStorage().getAuthentication().setUsername( USERNAME );
                        repoConf.getRemoteStorage().getAuthentication().setPassword( PASSWORD );
                        repo.configure( repoConf );

                        env.getApplicationConfiguration().getConfigurationModel().addRepository( repoConf );
                        env.getRepositoryRegistry().addRepository( repo );
                    }
                }
            };
        }
        return environmentBuilder;
    }

    // ==

    @Test
    public void fetchOneFileAndCountRemoteAccess()
        throws Exception
    {
        // this is proxy we created above, with IGNORE checksum policy, to not have checksum requests interfere with
        // main requests (those we do)
        final MavenProxyRepository proxy =
            getRepositoryRegistry().getRepositoryWithFacet( PROXY1_REPO_ID, MavenProxyRepository.class );

        final String EXISTING_PATH = "/activemq/activemq-core/1.2/activemq-core-1.2.jar";

        // we don't care about item really, we want to count needed requests to get it
        final StorageItem item = proxy.retrieveItem( new ResourceStoreRequest( EXISTING_PATH ) );

        final List<String> requests = record.getRequests();
        assertThat( "To HTTP GET one item from protected repository you'd need one preemptively authed GET", requests,
            hasSize( 1 ) );
        assertThat( "The item we asked should result as HTTP request on remote server", requests.get( 1 ),
            containsString( EXISTING_PATH ) );
    }

    // == Below is a fixed copy-paste of BasicAuth behaviour from server-provider 0.6
    // changes:
    // 1 - removed failed counter
    // 2 - on failed auth, this behavior sends proper challenge too, not the 401 code only

    public static class BasicAuth
        implements Behaviour
    {
        private final String password;

        private final String user;

        public BasicAuth( String user, String password )
        {
            this.user = checkNotNull( user );
            this.password = checkNotNull( password );
        }

        public boolean execute( final HttpServletRequest request, final HttpServletResponse response,
                                final Map<Object, Object> ctx )
            throws Exception
        {
            final String userPass = new String( B64Code.encode( ( user + ":" + password ).getBytes( "UTF-8" ) ) );
            if ( ( "Basic " + userPass ).equals( request.getHeader( "Authorization" ) ) )
            {
                return true;
            }

            response.addHeader( "WWW-Authenticate", "Basic realm=\"Test\"" );
            response.sendError( 401, "not authorized" );
            return false;
        }
    }
}
