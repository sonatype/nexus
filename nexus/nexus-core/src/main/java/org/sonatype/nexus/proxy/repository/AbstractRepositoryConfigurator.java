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
package org.sonatype.nexus.proxy.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;
import org.sonatype.nexus.plugins.RepositoryCustomizer;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;

public abstract class AbstractRepositoryConfigurator
    implements Configurator
{
    @Requirement
    private PlexusContainer plexusContainer;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    @Requirement( role = RepositoryCustomizer.class )
    private Map<String, RepositoryCustomizer> pluginRepositoryConfigurators;

    public final void applyConfiguration( Object target, ApplicationConfiguration configuration,
                                          CoreConfiguration config )
        throws ConfigurationException
    {
        doApplyConfiguration( (Repository) target, configuration, (CRepositoryCoreConfiguration) config );

        // config done, apply customizations
        for ( RepositoryCustomizer configurator : pluginRepositoryConfigurators.values() )
        {
            if ( configurator.isHandledRepository( (Repository) target ) )
            {
                configurator.configureRepository( (Repository) target );
            }
        }
    }

    public final void prepareForSave( Object target, ApplicationConfiguration configuration, CoreConfiguration config )
    {
        // in 1st round, i intentionally choosed to make our lives bitter, and handle plexus config manually
        // later we will see about it
        doPrepareForSave( (Repository) target, configuration, (CRepositoryCoreConfiguration) config );
    }

    protected void doApplyConfiguration( Repository repository, ApplicationConfiguration configuration,
                                         CRepositoryCoreConfiguration coreConfiguration )
        throws ConfigurationException
    {
        // Setting common things on a repository

        // FIXME: hm, we are called when we are dirty, so....
        CRepository repo = coreConfiguration.getConfiguration( true );
        
        // NX-198: filling up the default variable to store the "default" local URL
        File defaultStorageFile = new File( new File( configuration.getWorkingDirectory(), "storage" ), repo.getId() );

        try
        {
            repo.defaultLocalStorageUrl = defaultStorageFile.toURI().toURL().toString();
        }
        catch ( MalformedURLException e )
        {
            // will not happen, not user settable
            throw new InvalidConfigurationException( "Malformed URL for LocalRepositoryStorage!", e );
        }

        String localUrl = null;
        boolean usingDefaultLocalUrl = false;
        
        if ( repo.getLocalStorage() != null && !StringUtils.isEmpty( repo.getLocalStorage().getUrl() ) )
        {
            localUrl = repo.getLocalStorage().getUrl();
        }
        else
        {
            localUrl = repo.defaultLocalStorageUrl;
            usingDefaultLocalUrl = true;

            // Default dir is going to be valid
            defaultStorageFile.mkdirs();
        }

        if ( repo.getLocalStorage() == null )
        {
            repo.setLocalStorage( new CLocalStorage() );

            repo.getLocalStorage().setProvider( "file" );
        }

        LocalRepositoryStorage ls = getLocalRepositoryStorage( repo.getId(), repo.getLocalStorage().getProvider() );

        try
        {
            ls.validateStorageUrl( localUrl );

            if( !usingDefaultLocalUrl )
            {
                repo.getLocalStorage().setUrl( localUrl );
            }

            repository.setLocalStorage( ls );
        }
        catch ( StorageException e )
        {
            ValidationResponse response = new ApplicationValidationResponse();

            ValidationMessage error =
                new ValidationMessage( "overrideLocalStorageUrl", "Repository has an invalid local storage URL '"
                    + localUrl, "Invalid file location" );

            response.addValidationError( error );

            throw new InvalidConfigurationException( response );
        }

        // clear the NotFoundCache
        if ( repository.getNotFoundCache() != null )
        {
            repository.getNotFoundCache().purge();
        }
    }

    protected void doPrepareForSave( Repository repository, ApplicationConfiguration configuration,
                                     CRepositoryCoreConfiguration coreConfiguration )
    {
        // Setting common things on a repository
    }

    // ==

    protected PlexusContainer getPlexusContainer()
    {
        return plexusContainer;
    }

    protected RepositoryRegistry getRepositoryRegistry()
    {
        return repositoryRegistry;
    }

    protected RepositoryTypeRegistry getRepositoryTypeRegistry()
    {
        return repositoryTypeRegistry;
    }

    protected boolean existsRepositoryType( Class<?> role, String hint )
        throws InvalidConfigurationException
    {
        return componentExists( role, hint );
    }

    protected boolean existsLocalRepositoryStorage( String repoId, String provider )
        throws InvalidConfigurationException
    {
        return componentExists( LocalRepositoryStorage.class, provider );
    }

    protected boolean existsRemoteRepositoryStorage( String repoId, String provider )
        throws InvalidConfigurationException
    {
        return componentExists( RemoteRepositoryStorage.class, provider );
    }

    protected boolean componentExists( Class<?> role, String hint )
    {
        return getPlexusContainer().hasComponent( role, hint );
    }

    protected LocalRepositoryStorage getLocalRepositoryStorage( String repoId, String provider )
        throws InvalidConfigurationException
    {
        try
        {
            return getPlexusContainer().lookup( LocalRepositoryStorage.class, provider );
        }
        catch ( ComponentLookupException e )
        {
            throw new InvalidConfigurationException( "Repository " + repoId
                + " have local storage with unsupported provider: " + provider, e );
        }
    }

}
