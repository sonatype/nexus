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
package org.sonatype.nexus.client.core.subsystem.artifact;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Request for deleting an artifact from a Maven Repository.
 *
 * @since 2.3
 */
public class DeleteRequest
{

    private final String repositoryId;

    private String groupId;

    private String artifactId;

    private String version;

    private String classifier;

    private String extension;

    public DeleteRequest( String repositoryId )
    {
        this.repositoryId = checkNotNull( repositoryId );
    }

    public String repositoryId()
    {
        return repositoryId;
    }

    public String groupId()
    {
        return groupId;
    }

    public String artifactId()
    {
        return artifactId;
    }

    public String version()
    {
        return version;
    }

    public String classifier()
    {
        return classifier;
    }

    public String extension()
    {
        return extension;
    }

    public DeleteRequest withGroupId( final String groupId )
    {
        this.groupId = groupId;
        return this;
    }

    public DeleteRequest withArtifactId( final String artifactId )
    {
        this.artifactId = artifactId;
        return this;
    }

    public DeleteRequest withVersion( final String version )
    {
        this.version = version;
        return this;
    }

    public DeleteRequest withClassifier( final String classifier )
    {
        this.classifier = classifier;
        return this;
    }

    public DeleteRequest withExtension( final String extension )
    {
        this.extension = extension;
        return this;
    }

    public String repositoryPath()
    {
        final StringBuilder sb = new StringBuilder( "/" );
        if ( groupId != null )
        {
            sb.append( groupId );
        }
        if ( artifactId != null )
        {
            checkState( groupId != null, "Group id cannot be null" );
            sb.append( "/" ).append( artifactId );
        }
        if ( version != null )
        {
            checkState( groupId != null, "Group id cannot be null" );
            checkState( artifactId != null, "Artifact id cannot be null" );
            sb.append( "/" ).append( version );
        }
        if ( extension != null )
        {
            checkState( groupId != null, "Group id cannot be null" );
            checkState( artifactId != null, "Artifact id cannot be null" );
            checkState( version != null, "Version cannot be null" );
            sb.append( "/" ).append( artifactId ).append( "-" ).append( version );
            if ( classifier != null )
            {
                sb.append( "-" ).append( classifier );
            }
            sb.append( "." ).append( extension );
        }
        return sb.toString();
    }

}
