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
package org.sonatype.nexus.client.rest.jersey.subsystem;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.condition.NexusStatusConditions;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryGroup;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyRepositoryGroup;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Factory class to create a {@link org.sonatype.nexus.client.core.subsystem.repository.RepositoryGroup} subsystem for the {@link JerseyNexusClient}
 *
 * @since 1.0
 */
@Named
@Singleton
public class JerseyRepositoryGroupSubsystemFactory implements SubsystemFactory<RepositoryGroup, JerseyNexusClient> {
  @Override
  public Condition availableWhen() {
    return NexusStatusConditions.anyModern();
  }

  @Override
  public Class<RepositoryGroup> getType() {
    return RepositoryGroup.class;
  }

  @Override
  public RepositoryGroup create(JerseyNexusClient nexusClient) {
    return new JerseyRepositoryGroup( nexusClient);
  }
}
