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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * Adapter for NexusConfiguration.
 * 
 * @author cstamas
 */
@Component( role = ApplicationConfiguration.class )
public class ApplicationConfigurationAdapter
    implements ApplicationConfiguration
{
    @Requirement
    private NexusConfiguration nexusConfiguration;

    @Deprecated
    public Configuration getConfigurationModel()
    {
        return nexusConfiguration.getConfigurationModel();
    }

    public File getWorkingDirectory()
    {
        return nexusConfiguration.getWorkingDirectory();
    }

    public File getWorkingDirectory( final String key )
    {
        return nexusConfiguration.getWorkingDirectory( key );
    }

    public File getWorkingDirectory( final String key, final boolean createIfNeeded )
    {
        return nexusConfiguration.getWorkingDirectory( key, createIfNeeded );
    }

    public File getTemporaryDirectory()
    {
        return nexusConfiguration.getTemporaryDirectory();
    }

    public File getConfigurationDirectory()
    {
        return nexusConfiguration.getConfigurationDirectory();
    }

    public void saveConfiguration()
        throws IOException
    {
        nexusConfiguration.saveConfiguration();
    }

    public boolean isSecurityEnabled()
    {
        return nexusConfiguration.isSecurityEnabled();
    }

    public LocalStorageContext getGlobalLocalStorageContext()
    {
        return nexusConfiguration.getGlobalLocalStorageContext();
    }

    public RemoteStorageContext getGlobalRemoteStorageContext()
    {
        return nexusConfiguration.getGlobalRemoteStorageContext();
    }

}
