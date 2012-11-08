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
package org.sonatype.nexus.configuration;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.events.AbstractVetoableEvent;

/**
 * An event fired upon configuration load, when configurable components should validate configs. This is a
 * VetoableEvent, so, save may be vetoed.
 * 
 * @author cstamas
 */
public class ConfigurationValidateEvent
    extends AbstractVetoableEvent<ApplicationConfiguration>
{
    public ConfigurationValidateEvent( ApplicationConfiguration configuration )
    {
        super( configuration );
    }

    public ApplicationConfiguration getConfiguration()
    {
        return getEventSender();
    }
}
