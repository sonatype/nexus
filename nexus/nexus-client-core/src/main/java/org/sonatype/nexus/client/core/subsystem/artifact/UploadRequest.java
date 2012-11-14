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
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

/**
 * Request for uploading an artifact to a maven repository.
 *
 * @since 2.3
 */
public abstract class UploadRequest
{

    private final String repositoryId;

    private final List<File> attachments;

    UploadRequest( final String repositoryId )
    {
        this.repositoryId = checkNotNull( repositoryId );
        attachments = Lists.newArrayList();
    }

    public String repositoryId()
    {
        return repositoryId;
    }

    public static UploadRequestWithCoordinates artifact( final String repositoryId,
                                                         final String groupId,
                                                         final String artifactId,
                                                         final String version,
                                                         final String packaging,
                                                         final String classifier,
                                                         final String extension )
    {
        return new UploadRequestWithCoordinates(
            repositoryId, groupId, artifactId, version, packaging, classifier, extension
        );
    }

    public static UploadRequestWithCoordinates artifact( final String repositoryId,
                                                         final String groupId,
                                                         final String artifactId,
                                                         final String version,
                                                         final String packaging,
                                                         final String extension )
    {
        return artifact(
            repositoryId, groupId, artifactId, version, packaging, null, extension
        );
    }

    public static UploadRequestWithCoordinates artifact( final String repositoryId,
                                                         final String groupId,
                                                         final String artifactId,
                                                         final String version,
                                                         final String extension )
    {
        return artifact(
            repositoryId, groupId, artifactId, version, extension, extension
        );
    }

    public static UploadRequestWithCoordinates artifact( final String repositoryId,
                                                         final String groupId,
                                                         final String artifactId,
                                                         final String version )
    {
        return artifact(
            repositoryId, groupId, artifactId, version, "jar"
        );
    }

    public static UploadRequestWithPom pom( final String repositoryId,
                                            final File pomFile )
    {
        return new UploadRequestWithPom( repositoryId, pomFile );
    }

    public UploadRequest attach( final File file )
    {
        attachments.add( file );
        return this;
    }

    public FormDataMultiPart createUploadEntity()
    {
        final FormDataMultiPart entity = new FormDataMultiPart().field( "r", repositoryId() );
        prepare( entity );
        for ( final File attachment : attachments )
        {
            entity.bodyPart( new FileDataBodyPart( "file", attachment, APPLICATION_OCTET_STREAM_TYPE ) );
        }
        return entity;
    }

    abstract void prepare( FormDataMultiPart entity );

    public static class UploadRequestWithCoordinates
        extends UploadRequest
    {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String packaging;

        private final String classifier;

        private final String extension;

        public UploadRequestWithCoordinates( final String repositoryId,
                                             final String groupId,
                                             final String artifactId,
                                             final String version,
                                             final String packaging,
                                             final String classifier,
                                             final String extension )
        {
            super( repositoryId );
            this.groupId = checkNotNull( groupId );
            this.artifactId = checkNotNull( artifactId );
            this.version = checkNotNull( version );
            this.packaging = checkNotNull( packaging );
            this.classifier = classifier;
            this.extension = checkNotNull( extension );
        }

        @Override
        public void prepare( final FormDataMultiPart entity )
        {
            entity
                .field( "g", groupId )
                .field( "a", artifactId )
                .field( "v", version )
                .field( "p", packaging )
                .field( "c", defaultIfEmpty( classifier, "" ) )
                .field( "e", extension );
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

        public String packaging()
        {
            return packaging;
        }

        public String classifier()
        {
            return classifier;
        }

        public String extension()
        {
            return extension;
        }

    }

    public static class UploadRequestWithPom
        extends UploadRequest
    {

        private final File pom;

        public UploadRequestWithPom( final String repositoryId,
                                     final File pom )
        {
            super( repositoryId );
            this.pom = checkNotNull( pom );
        }

        @Override
        public void prepare( final FormDataMultiPart entity )
        {
            entity
                .field( "hasPom", "true" )
                .bodyPart( new FileDataBodyPart( "file", pom, APPLICATION_OCTET_STREAM_TYPE ) );
        }

        public File pom()
        {
            return pom;
        }

    }

}
