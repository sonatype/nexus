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
package org.sonatype.nexus.plugin;

import static java.lang.String.format;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.plugin.discovery.NexusConnectionInfo;
import org.sonatype.nexus.plugin.discovery.NexusDiscoveryException;
import org.sonatype.nexus.plugin.discovery.NexusInstanceDiscoverer;
import org.sonatype.nexus.plugin.useragent.Maven2UserAgentExtractor;
import org.sonatype.nexus.plugin.useragent.SettingsUserAgentExtractor;
import org.sonatype.nexus.restlight.common.AbstractRESTLightClient;
import org.sonatype.nexus.restlight.common.RESTLightClientException;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public abstract class AbstractNexusMojo
    extends AbstractMojo
    implements Contextualizable
{

    /**
     * NOT REQUIRED IN ALL CASES. If this is available, the current project will be used in the Nexus discovery process.
     *
     * @parameter default-value="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * If false, the Nexus discovery process will prompt the user to accept any Nexus connection information it finds
     * before using it. <br/>
     * <b>NOTE:</b> Batch-mode executions will override this parameter with an effective value of 'true'.
     *
     * @parameter expression="${nexus.automaticDiscovery}" default-value="false"
     */
    private boolean automatic;

    /**
     * The base URL for a Nexus Professional instance that includes the nexus-staging-plugin. If missing, the mojo will
     * prompt for this value.
     *
     * @parameter expression="${nexus.url}"
     */
    private String nexusUrl;

    /**
     * @component
     */
    private Prompter prompter;

    /**
     * The username that should be used to log into Nexus.
     *
     * @parameter expression="${nexus.username}" default-value="${user.name}"
     */
    private String username;

    /**
     * If provided, lookup username/password from this server entry in the current Maven settings.
     *
     * @parameter expression="${nexus.serverAuthId}"
     */
    private String serverAuthId;

    /**
     * If provided, lookup user agent from this server entry in the current Maven settings.
     *
     * @parameter expression="${nexus.serverUserAgent}"
     */
    private String serverUserAgent;

    /**
     * The password that should be used to log into Nexus. If missing, the mojo will prompt for this value.
     *
     * @parameter expression="${nexus.password}"
     */
    private String password;

    /**
     * @parameter default-value="${settings}"
     * @readonly
     */
    private Settings settings;

    /**
     * @parameter default-value="${session}"
     * @readonly
     */
    private MavenSession session;

    // proxy settings derived only from active Maven settings proxy
    private String proxyHost;

    private int proxyPort = -1;

    private String proxyUsername;

    private String proxyPassword;

    private NexusInstanceDiscoverer discoverer;

    private PlexusContainer container;

    // ==

    public final void execute()
        throws MojoExecutionException
    {
        fillMissing();

        doExecute();
    }

    protected abstract void doExecute()
        throws MojoExecutionException;

    // ==

    @Override
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );

        try
        {
            Object factory = LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger logger = null;
            if ( factory instanceof LoggerContext )
            {
                logger =
                    ( (LoggerContext) factory ).getLogger( ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME );
            }

            if ( logger != null )
            {
                LoggerManager loggerManager = (LoggerManager) container.lookup( LoggerManager.class.getName() );

                final int threshold = loggerManager.getThreshold();

                if ( org.codehaus.plexus.logging.Logger.LEVEL_DEBUG == threshold )
                {
                    logger.setLevel( Level.DEBUG );
                }
                else
                {
                    logger.setLevel( Level.INFO );
                }
            }

            discoverer = (NexusInstanceDiscoverer) container.lookup( NexusInstanceDiscoverer.class.getName() );
        }
        catch ( ComponentLookupException e )
        {
            throw new ContextException( "Cannot lookup discoverer!", e );
        }
    }

    // ==

    public String getNexusUrl()
    {
        return nexusUrl;
    }

    public void setNexusUrl( final String nexusUrl )
    {
        this.nexusUrl = nexusUrl;
    }

    public Prompter getPrompter()
    {
        return prompter;
    }

    public void setPrompter( final Prompter prompter )
    {
        this.prompter = prompter;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( final String username )
    {
        this.username = username;
    }

    public String getServerAuthId()
    {
        return serverAuthId;
    }

    public void setServerAuthId( final String serverAuthId )
    {
        this.serverAuthId = serverAuthId;
    }

    public String getServerUserAgent()
    {
        return serverUserAgent;
    }

    public void setServerUserAgent( final String serverUserAgent )
    {
        this.serverUserAgent = serverUserAgent;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( final String password )
    {
        this.password = password;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public void setSettings( final Settings settings )
    {
        this.settings = settings;
    }

    protected void setProxyHost( String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    protected void setProxyPort( int proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyPassword()
    {
        return proxyPassword;
    }

    protected void setProxyPassword( String proxyPassword )
    {
        this.proxyPassword = proxyPassword;
    }

    public String getProxyUsername()
    {
        return proxyUsername;
    }

    protected void setProxyUsername( String proxyUsername )
    {
        this.proxyUsername = proxyUsername;
    }

    protected abstract AbstractRESTLightClient connect()
        throws RESTLightClientException, MojoExecutionException;

    protected String formatUrl( final String url )
    {
        if ( url == null )
        {
            return null;
        }

        if ( url.length() < 1 )
        {
            return url;
        }

        return url.endsWith( "/" ) ? url.substring( 0, url.length() - 1 ) : url;
    }

    protected void fillMissing()
        throws MojoExecutionException
    {
        if ( getNexusUrl() != null )
        {
            boolean authFound = false;
            if ( getServerAuthId() != null )
            {
                Server server = getSettings() == null ? null : getSettings().getServer( getServerAuthId() );
                if ( server != null )
                {
                    getLog().info( "Using authentication information for server: '" + getServerAuthId() + "'." );

                    try
                    {
                        setUsername( server.getUsername() );
                        setPassword( discoverer.getSecDispatcher().decrypt( server.getPassword() ) );
                        authFound = true;
                    }
                    catch ( SecDispatcherException e )
                    {
                        throw new MojoExecutionException( "Failed to decrypt Nexus password: " + e.getMessage(), e );
                    }
                }
                else
                {
                    getLog().warn( "Server authentication entry not found for: '" + getServerAuthId() + "'." );
                }
            }

            if ( !authFound && getPassword() == null )
            {
                try
                {
                    NexusConnectionInfo info =
                        discoverer.fillAuth( getNexusUrl(), getSettings(), getProject(), getUsername(), isAutomatic() );

                    if ( info == null )
                    {
                        throw new MojoExecutionException( "Cannot determine login credentials for Nexus instance: "
                                                              + getNexusUrl() );
                    }

                    setUsername( info.getUser() );
                    setPassword( discoverer.getSecDispatcher().decrypt( info.getPassword() ) );

                }
                catch ( NexusDiscoveryException e )
                {
                    throw new MojoExecutionException( "Failed to determine authentication information for Nexus at: "
                                                          + getNexusUrl(), e );
                }
                catch ( SecDispatcherException e )
                {
                    throw new MojoExecutionException( "Failed to decrypt Nexus password: " + e.getMessage(), e );
                }
            }
        }
        else
        {
            try
            {
                NexusConnectionInfo info =
                    discoverer.discover( getSettings(), getProject(), getUsername(), isAutomatic() );

                if ( info == null )
                {
                    throw new MojoExecutionException( "Nexus instance discovery failed." );
                }
                setNexusUrl( info.getNexusUrl() );
                setUsername( info.getUser() );
                setPassword( discoverer.getSecDispatcher().decrypt( info.getPassword() ) );
            }
            catch ( NexusDiscoveryException e )
            {
                throw new MojoExecutionException( "Failed to discover Nexus instance.", e );
            }
            catch ( SecDispatcherException e )
            {
                throw new MojoExecutionException( "Failed to decrypt Nexus password: " + e.getMessage(), e );
            }
        }

    }

    /**
     * @param url the url to parse the host from
     * @return the host in the given url, or null if the url is malformed
     */
    protected String getHostFromUrl( final String url )
    {
        try
        {
            URL u = new URL( url );
            return u.getHost();
        }
        catch ( MalformedURLException ex )
        {
            // returning null instead
        }
        return null;
    }

    /**
     * Configures the proxy information based on the configured settings and Nexus URL.
     *
     * @since 1.9.2
     */
    protected void setAndValidateProxy()
    {
        // proxy only coming from maven
        if ( settings != null )
        {
            Proxy proxy = settings.getActiveProxy();
            if ( proxy != null )
            {
                // HttpClient only supports http
                if ( "http".equals( proxy.getProtocol() ) )
                {
                    // verify that the nexus host is not configured as a non proxiable
                    final String nexusUrl = getNexusUrl();
                    final String nexusHost = getHostFromUrl( nexusUrl );
                    if ( nexusHost != null )
                    {
                        final String nonProxyHosts = proxy.getNonProxyHosts();
                        if ( nonProxyHosts != null )
                        {
                            final String[] nphs = nonProxyHosts.split( "[\\;\\|]" );
                            for ( String nph : nphs )
                            {
                                if ( nph != null && nph.endsWith( "*." + nexusHost ) || nph.equals( nexusHost ) )
                                {
                                    // proxy config excludes proxying nexus host, ignore
                                    getLog().info(
                                        "Not proxying the Nexus connection because the active Maven proxy lists it as a nonProxyHost." );
                                    return;
                                }
                            }
                        }
                    }
                    getLog().info(
                        "Proxying the Nexus client connection because an applicable active Maven proxy is configured." );
                    // ok looks like it applies
                    setProxyHost( proxy.getHost() );
                    setProxyPort( proxy.getPort() );
                    setProxyUsername( proxy.getUsername() );
                    setProxyPassword( proxy.getPassword() );

                }
                else
                {
                    // FIXME hardcoded plugin artifactId
                    getLog().info(
                        "Not proxying the Nexus connection because the nexus-maven-plugin plugin only supports 'http' proxy protocol." );
                }
            }

        }
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( final MavenProject project )
    {
        this.project = project;
    }

    public boolean isAutomatic()
    {
        if ( getSettings() == null )
        {
            return automatic;
        }
        else
        {
            return !getSettings().isInteractiveMode() || automatic;
        }
    }

    public void setAutomatic( final boolean automatic )
    {
        this.automatic = automatic;
    }

    public NexusInstanceDiscoverer getDiscoverer()
    {
        return discoverer;
    }

    public void setDiscoverer( final NexusInstanceDiscoverer discoverer )
    {
        this.discoverer = discoverer;
    }

    public SecDispatcher getDispatcher()
    {
        return discoverer.getSecDispatcher();
    }

    public void setDispatcher( final SecDispatcher dispatcher )
    {
        this.discoverer.setSecDispatcher( dispatcher );
    }

    protected String getUserAgent()
        throws MojoExecutionException
    {
        String userAgent = null;

        try
        {
            final SettingsUserAgentExtractor extractor =
                (SettingsUserAgentExtractor) container.lookup( SettingsUserAgentExtractor.class.getName() );

            String serverForUserAgent = getServerUserAgent();
            if ( serverForUserAgent == null )
            {
                serverForUserAgent = getServerAuthId();
            }
            userAgent = extractor.getUserAgent( getSettings(), serverForUserAgent );
            if ( userAgent != null )
            {
                getLog().info( format( "Using user agent for server '%s': '%s'", serverForUserAgent, userAgent ) );
            }
            else
            {
                getLog().debug( format( "Could not determine user agent for server '%s'", serverForUserAgent ) );
            }
        }
        catch ( Exception ignore )
        {
            // try the other extractors
        }

        if ( userAgent == null )
        {
            try
            {
                final Maven2UserAgentExtractor extractor =
                    (Maven2UserAgentExtractor) container.lookup( Maven2UserAgentExtractor.class.getName() );
                userAgent = extractor.getUserAgent();
                if ( userAgent != null )
                {
                    getLog().info( format( "Using user agent from maven2: '%s'", userAgent ) );
                }
                else
                {
                    getLog().debug( "Could not determine user agent from maven2" );
                }
            }
            catch ( Exception ignore )
            {
                // try the other extractors
            }
        }

        if ( userAgent == null )
        {
            throw new MojoExecutionException( "Could not determine user agent" );
        }

        return userAgent;
    }

}
