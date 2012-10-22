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

import static com.sun.jersey.multipart.Boundary.addBoundary;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.artifact.ArtifactMaven;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveRequest;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveResponse;
import org.sonatype.nexus.client.core.subsystem.artifact.UploadRequest;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.ArtifactCoordinate;
import org.sonatype.nexus.rest.model.ArtifactResolveResource;
import org.sonatype.nexus.rest.model.ArtifactResolveResourceResponse;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.provider.CompletableReader;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

public class JerseyArtifactMaven extends SubsystemSupport<JerseyNexusClient> implements ArtifactMaven {

  private static final MediaType UPLOAD_ARTIFACT_RESPONSE_MEDIATYPE = MediaType.APPLICATION_JSON_TYPE;

  public JerseyArtifactMaven(final JerseyNexusClient nexusClient) {
    super(nexusClient);
    // no extra config needed, this is a core service actually
  }

  @Override
  public ResolveResponse resolve(final ResolveRequest req) {
    final MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add("r", req.getRepositoryId());
    queryParams.add("g", req.getGroupId());
    queryParams.add("a", req.getArtifactId());
    queryParams.add("v", req.getVersion());
    if (req.getPackaging() != null) {
      queryParams.add("p", req.getVersion());
    }
    if (req.getClassifier() != null) {
      queryParams.add("c", req.getClassifier());
    }
    if (req.getExtension() != null) {
      queryParams.add("e", req.getExtension());
    }

    try {
      final ArtifactResolveResource data = getNexusClient().serviceResource("artifact/maven/resolve", queryParams)
          .get(ArtifactResolveResourceResponse.class).getData();

      return new ResolveResponse(data.isPresentLocally(), data.getGroupId(), data.getArtifactId(), data.getVersion(),
          data.getBaseVersion(), data.getClassifier(), data.getExtension(), data.isSnapshot(), data.getSnapshotBuildNumber(),
          data.getSnapshotTimeStamp(), data.getFileName(), data.getSha1(), data.getRepositoryPath());
    } catch (UniformInterfaceException e) {
      if (e.getResponse().getStatus() == 404) {
        return null;
      } else {
        throw e;
      }
    }
  }

  @Override
  public ArtifactCoordinate upload(UploadRequest req) {
    final FormDataMultiPart entity = createEntity(req);
    final ClientResponse response = getNexusClient().serviceResource("artifact/maven/content").type(addBoundary(MULTIPART_FORM_DATA_TYPE))
        .accept(TEXT_HTML)
        .post(ClientResponse.class, entity);
    if (response.getStatus() < 300) {
      return parseEntity(response);
    }

    final String content = response.getEntity(String.class);
    response.close();
    throw new ClientHandlerException("Upload failed due to status code " + response.getStatus() + ".\nResponse: " + content);
  }

  private ArtifactCoordinate parseEntity(final ClientResponse response) {
    final MessageBodyReader<ArtifactCoordinate> mbr = getArtifactCoordsMessageBodyReader(response);
    try {
      ArtifactCoordinate result = mbr.readFrom(ArtifactCoordinate.class, ArtifactCoordinate.class, new Annotation[0],
          UPLOAD_ARTIFACT_RESPONSE_MEDIATYPE,
          response.getHeaders(),
          response.getEntityInputStream());
      if (mbr instanceof CompletableReader) {
        result = ((CompletableReader<ArtifactCoordinate>) mbr).complete(result);
      }
      if (!(result instanceof Closeable)) {
        response.close();
      }
      return result;
    } catch (IOException e) {
      response.close();
      throw new ClientHandlerException(e);
    }
  }

  private MessageBodyReader<ArtifactCoordinate> getArtifactCoordsMessageBodyReader(final ClientResponse response) {
    final MessageBodyReader<ArtifactCoordinate> mbr = response.getClient().getMessageBodyWorkers()
        .getMessageBodyReader(ArtifactCoordinate.class, ArtifactCoordinate.class, new Annotation[0], UPLOAD_ARTIFACT_RESPONSE_MEDIATYPE);
    if (mbr == null) {
      response.close();
      throw new ClientHandlerException("Could not find MessageBodyReader for type ArtifactCoordinate.class and media type "
          + UPLOAD_ARTIFACT_RESPONSE_MEDIATYPE);
    }
    return mbr;
  }

  private FormDataMultiPart createEntity(UploadRequest req) {
    final FormDataMultiPart entity = new FormDataMultiPart().field("r", req.getRepositoryId());
    if (req.isHasPom()) {
      entity.field("hasPom", "true").field("c", defaultIfEmpty(req.getClassifier(), "")).field("e", req.getExtension())
          .bodyPart(new FileDataBodyPart("file", req.getPomFile(), APPLICATION_OCTET_STREAM_TYPE));
    } else {
      entity.field("g", req.getGroupId()).field("a", req.getArtifactId()).field("v", req.getVersion()).field("p", req.getPackaging())
          .field("c", defaultIfEmpty(req.getClassifier(), "")).field("e", req.getExtension());
    }
    entity.bodyPart(new FileDataBodyPart("file", req.getFile(), APPLICATION_OCTET_STREAM_TYPE));
    return entity;
  }
}
