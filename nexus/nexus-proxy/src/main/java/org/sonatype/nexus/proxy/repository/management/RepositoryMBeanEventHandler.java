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

package org.sonatype.nexus.proxy.repository.management;

import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.plexus.appevents.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles events for {@link RepositoryMBean} installation.
 *
 * @since 2.2
 */
@Named
@Singleton
public class RepositoryMBeanEventHandler
    implements EventInspector
{
    private final RepositoryMBeanInstaller installer;

    @Inject
    public RepositoryMBeanEventHandler(final RepositoryMBeanInstaller installer) {
        this.installer = checkNotNull(installer);
    }

    @Override
    public boolean accepts(final Event<?> event) {
        return true;
    }

    @Override
    public void inspect(final Event<?> event) {
        if (event instanceof RepositoryRegistryEventAdd) {
            handle((RepositoryRegistryEventAdd) event);
        }
        else if (event instanceof RepositoryRegistryEventRemove) {
            handle((RepositoryRegistryEventRemove) event);
        }
    }

    private void handle(final RepositoryRegistryEventAdd event) {
        installer.install(event.getRepository());
    }

    private void handle(final RepositoryRegistryEventRemove event) {
        installer.uninstall(event.getRepository());
    }
}
