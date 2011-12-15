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
package org.sonatype.nexus.configuration.application;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * ApplicationConfiguration is the main component to have and maintain configuration.
 */
public interface ApplicationConfiguration
{
    /**
     * Gets the working directory as file. The directory is created if needed and is guaranteed to exists.
     * 
     * @return
     */
    File getWorkingDirectory();

    /**
     * Gets the working directory with some subpath. The directory is created and is guaranteed to exists.
     * 
     * @param key
     * @return
     */
    File getWorkingDirectory( String key );

    /**
     * Gets the working directory with some subpath. The directory is created if needed.
     * 
     * @param key the subpath you want to have access to
     * @param createIfNeeded set to {@code true} if you want to have it created, {@code false} otherwise. 
     * @return
     * @since 1.10.0
     */
    File getWorkingDirectory( String key, boolean createIfNeeded );

    /**
     * Returns the configuration directory. It defaults to $NEXUS_WORK/conf.
     * 
     * @return
     */
    File getConfigurationDirectory();

    /**
     * Returns the temporary directory.
     * 
     * @return
     */
    File getTemporaryDirectory();

    /**
     * Is security enabled?
     * 
     * @return
     */
    boolean isSecurityEnabled();

    /**
     * Gets the top level local storage context.
     * 
     * @return
     */
    LocalStorageContext getGlobalLocalStorageContext();
    
    /**
     * Gets the top level remote storage context.
     * 
     * @return
     */
    RemoteStorageContext getGlobalRemoteStorageContext();

    /**
     * Saves the configuration.
     * 
     * @throws IOException
     */
    void saveConfiguration()
        throws IOException;

    /**
     * Gets the Configuration object.
     * 
     * @return
     * @deprecated you should use setters/getters directly on Configurable instances, and not tampering with
     *             Configuration model directly!
     */
    Configuration getConfigurationModel();
}
