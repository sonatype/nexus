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
package org.sonatype.nexus.proxy.maven.routing.discovery;

import java.util.List;

import org.sonatype.nexus.proxy.maven.MavenProxyRepository;

/**
 * Component used to perform remote content discovery. It relies on multiple {@link RemoteStrategy} to achieve it's
 * goal.
 * 
 * @author cstamas
 * @since 2.4
 */
public interface RemoteContentDiscoverer
{
    /**
     * Performs the discovery using all available {@link RemoteStrategy} in sorted order, and returns the results.
     * 
     * @param mavenProxyRepository to discover remote content.
     * @return the result of discovery,
     */
    DiscoveryResult<MavenProxyRepository> discoverRemoteContent( MavenProxyRepository mavenProxyRepository );

    /**
     * Performs the discovery using the passed in {@link RemoteStrategy} as ordered in list, and returns the results.
     * 
     * @param mavenProxyRepository to discover remote content.
     * @param remoteStrategies the remote strategies to use for discovery, must not be {@code null}.
     * @return the result of discovery,
     */
    DiscoveryResult<MavenProxyRepository> discoverRemoteContent( MavenProxyRepository mavenProxyRepository,
                                                                 final List<RemoteStrategy> remoteStrategies );
}
