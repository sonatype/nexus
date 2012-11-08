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
package org.sonatype.nexus.proxy.mapping;

import java.util.List;
import java.util.Map;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.Configurable;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The Interface RequestRepositoryMapper. These mappers are used in Routers, to narrow the number of searched
 * repositories using some technique.
 */
public interface RequestRepositoryMapper
    extends Configurable
{
    /**
     * Returns an unmodifiable Map of mappings.
     * 
     * @return
     */
    Map<String, RepositoryPathMapping> getMappings();

    /**
     * Adds new mapping.
     * 
     * @param mapping
     * @throws ConfigurationException 
     */
    boolean addMapping( RepositoryPathMapping mapping ) throws ConfigurationException;

    /**
     * Removes mapping.
     * 
     * @param id
     */
    boolean removeMapping( String id );

    /**
     * Gets the mapped repositories.
     * 
     * @param request the request
     * @param resolvedRepositories the resolved repositories, possibly a bigger set
     * @return the mapped repositories repoIds
     */
    List<Repository> getMappedRepositories( Repository repository, ResourceStoreRequest request,
                                            List<Repository> resolvedRepositories )
        throws NoSuchResourceStoreException;
}
