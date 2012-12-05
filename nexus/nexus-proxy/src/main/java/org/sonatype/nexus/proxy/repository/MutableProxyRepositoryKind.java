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
package org.sonatype.nexus.proxy.repository;

import java.util.Collection;
import java.util.HashSet;

public class MutableProxyRepositoryKind
    implements RepositoryKind
{

    private final ProxyRepository repository;

    private final RepositoryKind hostedKind;

    private final RepositoryKind proxyKind;

    private final HashSet<Class<?>> facets;

    public MutableProxyRepositoryKind( final ProxyRepository repository, final Collection<Class<?>> sharedFacets,
        final RepositoryKind hostedKind, final RepositoryKind proxyKind )
    {
        this.repository = repository;
        this.hostedKind = hostedKind;
        this.proxyKind = proxyKind;
        this.facets = new HashSet<Class<?>>();
        if ( sharedFacets != null )
        {
            facets.addAll( sharedFacets );
        }
    }

    public RepositoryKind getProxyKind()
    {
        return proxyKind;
    }

    public RepositoryKind getHostedKind()
    {
        return hostedKind;
    }

    private boolean isProxy()
    {
        return repository.getRemoteUrl() != null;
    }

    private RepositoryKind getActualRepositoryKind()
    {
        if ( isProxy() )
        {
            return proxyKind;
        }
        else
        {
            return hostedKind;
        }
    }

    @Override
    public Class<?> getMainFacet()
    {
        return getActualRepositoryKind().getMainFacet();
    }

    @Override
    public boolean addFacet( Class<?> f )
    {
        return facets.add( f );
    }

    @Override
    public boolean removeFacet( Class<?> f )
    {
        return facets.remove( f );
    }

    @Override
    public boolean isFacetAvailable( Class<?> f )
    {
        if ( getActualRepositoryKind().isFacetAvailable( f ) )
        {
            return true;
        }

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
