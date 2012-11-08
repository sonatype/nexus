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
package org.sonatype.nexus.configuration.source;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.Configuration;

/**
 * A special "static" configuration source, that always return a factory provided defaults for Nexus configuration. It
 * is unmodifiable, since it actually reads the bundled config file from the module's JAR.
 * 
 * @author cstamas
 */
@Component( role = ApplicationConfigurationSource.class, hint = "static" )
public class StaticConfigurationSource
    extends AbstractApplicationConfigurationSource
{

    /**
     * Gets the configuration using getResourceAsStream from "/META-INF/nexus/nexus.xml".
     */
    public InputStream getConfigurationAsStream()
        throws IOException
    {
        InputStream result = getClass().getResourceAsStream( "/META-INF/nexus/nexus.xml" );

        if ( result != null )
        {
            return result;
        }
        else
        {
            getLogger().info( "No edition-specific configuration found, falling back to Core default configuration." );

            return getClass().getResourceAsStream( "/META-INF/nexus/default-oss-nexus.xml" );
        }
    }

    public Configuration loadConfiguration()
        throws ConfigurationException, IOException
    {
        loadConfiguration( getConfigurationAsStream() );

        return getConfiguration();
    }

    /**
     * This method will always throw UnsupportedOperationException, since NexusDefaultsConfigurationSource is read only.
     */
    public void storeConfiguration()
        throws IOException
    {
        throw new UnsupportedOperationException( "The NexusDefaultsConfigurationSource is static source!" );
    }

    /**
     * Static configuration has no default source, hence it cannot be defalted. Always returns false.
     */
    public boolean isConfigurationDefaulted()
    {
        return false;
    }

    /**
     * This method will always throw UnsupportedOperationException, since StaticConfigurationSource is read only.
     */
    public void backupConfiguration()
        throws IOException
    {
        throw new UnsupportedOperationException( "The NexusDefaultsConfigurationSource is static source!" );
    }

}
