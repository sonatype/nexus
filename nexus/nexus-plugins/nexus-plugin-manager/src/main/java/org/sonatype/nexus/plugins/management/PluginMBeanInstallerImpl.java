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

package org.sonatype.nexus.plugins.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.plugins.PluginDescriptor;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.sisu.goodies.jmx.MBeans;
import org.sonatype.sisu.goodies.jmx.ObjectNameBuilder;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.ObjectName;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link PluginMBeanInstaller} implementation.
 *
 * @since 2.3
 */
@Named
@Singleton
public class PluginMBeanInstallerImpl
    implements PluginMBeanInstaller
{
    private static final Logger log = LoggerFactory.getLogger(PluginMBeanInstallerImpl.class);

    public static final String DOMAIN = "org.sonatype.nexus.plugins";

    @Override
    public void install(final PluginDescriptor descriptor) {
        checkNotNull(descriptor);

        if (log.isDebugEnabled()) {
            log.debug("Installing: {}", descriptor.getPluginCoordinates());
        }

        ObjectName name = constructName(descriptor);
        MBeans.register(name, new PluginMBeanImpl(descriptor));
    }

    @Override
    public void uninstall(final PluginDescriptor descriptor) {
        checkNotNull(descriptor);

        if (log.isDebugEnabled()) {
            log.debug("Uninstalling: {}", descriptor.getPluginCoordinates());
        }

        ObjectName name = constructName(descriptor);
        MBeans.unregister(name);
    }

    private ObjectName constructName(final PluginDescriptor descriptor) {
        GAVCoordinate gav = descriptor.getPluginCoordinates();

        ObjectNameBuilder builder = new ObjectNameBuilder()
            .domain(DOMAIN)
            .property("groupId", gav.getGroupId())
            .property("artifactId", gav.getArtifactId())
            .property("version", gav.getVersion());

        // These 2 are optional properties
        if (gav.getClassifier() != null) {
            builder.property("classifier", gav.getClassifier());
        }
        if (gav.getType() != null) {
            builder.property("type", gav.getType());
        }

        return builder.buildQuiet();
    }
}
