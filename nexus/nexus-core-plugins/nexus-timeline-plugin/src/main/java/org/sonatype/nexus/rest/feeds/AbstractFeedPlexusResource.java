/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.rest.feeds;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.feeds.sources.FeedSource;

import com.sun.syndication.feed.synd.SyndFeed;

/**
 * A base Resource class for RSS feeds to be published over restlet.org. It overrides the get() method, the user only
 * needs to implement getChannel() method of ChannelSource interface.
 * 
 * @author cstamas
 * @author dip
 */
public abstract class AbstractFeedPlexusResource
    extends AbstractNexusPlexusResource
{
    private static final String RSS_2_0 = "rss_2.0";

    private static final String ATOM_1_0 = "atom_1.0";

    @Requirement( role = FeedSource.class )
    private Map<String, FeedSource> feeds;

    public List<Variant> getVariants()
    {
        List<Variant> result = super.getVariants();

        // the default resource implementation returns
        // application/xml and application/json
        result.add( new Variant( FeedRepresentation.RSS_MEDIA_TYPE ) );
        result.add( new Variant( FeedRepresentation.ATOM_MEDIA_TYPE ) );
        result.add( new Variant( MediaType.TEXT_XML ) );

        return result;
    }

    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        MediaType mediaType = variant.getMediaType();

        Form params = request.getResourceRef().getQueryAsForm();

        Integer from = null;
        Integer count = null;

        try
        {
            if ( params.getFirstValue( "from" ) != null )
            {
                from = Integer.valueOf( params.getFirstValue( "from" ) );
            }

            if ( params.getFirstValue( "count" ) != null )
            {
                count = Integer.valueOf( params.getFirstValue( "count" ) );
            }
        }
        catch ( NumberFormatException e )
        {
            throw new ResourceException(
                Status.CLIENT_ERROR_BAD_REQUEST,
                "The 'from' and 'count' parameters must be numbers!",
                e );
        }

        Map<String, String> par = params.getValuesMap();

        try
        {
            if ( !MediaType.APPLICATION_JSON.equals( mediaType, true ) )
            {
                SyndFeed feed = getFeed( context, request, getChannelKey( request ), from, count, par );

                if ( FeedRepresentation.ATOM_MEDIA_TYPE.equals( mediaType, true ) )
                {
                    feed.setFeedType( ATOM_1_0 );
                }
                else
                {
                    feed.setFeedType( RSS_2_0 );

                    // set the content type to RSS by default,
                    // however keep text/xml if it was requested this way (IE bug, see NEXUS-991)
                    if ( ! MediaType.TEXT_XML.equals( mediaType, true ) ) {
                      mediaType = FeedRepresentation.RSS_MEDIA_TYPE;
                    }
                }

                feed.setLink( request.getResourceRef().toString() );

                FeedRepresentation representation = new FeedRepresentation( mediaType, feed );

                return representation;
            }
            else
            {
                throw new ResourceException( Status.SERVER_ERROR_NOT_IMPLEMENTED, "Not implemented." );
            }
        }
        catch ( ComponentLookupException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Channel source not found!", e );
        }
        catch ( IOException e )
        {
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL, e );
        }
    }

    protected SyndFeed getFeed( Context context, Request request, String channelKey, Integer from, Integer count,
        Map<String, String> params )
        throws IOException,
            ComponentLookupException
    {
        FeedSource src = feeds.get( channelKey );

        if ( src != null )
        {
            return src.getFeed( from, count, params );
        }

        throw new ComponentLookupException( "Feed with key '" + channelKey + "' not found.", "FeedSource", channelKey );
    }

    protected abstract String getChannelKey( Request request );
}
