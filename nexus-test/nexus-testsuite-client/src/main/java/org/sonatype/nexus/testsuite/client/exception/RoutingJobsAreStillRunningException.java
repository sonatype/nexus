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
package org.sonatype.nexus.testsuite.client.exception;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sonatype.nexus.client.core.exception.NexusClientException;
import org.sonatype.sisu.goodies.common.Time;

/**
 * Thrown when waiting for Nexus to not run routing jobs but some jobs are still running in the configured timeout
 * period.
 * 
 * @since 2.4
 */
public class RoutingJobsAreStillRunningException
    extends NexusClientException
{
    public RoutingJobsAreStillRunningException( final Time timeout )
    {
        super( "Nexus was still running routing update jobs, configured timeout of "
            + checkNotNull( timeout.toString() ) );
    }
}
