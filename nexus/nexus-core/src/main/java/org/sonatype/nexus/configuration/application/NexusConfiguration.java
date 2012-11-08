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

import java.io.IOException;
import java.util.Map;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.NexusStreamResponse;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.source.ApplicationConfigurationSource;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * A component responsible for configuration management.
 * 
 * @author cstamas
 */
public interface NexusConfiguration
    extends ApplicationConfiguration, MutableConfiguration
{
    /**
     * Explicit loading of configuration. Does not force reload.
     * 
     * @throws ConfigurationException
     * @throws IOException
     */
    void loadConfiguration()
        throws ConfigurationException, IOException;

    /**
     * Explicit loading of configuration. Enables to force reloading of config.
     * 
     * @throws ConfigurationException
     * @throws IOException
     */
    void loadConfiguration( boolean forceReload )
        throws ConfigurationException, IOException;

    ApplicationConfigurationSource getConfigurationSource();

    boolean isInstanceUpgraded();

    boolean isConfigurationUpgraded();

    boolean isConfigurationDefaulted();

    /**
     * Creates a repository from the CRepository model. Do not use this method!
     * 
     * @param repository
     * @return
     * @throws ConfigurationException
     * @deprecated Do NOT use this method! The MutableConfiguration.createRepository( CRepository settings ) should be
     *             used instead.
     */
    Repository createRepositoryFromModel( CRepository repository )
        throws ConfigurationException;

    // ------------------------------------------------------------------
    // Booting

    /**
     * Creates internals like reposes configured in nexus.xml. Called on startup.
     */
    void createInternals()
        throws ConfigurationException;

    /**
     * Cleanups the internals, like on shutdown.
     */
    void dropInternals();

    /**
     * List the names of files under Configuration Directory
     * 
     * @return A map with the value be file name
     */
    Map<String, String> getConfigurationFiles();

    /**
     * Loads the config file.
     * 
     * @param key
     * @return
     * @throws IOException
     */
    NexusStreamResponse getConfigurationAsStreamByKey( String key )
        throws IOException;
}
