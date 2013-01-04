/*
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

/**
 * A Nexus proxy {@link Repository}.
 *
 * @since 2.3
 */
public interface ProxyRepository<T extends ProxyRepository>
    extends Repository<T, ProxyRepositoryStatus>
{

    /**
     * @return proxied URI
     */
    String proxyUri();

    /**
     * Configures repository policy (RELEASES/SNAPSHOTS/MIXED).
     *
     * @param policy repository policy
     * @return itself, for fluent api usage
     */
    T withRepoPolicy( final String policy );

    /**
     * Sets the URI of proxied repository.
     *
     * @param remoteUri of proxied repository
     * @return itself, for fluent api usage
     */
    T asProxyOf( String remoteUri );

    /**
     * Auto block the repository if proxied repository is unresponsive.
     *
     * @return itself, for fluent api usage
     */
    T autoBlock();

    /**
     * Do not auto block the repository if proxied repository is unresponsive.
     *
     * @return itself, for fluent api usage
     */
    T doNotAutoBlock();

    /**
     * Directly blocks the repository (no save required).
     *
     * @return itself, for fluent api usage
     */
    T block();

    /**
     * Directly unblocks the repository (no save required).
     *
     * @return itself, for fluent api usage
     */
    T unblock();

    /**
     * Sets the number of minutes that not found items will be kept in NFC.
     *
     * @param minutes not found cache TTL in minutes
     * @return itself, for fluent api usage
     */
    T withNotFoundCacheTTL( int minutes );

    /**
     * Configures number of minutes items will be cached.
     *
     * @param minutes to be cached
     * @return itself, for fluent api usage
     */
    T withItemMaxAge( int minutes );

}
