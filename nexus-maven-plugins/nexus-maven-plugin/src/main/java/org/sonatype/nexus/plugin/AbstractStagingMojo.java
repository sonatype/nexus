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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.sonatype.nexus.restlight.common.ProxyConfig;
import org.sonatype.nexus.restlight.common.RESTLightClientException;
import org.sonatype.nexus.restlight.stage.StageClient;
import org.sonatype.nexus.restlight.stage.StageProfile;
import org.sonatype.nexus.restlight.stage.StageRepository;

public abstract class AbstractStagingMojo
    extends AbstractNexusMojo
{

    /**
     * If provided, and this repository is available for selection, use it.
     *
     * @parameter expression="${nexus.repositoryId}"
     */
    private String repositoryId;

    private StageClient client;

    public AbstractStagingMojo()
    {
        super();
    }

    @Override
    protected final synchronized StageClient connect()
        throws RESTLightClientException
    {
        final String url = formatUrl( getNexusUrl() );

        setAndValidateProxy();
        getLog().info( "Logging into Nexus: " + url );
        getLog().info( "User: " + getUsername() );

        ProxyConfig proxyConfig = null;
        if ( getProxyHost() != null )
        {
            proxyConfig = new ProxyConfig( getProxyHost(), getProxyPort(), getProxyUsername(), getProxyPassword() );
        }

        client = new StageClient( url, getUsername(), getPassword(), proxyConfig );
        return client;
    }

    protected StageClient getClient()
        throws MojoExecutionException
    {
        if ( client == null )
        {
            try
            {
                connect();
            }
            catch ( RESTLightClientException e )
            {
                throw new MojoExecutionException( "Failed to connect to Nexus at: " + getNexusUrl() );
            }
        }

        return client;
    }

    protected void listRepos( final String groupId, final String artifactId, final String version, final String prompt )
        throws MojoExecutionException
    {
        List<StageRepository> repos;
        StringBuilder builder = new StringBuilder();

        try
        {
            if ( groupId != null )
            {
                repos = getClient().getClosedStageRepositoriesForUser( groupId, artifactId, version );
                builder.append( prompt ).append( " for: '" ).append( groupId ).append( ":" ).append( artifactId ).append(
                    ":" ).append( version ).append( "':" );
            }
            else
            {
                repos = getClient().getClosedStageRepositories();
                builder.append( prompt ).append( ": " );
            }
        }
        catch ( RESTLightClientException e )
        {
            throw new MojoExecutionException( "Failed to list staging repositories: " + e.getMessage(), e );
        }

        if ( repos.isEmpty() )
        {
            builder.append( "\n\nNone." );
        }
        else
        {
            for ( StageRepository repo : repos )
            {
                builder.append( "\n\n-  " );
                builder.append( listRepo( repo ) );
            }
        }

        builder.append( "\n\n" );

        getLog().info( builder.toString() );
    }

    protected CharSequence listRepo( final StageRepository repo )
    {
        StringBuilder builder = new StringBuilder();

        builder.append( repo.getRepositoryId() ).append( " (profile: " ).append( repo.getProfileName() ).append( ")" );

        if ( repo.getUrl() != null )
        {
            builder.append( "\n   URL: " ).append( repo.getUrl() );
        }

        if ( repo.getDescription() != null )
        {
            builder.append( "\n   Description: " ).append( repo.getDescription() );
        }

        builder.append( "\n   Details: (user: " ).append( repo.getUser() ).append( ", " );
        builder.append( "ip: " ).append( repo.getIpAddress() ).append( ", " );
        builder.append( "user agent: " ).append( repo.getUserAgent() ).append( ")" );

        return builder;
    }

    protected CharSequence listProfile( final StageProfile profile )
    {
        StringBuilder builder = new StringBuilder();

        builder.append( "Id: " ).append( profile.getProfileId() ).append( "\tname: " ).append( profile.getName() ).append(
            "\tmode: " ).append( profile.getMode() );

        return builder;
    }

    protected StageRepository select( final List<StageRepository> stageRepos, final String basicPrompt,
                                      final boolean allowAutoSelect )
        throws MojoExecutionException
    {
        List<StageRepository> stageRepositories = stageRepos;

        if ( stageRepositories == null || stageRepositories.isEmpty() )
        {
            throw new MojoExecutionException( "No repositories available." );
        }

        if ( allowAutoSelect && isAutomatic() )
        {
            stageRepositories = filterForAutomaticSelection( stageRepositories );
            if ( stageRepositories == null || stageRepositories.isEmpty() )
            {
                throw new MojoExecutionException( format(
                    "No repositories available for user id: '%s', user agent: '%s'",
                    getUsername(), getUserAgent() )
                );
            }
        }

        if ( getRepositoryId() != null )
        {
            for ( StageRepository repo : stageRepositories )
            {
                if ( getRepositoryId().equals( repo.getRepositoryId() ) )
                {
                    return repo;
                }
            }
        }

        if ( allowAutoSelect && isAutomatic() && stageRepositories.size() == 1 )
        {
            StageRepository repo = stageRepositories.get( 0 );
            getLog().info( "Using the only staged repository available: " + repo.getRepositoryId() );

            return repo;
        }

        LinkedHashMap<String, StageRepository> repoMap = new LinkedHashMap<String, StageRepository>();
        StringBuilder menu = new StringBuilder();
        List<String> choices = new ArrayList<String>();

        menu.append( "\n\n\nAvailable Staging Repositories:\n\n" );

        int i = 0;
        for ( StageRepository repo : stageRepositories )
        {
            ++i;
            repoMap.put( Integer.toString( i ), repo );
            choices.add( Integer.toString( i ) );

            menu.append( "\n" ).append( i ).append( ": " ).append( listRepo( repo ) ).append( "\n" );
        }

        menu.append( "\n\n" );

        if ( isAutomatic() )
        {
            getLog().info( menu.toString() );
            throw new MojoExecutionException(
                "Cannot auto-select; multiple staging repositories are available, and none are specified for use." );
        }
        else
        {
            String choice = null;
            while ( choice == null || !repoMap.containsKey( choice ) )
            {
                getLog().info( menu.toString() );
                try
                {
                    choice = getPrompter().prompt( basicPrompt, choices, "1" );
                }
                catch ( PrompterException e )
                {
                    throw new MojoExecutionException( "Failed to read from CLI prompt: " + e.getMessage(), e );
                }
            }

            return repoMap.get( choice );
        }
    }

    /**
     * Filters out all repositories that does not match the current user id / user agent.
     *
     * @param repositories to be filtered
     * @return filtered
     * @throws MojoExecutionException in case current user agent could not be determined
     */
    private List<StageRepository> filterForAutomaticSelection( final List<StageRepository> repositories )
        throws MojoExecutionException
    {
        final List<StageRepository> filtered = new ArrayList<StageRepository>();

        final String userId = getUsername();
        final String userAgent = getUserAgent();

        if ( userId != null && userAgent != null )
        {
            for ( final StageRepository repository : repositories )
            {
                if ( userId.equals( repository.getUser() )
                    && userAgent.equals( repository.getUserAgent() ) )
                {
                    filtered.add( repository );
                }
            }
        }

        return filtered;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( final String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

}