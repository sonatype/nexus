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
package org.sonatype.nexus.proxy.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DefaultRepositoryKind
    implements RepositoryKind
{
    private final Class<?> mainFacet;

    private final Set<Class<?>> facets;

    public DefaultRepositoryKind( Class<?> mainFacet, Collection<Class<?>> facets )
    {
        this.mainFacet = mainFacet;

        this.facets = new HashSet<Class<?>>();

        this.facets.add( mainFacet );

        if ( facets != null )
        {
            this.facets.addAll( facets );
        }
    }

    public Class<?> getMainFacet()
    {
        return mainFacet;
    }

    public boolean isFacetAvailable( Class<?> f )
    {
        for ( Class<?> facet : facets )
        {
            if ( f.isAssignableFrom( facet ) )
            {
                return true;
            }
        }

        return false;
    }
}
