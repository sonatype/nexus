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
package org.sonatype.nexus.proxy.maven;

import java.io.IOException;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.model.Model;
import org.sonatype.nexus.proxy.maven.gav.Gav;

/**
 * An adapter in charge for doing the IO against the storage, hiding the fact where it runs.
 * 
 * @author cstamas
 */
public interface MetadataLocator
{
    /**
     * Calculates the GAV for the request.
     * 
     * @param request
     * @return
     */
    Gav getGavForRequest( ArtifactStoreRequest request );

    /**
     * Constructs a Plugin elem for given request. It returns null if the artifacts's POM pointed out by request is not
     * "maven-plugin".
     * 
     * @param request
     * @return
     * @throws IOException
     */
    Plugin extractPluginElementFromPom( ArtifactStoreRequest request )
        throws IOException;

    /**
     * Constructs a POM for given request.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    Model retrievePom( ArtifactStoreRequest request )
        throws IOException;

    /**
     * Gets the packaging from POM for given request.
     * 
     * @param request
     * @return packaging, or null if not found on the request path
     * @throws IOException
     */
    String retrievePackagingFromPom( ArtifactStoreRequest request )
        throws IOException;

    /**
     * Retrieves the GAV level metadata.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    Metadata retrieveGAVMetadata( ArtifactStoreRequest request )
        throws IOException;

    /**
     * Retrieves the GA level metadata.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    Metadata retrieveGAMetadata( ArtifactStoreRequest request )
        throws IOException;

    /**
     * Retrieves the G level metadata.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    Metadata retrieveGMetadata( ArtifactStoreRequest request )
        throws IOException;

    /**
     * Stores the GAV level metadata.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    void storeGAVMetadata( ArtifactStoreRequest request, Metadata metadata )
        throws IOException;

    /**
     * Stores the GA level metadata.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    void storeGAMetadata( ArtifactStoreRequest request, Metadata metadata )
        throws IOException;

    /**
     * Stores the G level metadata.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    void storeGMetadata( ArtifactStoreRequest request, Metadata metadata )
        throws IOException;
}
