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
package org.sonatype.nexus.rest;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.Repository;

public class NexusCompat
{
    public static CRepository getRepositoryRawConfiguration( Repository repository )
    {
        return ( (CRepositoryCoreConfiguration) repository.getCurrentCoreConfiguration() ).getConfiguration( false );
    }

    /**
     * Returns repository's role.
     * 
     * @param repository
     * @return
     * @deprecated Use repository.getProviderRole() instead!
     */
    public static String getRepositoryProviderRole( Repository repository )
    {
        return repository.getProviderRole();
    }

    /**
     * Returns repository's hint.
     * 
     * @param repository
     * @return
     * @deprecated Use Repository.getProviderHint() instead!
     */
    public static String getRepositoryProviderHint( Repository repository )
    {
        return repository.getProviderHint();
    }

    public static String getRepositoryPolicy( Repository repository )
    {
        if ( repository.getRepositoryKind().isFacetAvailable( MavenRepository.class ) )
        {
            return repository.adaptToFacet( MavenRepository.class ).getRepositoryPolicy().toString();
        }
        else
        {
            return null;
        }
    }
}
