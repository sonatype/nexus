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
package org.sonatype.nexus.rest.feeds.sources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.feeds.RepositoryIdTimelineFilter;
import org.sonatype.nexus.timeline.Entry;

import com.google.common.base.Predicate;

/**
 * @author Juven Xu
 */
@Component( role = FeedSource.class, hint = "recentlyCachedOrDeployedArtifacts" )
public class RecentCachedOrDeployedArtifactFeedSource
    extends AbstractNexusItemEventFeedSource
{
    @Requirement( hint = "artifact" )
    private SyndEntryBuilder<NexusArtifactEvent> entryBuilder;

    public static final String CHANNEL_KEY = "recentlyCachedOrDeployedArtifacts";

    public String getFeedKey()
    {
        return CHANNEL_KEY;
    }

    public String getFeedName()
    {
        return getDescription();
    }

    @Override
    public String getDescription()
    {
        return "New artifacts in all Nexus repositories (cached or deployed).";
    }

    @Override
    public List<NexusArtifactEvent> getEventList( Integer from, Integer count, Map<String, String> params )
    {
        final Set<String> repositoryIds = getRepoIdsFromParams( params );

        final Predicate<Entry> filter =
            ( repositoryIds == null || repositoryIds.isEmpty() ) ? null
                : new RepositoryIdTimelineFilter( repositoryIds );

        return getFeedRecorder().getNexusArtifectEvents(
            new HashSet<String>( Arrays.asList( NexusArtifactEvent.ACTION_CACHED, NexusArtifactEvent.ACTION_DEPLOYED ) ),
            from, count, filter );
    }

    @Override
    public String getTitle()
    {
        return "New artifacts";
    }

    @Override
    public SyndEntryBuilder<NexusArtifactEvent> getSyndEntryBuilder( NexusArtifactEvent event )
    {
        return entryBuilder;
    }

}
