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
package org.sonatype.nexus.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.index.ArtifactInfo;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Filters artifact info collection, based on user permissions.
 * 
 * @author cstamas
 */
@Component( role = IndexArtifactFilter.class )
public class DefaultIndexArtifactFilter
    extends AbstractLoggingComponent
    implements IndexArtifactFilter
{
    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private NexusItemAuthorizer nexusItemAuthorizer;

    public Collection<ArtifactInfo> filterArtifactInfos( Collection<ArtifactInfo> artifactInfos )
    {
        if ( artifactInfos == null )
        {
            return null;
        }
        List<ArtifactInfo> result = new ArrayList<ArtifactInfo>( artifactInfos.size() );
        for ( ArtifactInfo artifactInfo : artifactInfos )
        {
            if ( this.filterArtifactInfo( artifactInfo ) )
            {
                result.add( artifactInfo );
            }
        }
        return result;
    }

    public boolean filterArtifactInfo( ArtifactInfo artifactInfo )
    {
        try
        {
            Repository repository = this.repositoryRegistry.getRepository( artifactInfo.repository );

            if ( MavenRepository.class.isAssignableFrom( repository.getClass() ) )
            {
                MavenRepository mr = (MavenRepository) repository;

                Gav gav =
                    new Gav( artifactInfo.groupId, artifactInfo.artifactId, artifactInfo.version,
                        artifactInfo.classifier, mr.getArtifactPackagingMapper().getExtensionForPackaging(
                            artifactInfo.packaging ), null, null, null, false, null, false, null );

                ResourceStoreRequest req = new ResourceStoreRequest( mr.getGavCalculator().gavToPath( gav ) );

                return this.nexusItemAuthorizer.authorizePath( mr, req, Action.read );
            }
            else
            {
                // we are only filtering maven artifacts
                return true;
            }
        }
        catch ( NoSuchRepositoryException e )
        {
            this.getLogger().warn(
                "Repository not found for artifact: " + artifactInfo.groupId + ":" + artifactInfo.artifactId + ":"
                    + artifactInfo.version + " in repository: " + artifactInfo.repository, e );

            // artifact does not exist, filter it out
            return false;
        }
    }
}
