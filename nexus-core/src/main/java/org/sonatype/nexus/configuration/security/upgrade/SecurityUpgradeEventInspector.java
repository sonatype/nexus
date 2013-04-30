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
package org.sonatype.nexus.configuration.security.upgrade;

import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.proxy.events.AbstractEventInspector;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.security.configuration.SecurityConfigurationManager;
import org.sonatype.security.events.SecurityConfigurationChanged;
import org.sonatype.security.model.CUser;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.source.SecurityModelConfigurationSource;
import org.sonatype.security.model.upgrade.SecurityDataUpgrader;
import org.sonatype.sisu.goodies.eventbus.EventBus;

@Component( role = EventInspector.class, hint = "SecurityUpgradeEventInspector" )
public class SecurityUpgradeEventInspector
    extends AbstractEventInspector
{

    @Requirement
    private ApplicationStatusSource applicationStatusSource;

    @Requirement( hint = "file" )
    private SecurityModelConfigurationSource realmConfigSource;

    @Requirement
    private SecurityConfigurationManager systemConfigManager;

    /**
     * Reuse the previous versions upgrader, this is normally run after the module upgrade of 2.0.1, so the module is
     * actually 2.0.2.
     */
    @Requirement( hint = "2.0.1" )
    private SecurityDataUpgrader upgrader;

    @Requirement
    private EventBus eventBus;

    public boolean accepts( Event<?> evt )
    {
        return ( evt instanceof NexusStartedEvent );
    }

    public void inspect( Event<?> evt )
    {
        final SystemStatus systemStatus = applicationStatusSource.getSystemStatus();

        if ( systemStatus.isConfigurationUpgraded() || systemStatus.isInstanceUpgraded() )
        {
            final NexusStartedEvent startedEvent = (NexusStartedEvent) evt;

            try
            {
                // re/load the config from file
                realmConfigSource.loadConfiguration();

                // if Nexus was upgraded and the security version is 2.0.2 we need to update the model
                // NOTE: once the security version changes we no longer need this class
                boolean changed = false;
                Configuration securityRealmConfig = realmConfigSource.getConfiguration();
                if ( securityRealmConfig.getVersion().equals( "2.0.2" ) )
                {
                    // first get the config and upgrade it
                    upgrader.upgrade( realmConfigSource.getConfiguration() );
                    changed = true;
                }

                // NEXUS-5049: but this time, we need to perform this _not_ against SecuritySystem API (is still not up)
                // but by directly "tampering" with it's configuration(s).
                if ( !systemConfigManager.isAnonymousAccessEnabled()
                    && !StringUtils.isBlank( systemConfigManager.getAnonymousUsername() ) )
                {
                    // get the probably _changed_ one again
                    securityRealmConfig = realmConfigSource.getConfiguration();

                    for ( CUser user : securityRealmConfig.getUsers() )
                    {
                        if ( StringUtils.equals( systemConfigManager.getAnonymousUsername(), user.getId() ) )
                        {
                            user.setStatus( CUser.STATUS_DISABLED );
                            changed = true;
                            break;
                        }
                    }
                }

                if ( changed )
                {
                    // now save
                    realmConfigSource.storeConfiguration();
                    // because we change the configuration directly we need to tell the SecuritySystem to clear the
                    // cache,
                    // although at this point nothing should be cached, but better safe the sorry
                    eventBus.post( new SecurityConfigurationChanged() );
                }
            }
            catch ( ConfigurationIsCorruptedException e )
            {
                this.getLogger().error( "Failed to upgrade security.xml: " + e );
                startedEvent.putVeto( this, e );
            }
            catch ( ConfigurationException e )
            {
                this.getLogger().error( "Failed to upgrade security.xml: " + e );
                startedEvent.putVeto( this, e );
            }
            catch ( IOException e )
            {
                this.getLogger().error( "Failed to upgrade security.xml: " + e );
                startedEvent.putVeto( this, e );
            }
        }
    }
}
