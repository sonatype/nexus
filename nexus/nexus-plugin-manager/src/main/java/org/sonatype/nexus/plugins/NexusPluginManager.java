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
package org.sonatype.nexus.plugins;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginMetadata;

/**
 * Manages Nexus plugins, including both system and user types.
 */
public interface NexusPluginManager
{
    /**
     * Queries which plugins are activated at the moment.
     * 
     * @return Map of activated plugins and their descriptors
     */
    Map<GAVCoordinate, PluginDescriptor> getActivatedPlugins();

    /**
     * Queries which plugins are installed at the moment.
     * 
     * @return Map of installed plugins and their metadata
     */
    Map<GAVCoordinate, PluginMetadata> getInstalledPlugins();

    /**
     * Queries for plugin responses to recent actions/requests.
     * 
     * @return Map of plugin responses to recent requests
     */
    Map<GAVCoordinate, PluginResponse> getPluginResponses();

    /**
     * Attempts to activate all installed plugins.
     * 
     * @return Responses to the activation request
     */
    Collection<PluginManagerResponse> activateInstalledPlugins();

    /**
     * Queries to see if the given plugin is activated or not.
     * 
     * @param gav The plugin coordinates
     * @return {@code true} if the plugin is activated; otherwise {@code false}
     */
    boolean isActivatedPlugin( GAVCoordinate gav );

    /**
     * Attempts to activate the given plugin.
     * 
     * @param gav The plugin coordinates
     * @return Response to the activation request
     */
    PluginManagerResponse activatePlugin( GAVCoordinate gav );

    /**
     * Attempts to de-activate the given plugin.
     * 
     * @param gav The plugin coordinates
     * @return Response to the de-activation request
     */
    PluginManagerResponse deactivatePlugin( GAVCoordinate gav );

    /**
     * Downloads and installs the given Nexus plugin into the writable repository.
     * 
     * @param bundle The plugin resource bundle
     * @return {@code true} if the plugin installed successfully; otherwise {@code false}
     */
    boolean installPluginBundle( URL bundle )
        throws IOException;

    /**
     * Uninstalls the given Nexus plugin from the writable repository.
     * 
     * @param gav The plugin coordinates
     * @return {@code true} if the plugin was successfully deleted; otherwise {@code false}
     */
    boolean uninstallPluginBundle( GAVCoordinate gav )
        throws IOException;
}
