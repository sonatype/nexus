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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import javax.ws.rs.core.MultivaluedMap;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.artifact.ArtifactMaven;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveRequest;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveResponse;
import org.sonatype.nexus.client.core.subsystem.artifact.UploadRequest;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.ArtifactCoordinate;
import org.sonatype.nexus.rest.model.ArtifactResolveResource;
import org.sonatype.nexus.rest.model.ArtifactResolveResourceResponse;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;

public class JerseyArtifactMaven
    extends SubsystemSupport<JerseyNexusClient>
    implements ArtifactMaven
{

    public JerseyArtifactMaven( final JerseyNexusClient nexusClient )
    {
        super( nexusClient );
        // no extra config needed, this is a core service actually
    }

    @Override
    public ResolveResponse resolve( final ResolveRequest req )
    {
        final MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add( "r", req.getRepositoryId() );
        queryParams.add( "g", req.getGroupId() );
        queryParams.add( "a", req.getArtifactId() );
        queryParams.add( "v", req.getVersion() );
        if ( req.getPackaging() != null )
        {
            queryParams.add( "p", req.getVersion() );
        }
        if ( req.getClassifier() != null )
        {
            queryParams.add( "c", req.getClassifier() );
        }
        if ( req.getExtension() != null )
        {
            queryParams.add( "e", req.getExtension() );
        }

        try
        {
            final ArtifactResolveResource data =
                getNexusClient().serviceResource( "artifact/maven/resolve", queryParams ).get(
                    ArtifactResolveResourceResponse.class ).getData();

            return new ResolveResponse( data.isPresentLocally(), data.getGroupId(), data.getArtifactId(),
                                        data.getVersion(), data.getBaseVersion(), data.getClassifier(),
                                        data.getExtension(), data.isSnapshot(),
                                        data.getSnapshotBuildNumber(), data.getSnapshotTimeStamp(), data.getFileName(),
                                        data.getSha1(),
                                        data.getRepositoryPath() );
        }
        catch ( UniformInterfaceException e )
        {
            if ( e.getResponse().getStatus() == 404 )
            {
                return null;
            }
            else
            {
                throw e;
            }
        }
    }

  @Override
  public ArtifactCoordinate upload(UploadRequest req) {
    final FormDataMultiPart entity = createEntity(req);
    return getNexusClient().serviceResource("artifact/maven/content").type(MULTIPART_FORM_DATA_TYPE).post(ArtifactCoordinate.class, entity);
  }

  private FormDataMultiPart createEntity(UploadRequest req) {
    if (req.isHasPom()) {
      return new FormDataMultiPart().field("r", req.getRepositoryId()).field("hasPom", "true")
          .field("c", defaultIfEmpty(req.getClassifier(), ""))
          .field("e", req.getExtension()).field("file", req.getPomFile(), APPLICATION_OCTET_STREAM_TYPE)
          .field("file", req.getFile(), APPLICATION_OCTET_STREAM_TYPE);
    }
    return new FormDataMultiPart().field("r", req.getRepositoryId()).field("g", req.getGroupId()).field("a", req.getArtifactId())
        .field("v", req.getVersion()).field("p", req.getPackaging()).field("c", defaultIfEmpty(req.getClassifier(), ""))
        .field("e", req.getExtension())
        .field("file", req.getFile(), APPLICATION_OCTET_STREAM_TYPE);
  }
}
