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
package org.sonatype.nexus.configuration.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractConfigurable;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.events.RemoteProxySettingsConfigurationChangedEvent;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteProxySettingsCoreConfiguration;
import org.sonatype.nexus.proxy.repository.DefaultRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import com.google.common.base.Throwables;

/**
 * @since 2.5
 */
@Component( role = RemoteProxySettingsConfiguration.class )
public class DefaultRemoteProxySettingsConfiguration
    extends AbstractConfigurable
    implements RemoteProxySettingsConfiguration
{

    @Requirement
    private AuthenticationInfoConverter authenticationInfoConverter;

    @Override
    protected ApplicationConfiguration getApplicationConfiguration()
    {
        return null;
    }

    @Override
    protected Configurator getConfigurator()
    {
        return null;
    }

    @Override
    protected CRemoteProxySettings getCurrentConfiguration( boolean forWrite )
    {
        return ( (CRemoteProxySettingsCoreConfiguration) getCurrentCoreConfiguration() ).getConfiguration( forWrite );
    }

    @Override
    protected CoreConfiguration wrapConfiguration( Object configuration )
        throws ConfigurationException
    {
        if ( configuration instanceof ApplicationConfiguration )
        {
            return new CRemoteProxySettingsCoreConfiguration( (ApplicationConfiguration) configuration );
        }
        else
        {
            throw new ConfigurationException( "The passed configuration object is of class \""
                                                  + configuration.getClass().getName() + "\" and not the required \""
                                                  + ApplicationConfiguration.class.getName() + "\"!" );
        }
    }

    // ==

    @Override
    public RemoteHttpProxySettings getHttpProxySettings()
    {
        if ( isEnabled() )
        {
            try
            {
                return convertFromModel( getCurrentConfiguration( false ).getHttpProxySettings() );
            }
            catch ( ConfigurationException e )
            {
                throw Throwables.propagate( e );
            }
        }

        return null;
    }

    @Override
    public void setHttpProxySettings( final RemoteHttpProxySettings settings )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }
        getCurrentConfiguration( true ).setHttpProxySettings( convertToModel( settings ) );
    }

    @Override
    public RemoteHttpProxySettings getHttpsProxySettings()
    {
        if ( isEnabled() )
        {
            try
            {
                return convertFromModel( getCurrentConfiguration( false ).getHttpsProxySettings() );
            }
            catch ( ConfigurationException e )
            {
                throw Throwables.propagate( e );
            }
        }

        return null;
    }

    @Override
    public void setHttpsProxySettings( final RemoteHttpProxySettings settings )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }
        getCurrentConfiguration( true ).setHttpsProxySettings( convertToModel( settings ) );
    }

    @Override
    public Set<String> getNonProxyHosts()
    {
        if ( isEnabled() )
        {
            return new HashSet<String>( getCurrentConfiguration( false ).getNonProxyHosts() );
        }

        return Collections.emptySet();
    }

    @Override
    public void setNonProxyHosts( Set<String> nonProxyHosts )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }

        getCurrentConfiguration( true ).setNonProxyHosts( new ArrayList<String>(
            nonProxyHosts == null ? Collections.<String>emptySet() : nonProxyHosts
        ) );
    }

    public RemoteProxySettings convertAndValidateFromModel( CRemoteProxySettings model )
        throws ConfigurationException
    {
        ( (CRemoteProxySettingsCoreConfiguration) getCurrentCoreConfiguration() ).doValidateChanges( model );

        if ( model == null )
        {
            return null;
        }

        final RemoteProxySettings settings = new DefaultRemoteProxySettings();

        settings.setHttpProxySettings( convertFromModel( model.getHttpProxySettings() ) );
        settings.setHttpsProxySettings( convertFromModel( model.getHttpsProxySettings() ) );
        settings.setNonProxyHosts( new HashSet<String>( model.getNonProxyHosts() ) );

        return settings;
    }

    private RemoteHttpProxySettings convertFromModel( CRemoteHttpProxySettings model )
        throws ConfigurationException
    {
        if ( model == null )
        {
            return null;
        }

        final RemoteHttpProxySettings settings = new DefaultRemoteHttpProxySettings();

        settings.setHostname( model.getProxyHostname() );
        settings.setPort( model.getProxyPort() );
        settings.setProxyAuthentication(
            authenticationInfoConverter.convertAndValidateFromModel( model.getAuthentication() )
        );

        return settings;
    }

    public CRemoteProxySettings convertToModel( RemoteProxySettings settings )
    {
        if ( settings == null )
        {
            return null;
        }

        final CRemoteProxySettings model = new CRemoteProxySettings();

        model.setHttpProxySettings( convertToModel( settings.getHttpProxySettings() ) );
        model.setHttpsProxySettings( convertToModel( settings.getHttpsProxySettings() ) );
        model.setNonProxyHosts( new ArrayList<String>( settings.getNonProxyHosts() ) );

        return model;
    }

    public CRemoteHttpProxySettings convertToModel( RemoteHttpProxySettings settings )
    {
        if ( settings == null )
        {
            return null;
        }

        final CRemoteHttpProxySettings model = new CRemoteHttpProxySettings();

        model.setProxyHostname( settings.getHostname() );
        model.setProxyPort( settings.getPort() );
        model.setAuthentication( authenticationInfoConverter.convertToModel( settings.getProxyAuthentication() ) );

        return model;
    }

    // ==

    public void disable()
    {
        ( (CRemoteProxySettingsCoreConfiguration) getCurrentCoreConfiguration() ).nullifyConfig();
    }

    public boolean isEnabled()
    {
        return getCurrentConfiguration( false ) != null;
    }

    protected void initConfig()
    {
        ( (CRemoteProxySettingsCoreConfiguration) getCurrentCoreConfiguration() ).initConfig();
    }

    @Override
    public boolean commitChanges()
        throws ConfigurationException
    {
        boolean wasDirty = super.commitChanges();

        if ( wasDirty )
        {
            eventBus().post( new RemoteProxySettingsConfigurationChangedEvent( this ) );
        }

        return wasDirty;
    }

    @Override
    public String getName()
    {
        return "Global Remote Proxy Settings";
    }

}
