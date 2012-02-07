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
package org.sonatype.nexus.proxy.walker;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The WalkerContext is usable to control the walk and to share some contextual data during the wak.
 * 
 * @author cstamas
 */
public interface WalkerContext
{
    /**
     * Gets the resource store request that initiated this walk.
     * 
     * @return
     */
    ResourceStoreRequest getResourceStoreRequest();

    /**
     * Will not try to reach remote storage.
     * 
     * @return
     */
    boolean isLocalOnly();

    /**
     * Returns the context.
     * 
     * @return
     */
    Map<String, Object> getContext();

    /**
     * Gets (and creates in null and empty list) the list of processors.
     * 
     * @return
     */
    List<WalkerProcessor> getProcessors();

    /**
     * Sets the list of processors to use.
     * 
     * @param processors
     */
    void setProcessors( List<WalkerProcessor> processors );

    /**
     * Stops the walker with cause.
     * 
     * @param cause
     */
    void stop( Throwable cause );

    /**
     * Returns true is walker is stopped in the middle of walking.
     * 
     * @return
     */
    boolean isStopped();

    /**
     * Returns the cause of stopping this walker or null if none is given.
     * 
     * @return
     */
    Throwable getStopCause();

    /**
     * Returns the filter used in walk or null.
     * 
     * @return the used filter or null.
     */
    WalkerFilter getFilter();

    /**
     * Returns the resource store instance that is/will be walked over.
     * 
     * @return
     */
    Repository getRepository();

    /**
     * Returns the "throttle control" of this context.
     * 
     * @return
     */
    WalkerThrottleController getThrottleController();
}
