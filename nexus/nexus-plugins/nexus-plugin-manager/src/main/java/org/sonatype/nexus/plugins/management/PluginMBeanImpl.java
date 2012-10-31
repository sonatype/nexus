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

import org.sonatype.nexus.plugins.PluginDescriptor;
import org.sonatype.sisu.goodies.jmx.StandardMBeanSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link PluginMBean} implementation.
 *
 * @since 2.3
 */
public class PluginMBeanImpl
    extends StandardMBeanSupport
    implements PluginMBean
{
    // FIXME: Really should implement a dynamic mbean to expose all of the exposed details

    private final PluginDescriptor descriptor;

    public PluginMBeanImpl(final PluginDescriptor descriptor) {
        super(PluginMBean.class, false);
        this.descriptor = checkNotNull(descriptor);
    }

    @Override
    public String getId() {
        return descriptor.getPluginCoordinates().toString();
    }
}
