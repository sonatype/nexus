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
package org.sonatype.nexus.proxy.repository;

import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractConfigurable;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.mirror.DefaultPublishedMirrors;
import org.sonatype.nexus.proxy.mirror.PublishedMirrors;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;

import com.google.common.base.Strings;

public class ConfigurableRepository
    extends AbstractConfigurable
{
    private PublishedMirrors pMirrors;

    @Override
    protected CRepository getCurrentConfiguration( boolean forWrite )
    {
        return ( (CRepositoryCoreConfiguration) getCurrentCoreConfiguration() ).getConfiguration( forWrite );
    }

    @Override
    protected Configurator getConfigurator()
    {
        return null;
    }

    @Override
    protected ApplicationConfiguration getApplicationConfiguration()
    {
        return null;
    }

    protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory()
    {
        return null;
    }

    @Override
    protected CRepositoryCoreConfiguration wrapConfiguration( Object configuration )
        throws ConfigurationException
    {
        if ( configuration instanceof CRepository )
        {
            return new CRepositoryCoreConfiguration( getApplicationConfiguration(), (CRepository) configuration,
                                                     getExternalConfigurationHolderFactory() );
        }
        else if ( configuration instanceof CRepositoryCoreConfiguration )
        {
            return (CRepositoryCoreConfiguration) configuration;
        }
        else
        {
            throw new ConfigurationException( "The passed configuration object is of class \""
                + configuration.getClass().getName() + "\" and not the required \"" + CRepository.class.getName()
                + "\"!" );
        }
    }

    public String getProviderRole()
    {
        return getCurrentConfiguration( false ).getProviderRole();
    }

    public String getProviderHint()
    {
        return getCurrentConfiguration( false ).getProviderHint();
    }
    
    public String getId()
    {
        return getCurrentConfiguration( false ).getId();
    }

    public void setId( String id )
    {
        getCurrentConfiguration( true ).setId( id );
    }

    public String getName()
    {
        return getCurrentConfiguration( false ).getName();
    }

    public void setName( String name )
    {
        getCurrentConfiguration( true ).setName( name );
    }

    public String getPathPrefix()
    {
        // a "fallback" mechanism: id's must be unique now across nexus,
        // but some older systems may have groups/reposes with same ID. To clear out the ID-clash, we will need to
        // change IDs, but we must _not_ change the published URLs on those systems.
        String pathPrefix = getCurrentConfiguration( false ).getPathPrefix();

        if ( !StringUtils.isBlank( pathPrefix ) )
        {
            return pathPrefix;
        }
        else
        {
            return getId();
        }
    }

    public void setPathPrefix( String prefix )
    {
        getCurrentConfiguration( true ).setPathPrefix( prefix );
    }

    public boolean isIndexable()
    {
        return getCurrentConfiguration( false ).isIndexable();
    }

    public void setIndexable( boolean indexable )
    {
        getCurrentConfiguration( true ).setIndexable( indexable );
    }

    public boolean isSearchable()
    {
        return getCurrentConfiguration( false ).isSearchable();
    }

    public void setSearchable( boolean searchable )
    {
        getCurrentConfiguration( true ).setSearchable( searchable );
    }
    
    /**
     * Method exposing is local storage URL of this repository explicitly set (and probably located at different
     * location than default would be), or it relies (relied if this is "live" {@link Repository} instance) to have the
     * location of local storage defaulted.
     * 
     * @return {@code true} if local storage URL of this repository is explicitly set, does not uses default location.
     * @since 2.5
     */
    public boolean isLocalUrlExplicitlySet()
    {
        // see NEXUS-2482
        return !( getCurrentConfiguration( false ).getLocalStorage() == null || StringUtils.isEmpty( getCurrentConfiguration(
            false ).getLocalStorage().getUrl() ) );
    }

    /**
     * Returns the local storage URL of this repository as String. In case no location is explicitly set, the
     * {@link CRepository#defaultLocalStorageUrl} value is returned. This behaviour is aligned with general Nexus
     * behavior, where if no explicit path is set for {@code repository/localStorage/url} (nexus.xml does not contain
     * it), it's defaulted at runtime (or better, repository creation time) to default local storage location (usually
     * at {@code sonatype-work/nexus/storage/REPO_ID}.
     * 
     * @return the local storage URL as String. Never {@code null} for "live" {@link Repository} instances. It might be
     *         {@code null} when accessed over {@link RepositoryTemplate#getConfigurableRepository()} (not yet created
     *         repository), in which case it means it will be defaulted once created.
     */
    public String getLocalUrl()
    {
        if ( isLocalUrlExplicitlySet() )
        {
            return getCurrentConfiguration( false ).getLocalStorage().getUrl();
        }
        return getCurrentConfiguration( false ).defaultLocalStorageUrl;
    }

    /**
     * Sets the local storage URL of this repository instance. This method will override the URL value only if needed
     * (is different than currently in effect, decided by comparing parameter value and value got from
     * {@link #getLocalUrl()} method), and will handle null-case when no {@link CLocalStorage} instance exists yet in
     * {@link CRepository} underlying configuration.
     * 
     * @param localUrl
     * @throws StorageException
     */
    public void setLocalUrl( final String localUrl )
        throws StorageException
    {
        final String oldLocalUrl = getLocalUrl();
        String newLocalUrl = localUrl == null ? null : localUrl.trim();

        // we need to change URL if (any of these is true):
        // a - old URL was "defaulted" and new URL is not null
        // b - old URL was "explicit" and new URL is not equal to old URL
        final boolean changed =
            ( !isLocalUrlExplicitlySet() && newLocalUrl != null )
                || ( isLocalUrlExplicitlySet() && !oldLocalUrl.equals( newLocalUrl ) );

        // shave if needed
        if ( changed && newLocalUrl != null )
        {
            // remove if needed trailing "/"
            if ( newLocalUrl.endsWith( RepositoryItemUid.PATH_SEPARATOR ) )
            {
                newLocalUrl = newLocalUrl.substring( 0, newLocalUrl.length() - 1 );
            }
            final CRepository newConfiguration = getCurrentConfiguration( true );
            if (newConfiguration.getLocalStorage() == null) 
            {
                final CLocalStorage localStorage = new CLocalStorage();
                localStorage.setProvider( DefaultFSLocalRepositoryStorage.PROVIDER_STRING );
                newConfiguration.setLocalStorage( localStorage );
            }
            getCurrentConfiguration( true ).getLocalStorage().setUrl( newLocalUrl );
        }
        else if ( changed && newLocalUrl == null )
        {
            // reset local storage, we want it defaulted
            getCurrentConfiguration( true ).setLocalStorage( null );
        }
    }

    public LocalStatus getLocalStatus()
    {
        if( getCurrentConfiguration( false ).getLocalStatus() == null )
        {
            return null;
        }
        return LocalStatus.valueOf( getCurrentConfiguration( false ).getLocalStatus() );
    }

    public void setLocalStatus( LocalStatus localStatus )
    {
        getCurrentConfiguration( true ).setLocalStatus( localStatus.toString() );
    }

    public RepositoryWritePolicy getWritePolicy()
    {
        return RepositoryWritePolicy.valueOf( getCurrentConfiguration( false ).getWritePolicy() );
    }

    public void setWritePolicy( RepositoryWritePolicy writePolicy )
    {
        getCurrentConfiguration( true ).setWritePolicy( writePolicy.name() );
    }    
    
    public boolean isBrowseable()
    {
        return getCurrentConfiguration( false ).isBrowseable();
    }

    public void setBrowseable( boolean browseable )
    {
        getCurrentConfiguration( true ).setBrowseable( browseable );
    }

    public boolean isUserManaged()
    {
        return getCurrentConfiguration( false ).isUserManaged();
    }

    public void setUserManaged( boolean userManaged )
    {
        getCurrentConfiguration( true ).setUserManaged( userManaged );
    }

    public boolean isExposed()
    {
        return getCurrentConfiguration( false ).isExposed();
    }

    public void setExposed( boolean exposed )
    {
        getCurrentConfiguration( true ).setExposed( exposed );
    }

    public int getNotFoundCacheTimeToLive()
    {
        return getCurrentConfiguration( false ).getNotFoundCacheTTL();
    }

    public void setNotFoundCacheTimeToLive( int notFoundCacheTimeToLive )
    {
        getCurrentConfiguration( true ).setNotFoundCacheTTL( notFoundCacheTimeToLive );
    }

    public boolean isNotFoundCacheActive()
    {
        return getCurrentConfiguration( false ).isNotFoundCacheActive();
    }

    public void setNotFoundCacheActive( boolean notFoundCacheActive )
    {
        getCurrentConfiguration( true ).setNotFoundCacheActive( notFoundCacheActive );
    }

    public PublishedMirrors getPublishedMirrors()
    {
        if ( pMirrors == null )
        {
            pMirrors = new DefaultPublishedMirrors( (CRepositoryCoreConfiguration) getCurrentCoreConfiguration() );
        }

        return pMirrors;
    }

    // ==

    @Override
    public String toString()
    {
        // this might be instance that is not configured yet, so be careful about getting ID
        // getId() would NPE!
        String repoId = "not-configured-yet";
        final CRepositoryCoreConfiguration currentCoreConfiguration =
            (CRepositoryCoreConfiguration) getCurrentCoreConfiguration();
        if ( currentCoreConfiguration != null )
        {
            final CRepository crepository = currentCoreConfiguration.getConfiguration( false );
            if ( crepository != null && crepository.getId() != null && crepository.getId().trim().length() > 0 )
            {
                repoId = crepository.getId();
            }
        }
        return String.format( "%s(id=%s)", getClass().getSimpleName(), repoId );
    }
}
