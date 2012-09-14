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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryGroup;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryGroupRequest;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryGroupResponse;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static java.net.URLEncoder.encode;

/**
 * @author sherold
 */
public class JerseyRepositoryGroup extends SubsystemSupport<JerseyNexusClient>
    implements RepositoryGroup {

  public JerseyRepositoryGroup(final JerseyNexusClient nexusClient)
  {
    super( nexusClient );
  }

  @Override
  public RepositoryGroupResponse create(RepositoryGroupRequest repositoryGroupRequest) {
    return mapResponse(getNexusClient().serviceResource("repo_groups").post(RepositoryGroupResourceResponse.class, mapRequest(repositoryGroupRequest)).getData());
  }

  private RepositoryGroupResourceResponse mapRequest(RepositoryGroupRequest repositoryGroupRequest) {
    final RepositoryGroupResourceResponse request = new RepositoryGroupResourceResponse();
    final RepositoryGroupResource resource = new RepositoryGroupResource();
    resource.setId(repositoryGroupRequest.getId());
    resource.setName(repositoryGroupRequest.getName());
    resource.setProvider(repositoryGroupRequest.getProvider());
    resource.setExposed(repositoryGroupRequest.getExposed());
    if (repositoryGroupRequest.getMemberRepositoryIds() != null) {
      for (String memberRepositoryId : repositoryGroupRequest.getMemberRepositoryIds()) {
        final RepositoryGroupMemberRepository memberRepository = new RepositoryGroupMemberRepository();
        memberRepository.setId(memberRepositoryId);
        resource.addRepository(memberRepository);
      }
    }
    request.setData(resource);
    return request;
  }

  public void delete(String repositoryGroupId) {
    try {
      getNexusClient().serviceResource("repo_groups/" + encode(repositoryGroupId, "UTF-8")).delete();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Could not encode repository group id.", e);
    }
  }

  private RepositoryGroupResponse mapResponse(RepositoryGroupResource resource) {
    return new RepositoryGroupResponse(resource.getId(), resource.getName(), resource.getProvider(), resource.getProviderRole(), resource.getContentResourceURI(), mapRepositories(resource.getRepositories()));
  }

  private List<String> mapRepositories(List<RepositoryGroupMemberRepository> repositories) {
    if (repositories != null) {
      final List<String> repositoryIds = new ArrayList<String>(repositories.size());
      for (RepositoryGroupMemberRepository memberRepository : repositories) {
        repositoryIds.add(memberRepository.getId());
      }
      return repositoryIds;
    }

    return null;
  }
}
