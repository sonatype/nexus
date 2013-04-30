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
import org.sonatype.plexus.appevents.AbstractEvent;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plugin.metadata.GAVCoordinate;

/**
 * This {@link Event} is triggered when a Nexus plugin fails during activation.
 */
public final class PluginRejectedEvent
    extends AbstractEvent<NexusPluginManager>
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final GAVCoordinate gav;

    private final Throwable reason;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    public PluginRejectedEvent( final NexusPluginManager component, final GAVCoordinate gav, final Throwable reason )
    {
        super( component );

        this.gav = gav;
        this.reason = reason;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public GAVCoordinate getPluginCoordinates()
    {
        return gav;
    }

    public Throwable getReason()
    {
        return reason;
    }

    public NexusPluginManager getNexusPluginManager()
    {
        return getEventSender();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "gav=" + gav +
            ", reason=" + (reason != null ? reason.getMessage() : null) +
            '}';
    }
}