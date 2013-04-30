/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
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

/**
 * A Nexus hosted {@link Repository}.
 *
 * @since 2.3
 */
public interface HostedRepository<T extends HostedRepository>
    extends Repository<T, RepositoryStatus>
{

    T withRepoPolicy( final String policy );

    /**
     * Makes repository a read-only repository.
     *
     * @return itself, for fluent api usage
     */
    T readOnly();

    /**
     * Allow redeploy of items into repository.
     *
     * @return itself, for fluent api usage
     */
    T allowRedeploy();

    /**
     * Do not allow redeployment into repository (items cannot be updated)
     *
     * @return itself, for fluent api usage
     */
    T disableRedeploy();

    /**
     * Enable browsing (see content of repository)
     *
     * @return itself, for fluent api usage
     */
    T enableBrowsing();

    /**
     * Disable browsing (see content of repository).
     *
     * @return itself, for fluent api usage
     */
    T disableBrowsing();

}
