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

import org.sonatype.nexus.plugins.events.PluginActivatedEvent;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.plexus.appevents.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles events for {@link PluginMBean} installation.
 *
 * @since 2.3
 */
@Named
@Singleton
public class PluginMBeanEventHandler
    implements EventInspector
{
    private final PluginMBeanInstaller installer;

    @Inject
    public PluginMBeanEventHandler(final PluginMBeanInstaller installer) {
        this.installer = checkNotNull(installer);
    }

    @Override
    public boolean accepts(final Event<?> event) {
        return true;
    }

    @Override
    public void inspect(final Event<?> event) {
        if (event instanceof PluginActivatedEvent) {
            handle((PluginActivatedEvent) event);
        }
        // NOTE: There is no uninstall really, deactivate event is never used
        // NOTE: There is however a separate event for active or rejected (failed) which is a PITA
    }

    private void handle(final PluginActivatedEvent event) {
        installer.install(event.getPluginDescriptor());
    }
}
