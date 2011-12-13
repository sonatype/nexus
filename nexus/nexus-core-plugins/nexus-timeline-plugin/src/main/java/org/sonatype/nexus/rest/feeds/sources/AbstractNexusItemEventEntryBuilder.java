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
package org.sonatype.nexus.rest.feeds.sources;

import java.util.Date;

import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.MediaType;
import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;

/**
 * @author Juven Xu
 */
abstract public class AbstractNexusItemEventEntryBuilder
    extends AbstractLoggingComponent
    implements SyndEntryBuilder<NexusArtifactEvent>
{
    @Requirement
    private Nexus nexus;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    protected Nexus getNexus()
    {
        return nexus;
    }

    protected RepositoryRegistry getRepositoryRegistry()
    {
        return repositoryRegistry;
    }

    public SyndEntry buildEntry( NexusArtifactEvent event )
    {
        SyndEntry entry = new SyndEntryImpl();

        entry.setTitle( buildTitle( event ) );

        entry.setLink( buildLink( event ) );

        entry.setPublishedDate( buildPublishDate( event ) );

        entry.setAuthor( buildAuthor( event ) );

        entry.setDescription( buildDescription( event ) );

        return entry;
    }

    // better to override to provide more detailed information
    protected String buildTitle( NexusArtifactEvent event )
    {
        return event.getAction();
    }

    protected String buildLink( NexusArtifactEvent event )
    {
        return "content/repositories/" + event.getNexusItemInfo().getRepositoryId()
            + event.getNexusItemInfo().getPath();
    }

    protected Date buildPublishDate( NexusArtifactEvent event )
    {
        return event.getEventDate();
    }

    protected String buildAuthor( NexusArtifactEvent event )
    {
        if ( event.getEventContext().containsKey( AccessManager.REQUEST_USER ) )
        {
            return (String) event.getEventContext().get( AccessManager.REQUEST_USER );
        }
        else
        {
            return null;
        }
    }

    protected SyndContent buildDescription( NexusArtifactEvent event )
    {
        SyndContent content = new SyndContentImpl();

        content.setType( MediaType.TEXT_PLAIN.toString() );

        StringBuilder msg = new StringBuilder();

        if ( event.getMessage() != null )
        {
            msg.append( event.getMessage() );
            msg.append( ' ' );
        }

        msg.append( buildDescriptionMsgItem( event ) );

        msg.append( buildDescriptionMsgAction( event ) );

        msg.append( buildDescriptionMsgAuthor( event ) );

        msg.append( buildDescriptionMsgAddress( event ) );

        content.setValue( msg.toString() );

        return content;
    }

    protected String getRepositoryName( NexusArtifactEvent event )
    {
        String repoId = event.getNexusItemInfo().getRepositoryId();

        try
        {
            Repository repository = getRepositoryRegistry().getRepository( repoId );

            return repository.getName();
        }
        catch ( NoSuchRepositoryException e )
        {
            // that's fine, no need to yell, old timeline entries might correspond to long-time removed reposes
            return repoId;
        }
    }

    abstract protected String buildDescriptionMsgItem( NexusArtifactEvent event );

    protected String buildDescriptionMsgAction( NexusArtifactEvent event )
    {
        StringBuilder msg = new StringBuilder( " was " );

        if ( NexusArtifactEvent.ACTION_CACHED.equals( event.getAction() ) )
        {
            msg.append( "cached from remote URL " ).append( event.getNexusItemInfo().getRemoteUrl() ).append( "." );
        }
        else if ( NexusArtifactEvent.ACTION_DEPLOYED.equals( event.getAction() ) )
        {
            msg.append( "deployed." );

        }
        else if ( NexusArtifactEvent.ACTION_DELETED.equals( event.getAction() ) )
        {
            msg.append( "deleted." );
        }
        else if ( NexusArtifactEvent.ACTION_RETRIEVED.equals( event.getAction() ) )
        {
            msg.append( "served downstream." );
        }
        else if ( NexusArtifactEvent.ACTION_BROKEN.equals( event.getAction() ) )
        {
            msg.append( "broken." );

            if ( event.getMessage() != null )
            {
                msg.append( " Details: \n" );

                msg.append( event.getMessage() );

                msg.append( "\n" );
            }
        }
        else if ( NexusArtifactEvent.ACTION_BROKEN_WRONG_REMOTE_CHECKSUM.equals( event.getAction() ) )
        {
            msg.append( "proxied, and the remote repository contains wrong checksum for it." );

            if ( event.getMessage() != null )
            {
                msg.append( " Details: \n" );

                msg.append( event.getMessage() );

                msg.append( "\n" );
            }
        }

        return msg.toString();
    }

    protected String buildDescriptionMsgAuthor( NexusArtifactEvent event )
    {
        final String author = buildAuthor( event );

        if ( author != null )
        {
            return "Action was initiated by user \"" + author + "\".\n";

        }
        return "";
    }

    protected String buildDescriptionMsgAddress( NexusArtifactEvent event )
    {
        if ( event.getEventContext().containsKey( AccessManager.REQUEST_REMOTE_ADDRESS ) )
        {
            return "Request originated from IP address "
                + (String) event.getEventContext().get( AccessManager.REQUEST_REMOTE_ADDRESS ) + ".\n";
        }
        return "";
    }

    public boolean shouldBuildEntry( NexusArtifactEvent event )
    {
        return true;
    }
}
