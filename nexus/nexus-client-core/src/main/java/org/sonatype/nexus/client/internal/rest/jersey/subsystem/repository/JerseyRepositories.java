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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.spi.subsystem.repository.RepositoryFactory;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @since 2.2
 */
public class JerseyRepositories
    extends SubsystemSupport<JerseyNexusClient>
    implements Repositories
{

    private final Set<RepositoryFactory> repositoryFactories;

    public JerseyRepositories( final JerseyNexusClient nexusClient,
                               final Set<RepositoryFactory> repositoryFactories )
    {
        super( nexusClient );
        this.repositoryFactories = repositoryFactories;
    }

    @Override
    public <R extends Repository> R get( final String id )
    {
        checkNotNull( id );

        // TODO the code is inefficient as it will at worst make two calls. Should be fixed when we have a proper repositories REST resource that will return also groups

        ClientResponse response = getNexusClient()
            .serviceResource( "repositories/" + id )
            .get( ClientResponse.class );

        if ( response.getStatus() < 300 )
        {
            return convert( id, response.getEntity( RepositoryResourceResponse.class ).getData() );
        }

        response.close();
        response = getNexusClient()
            .serviceResource( "repo_groups/" + id )
            .get( ClientResponse.class );

        if ( response.getStatus() < 300 )
        {
            return convert( id, response.getEntity( RepositoryGroupResourceResponse.class ).getData() );
        }

        throw new UniformInterfaceException( response );
    }

    @Override
    public <R extends Repository> R get( final Class<R> type, final String id )
    {
        return type.cast( get( id ) );
    }

    @Override
    public Collection<Repository> get()
    {
        final RepositoryListResourceResponse response = getNexusClient()
            .serviceResource( "repositories" )
            .get( RepositoryListResourceResponse.class );

        if ( response == null || response.getData() == null || response.getData().isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Repository> repositories = Lists.newArrayList();

        for ( final RepositoryListResource resource : response.getData() )
        {
            repositories.add( get( resource.getId() ) );
        }

        return repositories;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <R extends Repository> Collection<R> get( final Class<R> type )
    {
        final Collection<Repository> repositories = get();
        final Collection<Repository> filtered = Collections2.filter( repositories, new Predicate<Repository>()
        {
            @Override
            public boolean apply( @Nullable final Repository repository )
            {
                return repository != null && type.isAssignableFrom( repository.getClass() );
            }
        }
        );
        return (Collection<R>) filtered;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <R extends Repository> R create( final Class<R> type, final String id )
    {
        for ( RepositoryFactory repositoryFactory : repositoryFactories )
        {
            if ( repositoryFactory.canCreate( type ) )
            {
                final Repository r = repositoryFactory.create( getNexusClient(), id );
                return (R) r;
            }
        }
        throw new IllegalStateException(
            format( "No repository factory found for repository of type %s", type.getName() )
        );
    }

    @SuppressWarnings( "unchecked" )
    private <R extends Repository> R convert( final String id, final RepositoryBaseResource rbs )
    {
        int currentScore = 0;
        RepositoryFactory factory = null;
        for ( RepositoryFactory repositoryFactory : repositoryFactories )
        {
            final int score = repositoryFactory.canAdapt( rbs );
            if ( score > currentScore )
            {
                factory = repositoryFactory;
            }
        }

        if ( factory == null )
        {
            throw new IllegalStateException(
                format( "No repository factory found for repository with id %s", id )
            );
        }

        return (R) factory.adapt( getNexusClient(), rbs );
    }

}
