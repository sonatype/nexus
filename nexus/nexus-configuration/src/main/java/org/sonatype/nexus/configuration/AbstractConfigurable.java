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
package org.sonatype.nexus.configuration;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plexus.appevents.EventListener;

/**
 * Abstract class to implement configurable components to "click" them in into generic configuration environment.
 *
 * NOTE: Don't convert AbstractConfigurable plexus components to sisu, they will be initialized BEFORE configuration is loaded.
 * 
 * @author cstamas
 */
public abstract class AbstractConfigurable
    implements Configurable, EventListener, Initializable, Disposable
{
    /** The configuration */
    private CoreConfiguration coreConfiguration;

    @Requirement
    private ApplicationEventMulticaster applicationEventMulticaster;

    /**
     * For plexus injection.
     */
    public AbstractConfigurable()
    {
    }

    public AbstractConfigurable( final ApplicationEventMulticaster applicationEventMulticaster )
    {
        this.applicationEventMulticaster = applicationEventMulticaster;
    }

    protected boolean isConfigured()
    {
        return coreConfiguration != null;
    }

    public void initialize()
        throws InitializationException
    {
        applicationEventMulticaster.addEventListener( this );

        try
        {
            initializeConfiguration();
        }
        catch ( ConfigurationException e )
        {
            throw new InitializationException( "Cannot configure the component!", e );
        }
    }

    protected void initializeConfiguration()
        throws ConfigurationException
    {
        // someone needs this, someone not
        // for example, whoever is configged using framework, will not need this,
        // but we still have components on their own, like DefaultTaskConfigManager
        // that are driven by spice Scheduler
    }

    public void dispose()
    {
        applicationEventMulticaster.removeEventListener( this );
    }

    public void onEvent( final Event<?> evt )
    {
        // act automatically on config events
        if ( evt instanceof ConfigurationPrepareForLoadEvent )
        {
            ConfigurationPrepareForLoadEvent vevt = (ConfigurationPrepareForLoadEvent) evt;

            try
            {
                // validate
                initializeConfiguration();
            }
            catch ( ConfigurationException e )
            {
                // put a veto
                vevt.putVeto( this, e );
            }
        }
        else if ( evt instanceof ConfigurationPrepareForSaveEvent )
        {
            if ( isDirty() )
            {
                ConfigurationPrepareForSaveEvent psevt = (ConfigurationPrepareForSaveEvent) evt;

                try
                {
                    // prepare
                    prepareForSave();

                    // register ourselves as changed
                    psevt.getChanges().add( this );
                }
                catch ( ConfigurationException e )
                {
                    // put a veto
                    psevt.putVeto( this, e );
                }
            }
        }
        else if ( evt instanceof ConfigurationValidateEvent )
        {
            ConfigurationValidateEvent vevt = (ConfigurationValidateEvent) evt;

            try
            {
                // validate
                getCurrentCoreConfiguration().validateChanges();
            }
            catch ( ConfigurationException e )
            {
                // put a veto
                vevt.putVeto( this, e );
            }
        }
        else if ( evt instanceof ConfigurationCommitEvent )
        {
            try
            {
                commitChanges();
            }
            catch ( ConfigurationException e )
            {
                // FIXME: log or something?
                rollbackChanges();
            }
        }
        else if ( evt instanceof ConfigurationRollbackEvent )
        {
            rollbackChanges();
        }
    }

    protected ApplicationEventMulticaster getApplicationEventMulticaster()
    {
        return applicationEventMulticaster;
    }

    protected abstract ApplicationConfiguration getApplicationConfiguration();

    // Configurable iface

    public final CoreConfiguration getCurrentCoreConfiguration()
    {
        return coreConfiguration;
    }

    public final void configure( Object config )
        throws ConfigurationException
    {
        this.coreConfiguration = wrapConfiguration( config );

        // "pull" the config to make it dirty
        getCurrentConfiguration( true );

        // do commit
        doConfigure();
    }

    public boolean isDirty()
    {
        return getCurrentCoreConfiguration().isDirty();
    }

    protected void prepareForSave()
        throws ConfigurationException
    {
        if ( isDirty() )
        {
            getCurrentCoreConfiguration().validateChanges();

            if ( getConfigurator() != null )
            {
                // prepare for save: transfer what we have in memory (if any) to model
                getConfigurator().prepareForSave( this, getApplicationConfiguration(), getCurrentCoreConfiguration() );
            }
        }
    }

    public boolean commitChanges()
        throws ConfigurationException
    {
        if ( isDirty() )
        {
            doConfigure();

            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean rollbackChanges()
    {
        if ( isDirty() )
        {
            getCurrentCoreConfiguration().rollbackChanges();

            return true;
        }
        else
        {
            return false;
        }
    }

    // ==

    protected void doConfigure()
        throws ConfigurationException
    {
        // 1st, validate
        getCurrentCoreConfiguration().validateChanges();

        // 2nd, we apply configurator (it will map things that are not 1:1 from config object)
        if ( getConfigurator() != null )
        {
            // apply config, transfer what is not mappable (if any) from model
            getConfigurator().applyConfiguration( this, getApplicationConfiguration(), getCurrentCoreConfiguration() );

            // prepare for save: transfer what we have in memory (if any) to model
            getConfigurator().prepareForSave( this, getApplicationConfiguration(), getCurrentCoreConfiguration() );
        }

        // 3rd, commit
        getCurrentCoreConfiguration().commitChanges();
    }

    protected abstract Configurator getConfigurator();

    protected abstract Object getCurrentConfiguration( boolean forWrite );

    protected abstract CoreConfiguration wrapConfiguration( Object configuration )
        throws ConfigurationException;
}
