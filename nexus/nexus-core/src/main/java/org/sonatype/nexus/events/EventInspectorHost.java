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
package org.sonatype.nexus.events;

import org.sonatype.plexus.appevents.EventListener;

/**
 * A component that receives events and simply re-emits then to the registered EventInspectors.
 * 
 * @author cstamas
 */
public interface EventInspectorHost
    extends EventListener
{
    /**
     * Shuts down event inspector cleanly (mainly maintains the thread pool).
     */
    void shutdown();

    /**
     * Debug only: returns true if it is "calm period", and no async event inspector is running in the thread pool.
     * False otherwise.
     * 
     * @return
     */
    boolean isCalmPeriod();
}
