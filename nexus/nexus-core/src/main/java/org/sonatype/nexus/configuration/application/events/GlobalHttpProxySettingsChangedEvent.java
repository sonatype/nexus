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
package org.sonatype.nexus.configuration.application.events;

import org.sonatype.nexus.configuration.application.GlobalHttpProxySettings;
import org.sonatype.plexus.appevents.AbstractEvent;

/**
 * Event fired when global HTTP Proxy settings are changed (within configuration change). The settings carried in
 * this event will reflect the NEW values, but if you have the {@link GlobalHttpProxySettings} component, you can
 * query it too <em>after</em> you received this event .
 * 
 * @since 2.0
 */
public class GlobalHttpProxySettingsChangedEvent
    extends AbstractEvent<GlobalHttpProxySettings>
{

    public GlobalHttpProxySettingsChangedEvent( GlobalHttpProxySettings settings )
    {
        super( settings );
    }

    public GlobalHttpProxySettings getGlobalHttpProxySettings()
    {
        return getEventSender();
    }

}
