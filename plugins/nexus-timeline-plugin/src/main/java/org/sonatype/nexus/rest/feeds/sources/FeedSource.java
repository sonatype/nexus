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

import java.io.IOException;
import java.util.Map;

import javax.inject.Singleton;

import org.sonatype.plugin.ExtensionPoint;

import com.sun.syndication.feed.synd.SyndFeed;

/**
 * A Feed source. Impementors of this interface produces feeds.
 * 
 * @author cstamas
 */
@ExtensionPoint
@Singleton
public interface FeedSource
{
    /**
     * Returns the channel key that identifies this channel.
     * 
     * @return
     */
    String getFeedKey();

    /**
     * Returns the feed human name.
     * 
     * @return
     */
    String getFeedName();

    /**
     * Returns a Feed Channel.
     * 
     * @return a channel
     * @throws IOException
     */
    SyndFeed getFeed( Integer from, Integer count, Map<String, String> params )
        throws IOException;
}
