/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.events;

import org.sonatype.nexus.plugins.NexusPluginManager;
import org.sonatype.nexus.plugins.PluginDescriptor;
import org.sonatype.plexus.appevents.AbstractEvent;
import org.sonatype.plexus.appevents.Event;

/**
 * This {@link Event} is triggered when a Nexus plugin is de-activated.
 */
public final class PluginDeactivatedEvent
    extends AbstractEvent<NexusPluginManager>
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final PluginDescriptor descriptor;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    public PluginDeactivatedEvent( final NexusPluginManager component, final PluginDescriptor descriptor )
    {
        super( component );

        this.descriptor = descriptor;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public PluginDescriptor getPluginDescriptor()
    {
        return descriptor;
    }

    public NexusPluginManager getNexusPluginManager()
    {
        return getEventSender();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "gav=" + descriptor.getPluginCoordinates() +
            '}';
    }
}
