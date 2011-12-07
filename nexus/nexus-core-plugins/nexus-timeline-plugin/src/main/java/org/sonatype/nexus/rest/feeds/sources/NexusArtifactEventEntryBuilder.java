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

import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Build feeds entry based on maven artifacts( poms and claasifiers)
 * 
 * @author Juven Xu
 */
@Component( role = SyndEntryBuilder.class, hint = "artifact" )
public class NexusArtifactEventEntryBuilder
    extends AbstractNexusItemEventEntryBuilder
{

    @Override
    protected String buildTitle( NexusArtifactEvent event )
    {
        return getGAVString( event );
    }

    @Override
    protected String buildDescriptionMsgItem( NexusArtifactEvent event )
    {
        StringBuilder msg = new StringBuilder();

        msg.append( "The artifact '" );

        msg.append( getGAVString( event ) );

        msg.append( "' in repository '" );

        msg.append( getRepositoryName( event ) );

        msg.append( "'" );

        return msg.toString();
    }

    private Gav buildGAV( NexusArtifactEvent event )
    {
        if ( event.getNexusItemInfo() == null )
        {
            return null;
        }
        try
        {
            Repository repo = getRepositoryRegistry().getRepository( event.getNexusItemInfo().getRepositoryId() );

            if ( MavenRepository.class.isAssignableFrom( repo.getClass() ) )
            {
                return ( (MavenRepository) repo ).getGavCalculator().pathToGav( event.getNexusItemInfo().getPath() );
            }

            return null;
        }
        catch ( NoSuchRepositoryException e )
        {
            getLogger().debug(
                "Feed entry contained invalid repository id " + event.getNexusItemInfo().getRepositoryId(),
                e );

            return null;
        }
    }

    private String getGAVString( NexusArtifactEvent event )
    {
        if ( event.getNexusItemInfo() == null )
        {
            return "unknown:unknown:unknown";
        }

        Gav gav = buildGAV( event );

        if ( gav == null )
        {
            return event.getNexusItemInfo().getPath();
        }

        StringBuilder result = new StringBuilder( gav.getGroupId() )
            .append( ":" ).append( gav.getArtifactId() ).append( ":" ).append(
                gav.getVersion() != null ? gav.getVersion() : "unknown" );

        if ( gav.getClassifier() != null )
        {
            result.append( ":" ).append( gav.getClassifier() );
        }

        return result.toString();

    }

    /**
     * Only when it's a pom file or the file has a classifier will we build the maven artifact entry
     */
    @Override
    public boolean shouldBuildEntry( NexusArtifactEvent event )
    {
        if ( !super.shouldBuildEntry( event ) )
        {
            return false;
        }

        Gav gav = buildGAV( event );

        if ( gav == null )
        {
            return false;
        }

        if ( !StringUtils.isEmpty( gav.getExtension() ) && gav.getExtension().toLowerCase().equals( "pom" ) )
        {
            return true;
        }

        if ( !StringUtils.isEmpty( gav.getClassifier() ) )
        {
            return true;
        }

        return false;
    }
}
