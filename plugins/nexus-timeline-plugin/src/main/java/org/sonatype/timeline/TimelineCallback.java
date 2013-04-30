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
package org.sonatype.timeline;

import java.io.IOException;

/**
 * Callback to receive and process timeline records.
 * 
 * @author cstamas
 * @since 3.0
 */
public interface TimelineCallback
{
    /**
     * Method to process next constructed {@link TimelineRecord}. Should return {@code false} if no more records are
     * needed. or simply the processing should stop.
     * 
     * @param rec
     * @return {@code true} if next record is awaited, {@code false} if no more records needed.
     * @throws IOException
     */
    boolean processNext( TimelineRecord rec )
        throws IOException;
}
