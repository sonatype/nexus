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
package org.sonatype.nexus.feeds;

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.timeline.Entry;

import com.google.common.base.Predicate;

/**
 * Timeline filter that filters by repository IDs.
 *
 * @author: cstamas
 * @since 2.0
 */
public class RepositoryIdTimelineFilter
    implements Predicate<Entry>
{
    private final Set<String> repositoryIds;

    public RepositoryIdTimelineFilter( String repositoryId )
    {
        this.repositoryIds = new HashSet<String>();

        this.repositoryIds.add( repositoryId );
    }

    public RepositoryIdTimelineFilter( Set<String> repositoryIds )
    {
        this.repositoryIds = repositoryIds;
    }

    public boolean apply( final Entry hit )
    {
        return ( hit.getData().containsKey( DefaultFeedRecorder.REPOSITORY ) && repositoryIds.contains( hit.getData().get(
            DefaultFeedRecorder.REPOSITORY ) ) );
    }
}
