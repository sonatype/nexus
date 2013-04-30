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
package org.sonatype.nexus.proxy.item.uid;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.proxy.events.AbstractEventInspector;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.NexusInitializedEvent;
import org.sonatype.plexus.appevents.Event;

@Component( role = EventInspector.class, hint = "RepositoryItemUidAttributeEventInspector" )
public class RepositoryItemUidAttributeEventInspector
    extends AbstractEventInspector
    implements EventInspector
{
    @Requirement
    private RepositoryItemUidAttributeManager manager;

    @Override
    public boolean accepts( Event<?> evt )
    {
        final String simpleName = evt.getClass().getName();

        // TODO: nexus-proxy module does not reference plugin manager, so this is a quick'n'dirty workaround for now
        return evt instanceof NexusInitializedEvent // for core
            || StringUtils.equals( simpleName, "org.sonatype.nexus.plugins.events.PluginActivatedEvent" ) // for plugin loaded
            || StringUtils.equals( simpleName, "org.sonatype.nexus.plugins.events.PluginDeactivatedEvent" ); // for plugin unloaded
    }

    @Override
    public void inspect( Event<?> evt )
    {
        manager.reset();
    }
}
