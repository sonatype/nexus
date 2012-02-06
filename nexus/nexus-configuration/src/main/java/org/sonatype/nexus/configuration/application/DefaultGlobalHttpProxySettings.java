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
import org.sonatype.nexus.configuration.application.events.GlobalHttpProxySettingsChangedEvent;
import org.sonatype.nexus.configuration.model.CGlobalHttpProxySettingsCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;

@Component( role = GlobalHttpProxySettings.class )
public class DefaultGlobalHttpProxySettings
    extends AbstractConfigurable
    implements GlobalHttpProxySettings
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
    protected CRemoteHttpProxySettings getCurrentConfiguration( boolean forWrite )
    {
        return ( (CGlobalHttpProxySettingsCoreConfiguration) getCurrentCoreConfiguration() )
            .getConfiguration( forWrite );
    }

    @Override
    protected CoreConfiguration wrapConfiguration( Object configuration )
        throws ConfigurationException
    {
        if ( configuration instanceof ApplicationConfiguration )
        {
            return new CGlobalHttpProxySettingsCoreConfiguration( (ApplicationConfiguration) configuration );
        }
        else
        {
            throw new ConfigurationException( "The passed configuration object is of class \""
                + configuration.getClass().getName() + "\" and not the required \""
                + ApplicationConfiguration.class.getName() + "\"!" );
        }
    }

    // ==

    public boolean isBlockInheritance()
    {
        if ( isEnabled() )
        {
            return getCurrentConfiguration( false ).isBlockInheritance();
        }
        
        return false;
    }

    public void setBlockInheritance( boolean val )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }

        getCurrentConfiguration( true ).setBlockInheritance( val );
    }

    public String getHostname()
    {
        if ( isEnabled() )
        {
            return getCurrentConfiguration( false ).getProxyHostname();
        }
        
        return null;
    }

    public void setHostname( String hostname )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }

        getCurrentConfiguration( true ).setProxyHostname( hostname );
    }

    public int getPort()
    {
        if ( isEnabled() )
        {
            return getCurrentConfiguration( false ).getProxyPort();
        }
        
        return -1;
    }

    public void setPort( int port )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }

        getCurrentConfiguration( true ).setProxyPort( port );
    }

    public RemoteAuthenticationSettings getProxyAuthentication()
    {
        if ( isEnabled() )
        {
            try
            {
                return authenticationInfoConverter.convertAndValidateFromModel( getCurrentConfiguration( false )
                    .getAuthentication() );
            }
            catch ( ConfigurationException e )
            {
                // FIXME: what here??
    
                setProxyAuthentication( null );
    
                return null;
            }
        }
        
        return null;
    }

    public void setProxyAuthentication( RemoteAuthenticationSettings proxyAuthentication )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }

        getCurrentConfiguration( true ).setAuthentication(
                                                           authenticationInfoConverter
                                                               .convertToModel( proxyAuthentication ) );
    }

    public RemoteProxySettings convertAndValidateFromModel( CRemoteHttpProxySettings model )
        throws ConfigurationException
    {
        ( (CGlobalHttpProxySettingsCoreConfiguration) getCurrentCoreConfiguration() ).doValidateChanges( model );

        if ( model != null )
        {
            RemoteProxySettings remoteProxySettings = new DefaultRemoteProxySettings();

            remoteProxySettings.setBlockInheritance( model.isBlockInheritance() );

            if ( remoteProxySettings.isBlockInheritance() )
            {
                return remoteProxySettings;
            }

            remoteProxySettings.setHostname( model.getProxyHostname() );

            remoteProxySettings.setPort( model.getProxyPort() );

            remoteProxySettings.setProxyAuthentication( authenticationInfoConverter.convertAndValidateFromModel( model
                .getAuthentication() ) );
            
            remoteProxySettings.setNonProxyHosts( new HashSet<String>( model.getNonProxyHosts() ) );

            return remoteProxySettings;
        }
        else
        {
            return null;
        }
    }

    public CRemoteHttpProxySettings convertToModel( RemoteProxySettings settings )
    {
        if ( settings == null )
        {
            return null;
        }
        else
        {
            CRemoteHttpProxySettings model = new CRemoteHttpProxySettings();

            model.setBlockInheritance( settings.isBlockInheritance() );

            model.setProxyHostname( settings.getHostname() );

            model.setProxyPort( settings.getPort() );

            model.setAuthentication( authenticationInfoConverter.convertToModel( settings.getProxyAuthentication() ) );
            
            model.setNonProxyHosts( new ArrayList<String>(settings.getNonProxyHosts() ) );

            return model;
        }
    }

    // ==

    public void disable()
    {
        ( (CGlobalHttpProxySettingsCoreConfiguration) getCurrentCoreConfiguration() ).nullifyConfig();
    }

    public boolean isEnabled()
    {
        return getCurrentConfiguration( false ) != null;
    }
    
    protected void initConfig()
    {
        ( (CGlobalHttpProxySettingsCoreConfiguration) getCurrentCoreConfiguration() ).initConfig();
    }

    public String getName()
    {
        return "Global Http Proxy Settings";
    }

    public Set<String> getNonProxyHosts()
    {
        if ( isEnabled() )
        {
            return new HashSet<String>( getCurrentConfiguration( false ).getNonProxyHosts() );
        }
        
        return Collections.emptySet();
    }

    public void setNonProxyHosts( Set<String> nonProxyHosts )
    {
        if ( !isEnabled() )
        {
            initConfig();
        }

        getCurrentConfiguration( true ).setNonProxyHosts( new ArrayList<String>( nonProxyHosts ) );
    }

    @Override
    public boolean commitChanges()
        throws ConfigurationException
    {
        boolean wasDirty = super.commitChanges();

        if ( wasDirty )
        {
            getApplicationEventMulticaster().notifyEventListeners( new GlobalHttpProxySettingsChangedEvent( this ) );
        }

        return wasDirty;
    }

}
