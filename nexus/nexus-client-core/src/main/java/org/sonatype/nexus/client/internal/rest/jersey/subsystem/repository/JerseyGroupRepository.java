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

import org.sonatype.nexus.client.core.subsystem.repository.GroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryStatus;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;

/**
 * @since 2.3
 */
public class JerseyGroupRepository<T extends GroupRepository>
    extends JerseyRepository<T, RepositoryGroupResource, RepositoryStatus>
    implements GroupRepository<T>
{

    public JerseyGroupRepository( final JerseyNexusClient nexusClient, final String id )
    {
        super( nexusClient, id );
    }

    public JerseyGroupRepository( final JerseyNexusClient nexusClient, final RepositoryGroupResource resource )
    {
        super( nexusClient, resource );
    }

    @Override
    protected RepositoryGroupResource createSettings()
    {
        final RepositoryGroupResource settings = new RepositoryGroupResource();

        settings.setRepoType( "group" );
        settings.setProviderRole( "org.sonatype.nexus.proxy.repository.GroupRepository" );
        settings.setExposed( true );

        return settings;
    }

    private T me()
    {
        return (T) this;
    }

    @Override
    String uri()
    {
        return "repo_groups";
    }

    @Override
    RepositoryGroupResource doGet()
    {
        final RepositoryGroupResourceResponse response = getNexusClient()
            .serviceResource( uri() + "/" + id() )
            .get( RepositoryGroupResourceResponse.class );

        return response.getData();
    }

    @Override
    RepositoryGroupResource doCreate()
    {
        final RepositoryGroupResourceResponse request = new RepositoryGroupResourceResponse();
        request.setData( settings() );

        final RepositoryGroupResourceResponse response = getNexusClient()
            .serviceResource( uri() )
            .post( RepositoryGroupResourceResponse.class, request );

        return response.getData();
    }

    @Override
    RepositoryGroupResource doUpdate()
    {
        final RepositoryGroupResourceResponse request = new RepositoryGroupResourceResponse();
        request.setData( settings() );

        final RepositoryGroupResourceResponse response = getNexusClient()
            .serviceResource( uri() + "/" + id() )
            .put( RepositoryGroupResourceResponse.class, request );

        return response.getData();
    }

    @Override
    public T ofRepositories( final String... memberRepositoryIds )
    {
        for ( final String memberRepositoryId : memberRepositoryIds )
        {
            final RepositoryGroupMemberRepository repository = new RepositoryGroupMemberRepository();
            repository.setId( memberRepositoryId );
            settings().addRepository( repository );
        }
        return me();
    }

}
