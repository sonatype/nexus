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
package org.sonatype.nexus.notification;

import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractConfigurable;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CNotification;
import org.sonatype.nexus.configuration.model.CNotificationConfiguration;
import org.sonatype.nexus.configuration.model.CNotificationTarget;
import org.sonatype.nexus.logging.Slf4jPlexusLogger;

@Component( role = NotificationManager.class )
public class DefaultNotificationManager
    extends AbstractConfigurable
    implements NotificationManager
{
    private Logger logger = Slf4jPlexusLogger.getPlexusLogger( getClass() );

    @Requirement
    private NexusConfiguration nexusConfig;

    @Requirement( role = Carrier.class )
    private Map<String, Carrier> carriers;
    
    // ==

    protected Logger getLogger()
    {
        return logger;
    }

    // ==

    public String getName()
    {
        return "Notification configuration";
    }

    @Override
    protected void initializeConfiguration()
        throws ConfigurationException
    {
        if ( getApplicationConfiguration().getConfigurationModel() != null )
        {
            configure( getApplicationConfiguration() );
        }
    }

    @Override
    protected boolean isConfigured()
    {
        return super.isConfigured() && getCurrentConfiguration( false ) != null;
    }

    @Override
    protected ApplicationConfiguration getApplicationConfiguration()
    {
        return nexusConfig;
    }

    @Override
    protected Configurator getConfigurator()
    {
        // not custom configurators needed
        return null;
    }

    @Override
    protected CNotification getCurrentConfiguration( boolean forWrite )
    {
        return ( (CNotificationConfiguration) getCurrentCoreConfiguration() ).getConfiguration( forWrite );
    }

    @Override
    protected CoreConfiguration wrapConfiguration( Object configuration )
        throws ConfigurationException
    {
        if ( configuration instanceof ApplicationConfiguration )
        {
            return new CNotificationConfiguration( getApplicationConfiguration() );
        }
        else
        {
            throw new ConfigurationException( "The passed configuration object is of class \""
                + configuration.getClass().getName() + "\" and not the required \""
                + ApplicationConfiguration.class.getName() + "\"!" );
        }
    }

    // ==
    public boolean isEnabled()
    {
        // TODO: this is needed, since events are happening even before configuration is loaded
        // And the NotificationEventInspector will start relaying even before we know our config!
        // This is actually chicken-egg problem: the EventInspector Hosts are wired in before we 
        // load Nexus config.
        return isConfigured() && getCurrentConfiguration( false ).isEnabled();
    }

    public void setEnabled( boolean val )
    {
        getCurrentConfiguration( true ).setEnabled( val );
    }

    public NotificationTarget readNotificationTarget( String targetId )
    {
        if ( targetId == null )
        {
            throw new NullPointerException( "Notification target ID can't be null!" );
        }

        // TODO: we always ask for this since we have this group only
        targetId = NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID;

        List<CNotificationTarget> targets = getCurrentConfiguration( false ).getNotificationTargets();

        for ( CNotificationTarget target : targets )
        {
            if ( targetId.equals( target.getTargetId() ) )
            {
                NotificationTarget result = new NotificationTarget();

                result.setTargetId( target.getTargetId() );

                result.getTargetRoles().addAll( target.getTargetRoles() );

                result.getTargetUsers().addAll( target.getTargetUsers() );

                result.getExternalTargets().addAll( target.getTargetExternals() );

                return result;
            }
        }

        return null;
    }

    public void updateNotificationTarget( NotificationTarget target )
    {
        if ( target == null )
        {
            throw new NullPointerException( "Notification target can't be null!" );
        }

        // TODO: reimplement this to handle multiple targets!
        target.setTargetId( NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID );

        CNotificationTarget ctarget = new CNotificationTarget();

        ctarget.setTargetId( target.getTargetId() );
        ctarget.getTargetRoles().addAll( target.getTargetRoles() );
        ctarget.getTargetUsers().addAll( target.getTargetUsers() );
        ctarget.getTargetExternals().addAll( target.getExternalTargets() );

        List<CNotificationTarget> targets = getCurrentConfiguration( true ).getNotificationTargets();

        targets.clear();

        targets.add( ctarget );
    }

    public void notifyTargets( NotificationRequest request )
    {
        if ( !isEnabled() )
        {
            return;
        }

        for ( NotificationTarget target : request.getTargets() )
        {
            // TODO: should come from the group
            // for now, email is wired in
            String carrierKey = NotificationCheat.CARRIER_KEY;

            Carrier carrier = carriers.get( carrierKey );

            if ( carrier != null )
            {
                try
                {
                    carrier.notifyTarget( target, request.getMessage() );
                }
                catch ( NotificationException e )
                {
                    getLogger().warn( "Could not send out notification over carrier \"" + carrierKey + "\".", e );
                }
            }
            else
            {
                getLogger().info( "Notification carrier \"" + carrierKey + "\" is unknown!" );
            }
        }
    }

}
