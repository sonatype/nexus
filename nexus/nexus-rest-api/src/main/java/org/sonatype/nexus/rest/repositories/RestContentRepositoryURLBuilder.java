package org.sonatype.nexus.rest.repositories;


import org.codehaus.plexus.util.StringUtils;
import org.restlet.data.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryURLBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Return the REST mounted URLS for Nexus.
 */
@Singleton
@Named( value = "REST" )
public class RestContentRepositoryURLBuilder
    implements RepositoryURLBuilder
{
    private final Logger log = LoggerFactory.getLogger( RestContentRepositoryURLBuilder.class );

    private RepositoryRegistry repositoryRegistry;

    private RepositoryTypeRegistry repositoryTypeRegistry;

    private GlobalRestApiSettings globalRestApiSettings;

    @Inject
    public RestContentRepositoryURLBuilder( RepositoryRegistry repositoryRegistry,
                                            RepositoryTypeRegistry repositoryTypeRegistry,
                                            GlobalRestApiSettings globalRestApiSettings )
    {
        this.repositoryRegistry = repositoryRegistry;
        this.globalRestApiSettings = globalRestApiSettings;
        this.repositoryTypeRegistry = repositoryTypeRegistry;
    }

    @Override
    public String getRepositoryUrl( String repositoryId )
        throws NoSuchRepositoryException
    {
        return getRepositoryUrl( repositoryRegistry.getRepository( repositoryId ) );
    }

    @Override
    public String getRepositoryUrl( Repository repository )
    {
        boolean forceBaseURL =
            globalRestApiSettings.isEnabled() && globalRestApiSettings.isForceBaseUrl() && StringUtils.isNotEmpty(
                globalRestApiSettings.getBaseUrl() );
        String baseURL = null;

        // if force, always use force
        if ( forceBaseURL )
        {
            baseURL = globalRestApiSettings.getBaseUrl();
        }
        // next check if this thread has a request
        else if ( Request.getCurrent() != null )
        {
            baseURL = Request.getCurrent().getRootRef().toString();
        }
        // try to use the baseURL
        else
        {
            baseURL = globalRestApiSettings.getBaseUrl();
        }

        // if all else fails?
        if ( StringUtils.isEmpty( baseURL ) )
        {
            baseURL = "http://base-url-not-set/"; // TODO: what should we do here ?
        }

        StringBuffer url = new StringBuffer( baseURL );
        if ( !baseURL.endsWith( "/" ) )
        {
            url.append( "/" );
        }

        String descriptiveURLPart = "repositories";
        for ( RepositoryTypeDescriptor desc : repositoryTypeRegistry.getRegisteredRepositoryTypeDescriptors() )
        {
            if ( repository.getProviderRole().equals( desc.getRole().getName() ) )
            {
                descriptiveURLPart = desc.getPrefix();
                break;
            }
        }

        url.append( "content/" ).append( descriptiveURLPart ).append( "/" ).append( repository.getId() );

        return url.toString();
    }
}
