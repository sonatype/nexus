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
import java.util.Set;

/**
 * A storage we can store and query timestamped data, based on keywords and timestamp. This is a high level API over the
 * two components of timeline under the hood: the persistor, that persists the records in a compact binary format
 * (linearly, as they arrive, is "raw data". Not in timestamp order either!), and the indexer that builds a Lucene index
 * from raw data used for retrievals and searches. This component also hides index failures, and keeps recording the
 * records in case of some Lucene index corruption: no data loss happens, but index must be rebuilt to make it again
 * retrievable as all {@link #retrieve(int, int, Set, Set, TimelineFilter, TimelineCallback)} calls will return empty
 * result sets.
 * 
 * @author cstamas
 */
public interface Timeline
{
    /**
     * Configures and starts component. It has to be invoked before any use of it. If exception is thrown -- failing
     * start -- the component will clean itself up as possible, so no {@link #stop()} is needed to be invoked.
     * 
     * @param config
     * @throws IOException
     */
    void start( TimelineConfiguration config )
        throws IOException;

    /**
     * Stops component. All calls made after invoking this method will actually do nothing.
     * 
     * @throws IOException
     */
    void stop()
        throws IOException;

    /**
     * Adds a record(s) to the Timeline.
     * 
     * @param records the record(s) to add.
     */
    void add( TimelineRecord... records );

    /**
     * Deletes records from timeline that are older than timestamp, and suits the filter (or null for no filtering).
     * 
     * @param timestamp the timestamp to which compared older records should be deleted.
     * @param types the types to purge
     * @param subTypes the subTypes to purge
     * @param filter the filter that should be applied before deletion, or null for no filtering.
     * @return the record count deleted from timeline.
     */
    int purge( long timestamp, Set<String> types, Set<String> subTypes, TimelineFilter filter );

    /**
     * Retrieves records from timeline. The order is desceding, newest is 1st, oldest last.
     * 
     * @param fromItem the number of items you want to skip (paging), 0 for none ("from beginning").
     * @param count the count of records you want to retrieve.
     * @param types the types to purge
     * @param subTypes the subTypes to purge
     * @param filter the filter you want to apply, or null for no filtering.
     * @param callback the callback to receive results, never {@code null}.
     */
    void retrieve( int fromItem, int count, Set<String> types, Set<String> subTypes, TimelineFilter filter,
                   TimelineCallback callback );

    /**
     * Retrieves records from timeline. The order is desceding, newest is 1st, oldest last.
     * 
     * @param fromTime
     * @param toTime
     * @param fromItem the number of items you want to skip (paging), 0 for none ("from beginning").
     * @param count the count of records you want to retrieve.
     * @param types the types to purge
     * @param subTypes the subTypes to purge
     * @param filter the filter you want to apply, or null for no filtering.
     * @param callback the callback to receive results, never {@code null}.
     */
    void retrieve( long fromTime, long toTime, int fromItem, int count, Set<String> types, Set<String> subTypes, TimelineFilter filter,
                   TimelineCallback callback );
}
