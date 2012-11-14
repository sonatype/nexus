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

import java.io.File;

/**
 * @since 2.3
 */
public class UploadWithPomRequest
{

    private final String repositoryId;

    private final boolean hasPom;

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String packaging;

    private final String classifier;

    private final String extension;

    private final File file;

    private final File pomFile;

    public UploadWithPomRequest( String repositoryId, String groupId, String artifactId, String version,
                                 String packaging,
                                 String classifier,
                                 String extension, File file )
    {
        this.repositoryId = repositoryId;
        this.hasPom = false;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.classifier = classifier;
        this.extension = extension;
        this.file = file;
        this.pomFile = null;
    }

    public UploadWithPomRequest( String repositoryId, File pomFile, String classifier, String extension, File file )
    {
        this.repositoryId = repositoryId;
        this.hasPom = true;
        this.pomFile = pomFile;
        this.classifier = classifier;
        this.extension = extension;
        this.file = file;
        this.groupId = null;
        this.artifactId = null;
        this.version = null;
        this.packaging = null;
    }

    public UploadWithPomRequest( String repositoryId, File pomFile, File file )
    {
        this( repositoryId, pomFile, "jar", file );
    }

    public UploadWithPomRequest( final String repositoryId,
                                 final File pomFile,
                                 final String extension,
                                 final File file )
    {
        this( repositoryId, pomFile, extension, extension, file );
    }

    public UploadWithPomRequest( final String repositoryId,
                                 final String groupId,
                                 final String artifactId,
                                 final String version,
                                 final File file )
    {
        this( repositoryId, groupId, artifactId, version, "jar", file );
    }

    public UploadWithPomRequest( final String repositoryId,
                                 final String groupId,
                                 final String artifactId,
                                 final String version,
                                 final String extension,
                                 final File file )
    {
        this( repositoryId, groupId, artifactId, version, extension, null, extension, file );
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public boolean hasProm()
    {
        return hasPom;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getExtension()
    {
        return extension;
    }

    public File getFile()
    {
        return file;
    }

    public File getPomFile()
    {
        return pomFile;
    }
}
