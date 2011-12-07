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
package org.sonatype.nexus.feeds;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

@Component( role = FeedArtifactEventFilter.class )
public class DefaultFeedArtifactEventFilter
    extends AbstractLoggingComponent
    implements FeedArtifactEventFilter
{
    @Requirement
    private NexusItemAuthorizer nexusItemAuthorizer;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    public List<NexusArtifactEvent> filterArtifactEventList( List<NexusArtifactEvent> artifactEvents )
    {
        // make sure we have something to filter
        if( artifactEvents == null )
        {
            return null;
        }
        
        List<NexusArtifactEvent> filteredList = new ArrayList<NexusArtifactEvent>();

        for ( NexusArtifactEvent nexusArtifactEvent : artifactEvents )
        {
            if ( this.filterEvent( nexusArtifactEvent ) )
            {
                filteredList.add( nexusArtifactEvent );
            }
        }

        return filteredList;
    }

    private boolean filterEvent( NexusArtifactEvent event )
    {
        try
        {
            Repository repo = this.repositoryRegistry.getRepository( event.getNexusItemInfo().getRepositoryId() );

            ResourceStoreRequest req = new ResourceStoreRequest( event.getNexusItemInfo().getPath() );

            if ( !this.nexusItemAuthorizer.authorizePath( repo, req, Action.read ) )
            {
                return false;
            }
        }
        catch ( NoSuchRepositoryException e )
        {
            // Can't get repository for artifact, therefore we can't authorize access, therefore you don't see it
            getLogger().debug(
                "Feed entry contained invalid repository id " + event.getNexusItemInfo().getRepositoryId(),
                e );

            return false;
        }

        return true;
    }

}
