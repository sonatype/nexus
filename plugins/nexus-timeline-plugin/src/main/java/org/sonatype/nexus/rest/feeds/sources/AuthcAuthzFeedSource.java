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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.data.MediaType;
import org.sonatype.nexus.feeds.AuthcAuthzEvent;
import org.sonatype.nexus.feeds.FeedRecorder;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

@Component( role = FeedSource.class, hint = "authcAuthz" )
public class AuthcAuthzFeedSource
    extends AbstractFeedSource
{

    public static final String CHANNEL_KEY = "authcAuthz";

    public String getFeedKey()
    {
        return CHANNEL_KEY;
    }

    public String getFeedName()
    {
        return getDescription();
    }

    @Override
    public String getTitle()
    {
        return "Authentication and Authorization";
    }

    @Override
    public String getDescription()
    {
        return "Authentication and Authorization Events";
    }

    public SyndFeed getFeed( Integer from, Integer count, Map<String, String> params )
        throws IOException
    {
        List<AuthcAuthzEvent> items = getFeedRecorder().getAuthcAuthzEvents( null, from, count, null );

        SyndFeedImpl feed = new SyndFeedImpl();

        feed.setTitle( getTitle() );

        feed.setDescription( getDescription() );

        feed.setAuthor( "Nexus " + getApplicationStatusSource().getSystemStatus().getVersion() );

        feed.setPublishedDate( new Date() );

        List<SyndEntry> entries = new ArrayList<SyndEntry>( items.size() );

        for ( AuthcAuthzEvent item : items )
        {
            SyndEntry entry = new SyndEntryImpl();

            if ( FeedRecorder.SYSTEM_AUTHC.equals( item.getAction() ) )
            {
                entry.setTitle( "Authentication" );
            }
            else if ( FeedRecorder.SYSTEM_AUTHZ.equals( item.getAction() ) )
            {
                entry.setTitle( "Authorization" );
            }
            else
            {
                entry.setTitle( item.getAction() );
            }

            SyndContent content = new SyndContentImpl();

            content.setType( MediaType.TEXT_PLAIN.toString() );

            content.setValue( item.getMessage() );

            entry.setPublishedDate( item.getEventDate() );

            entry.setAuthor( feed.getAuthor() );

            entry.setLink( "/" );

            entry.setDescription( content );

            entries.add( entry );

        }

        feed.setEntries( entries );

        return feed;
    }

}
