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
package org.sonatype.nexus.client.core.subsystem.repository;

import java.util.List;

public class RepositoryGroupResponse {
  private final String id;
  private final String name;
  private final String provider;
  private final String providerRole;
  private final String contentResourceURI;
  private final List<String> memberRepositoryIds;

  public RepositoryGroupResponse(String id, String name, String provider, String providerRole, String contentResourceURI, List<String> memberRepositoryIds) {

    this.id = id;
    this.name = name;
    this.provider = provider;
    this.providerRole = providerRole;
    this.contentResourceURI = contentResourceURI;
    this.memberRepositoryIds = memberRepositoryIds;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderRole() {
    return providerRole;
  }

  public String getContentResourceURI() {
    return contentResourceURI;
  }

  public List<String> getMemberRepositoryIds() {
    return memberRepositoryIds;
  }
}
