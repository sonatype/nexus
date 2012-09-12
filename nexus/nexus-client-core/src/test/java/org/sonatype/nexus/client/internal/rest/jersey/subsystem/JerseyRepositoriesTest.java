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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.util.Set;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.client.core.spi.subsystem.repository.RepositoryFactory;
import org.sonatype.nexus.client.core.subsystem.repository.GenericRepository;
import org.sonatype.nexus.client.core.subsystem.repository.GroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.HostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenGroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyRepositories;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.sisu.litmus.testsupport.inject.InjectedTestSupport;
import com.google.inject.Binder;
import com.sun.jersey.api.client.WebResource;

public class JerseyRepositoriesTest
    extends InjectedTestSupport
{

    private JerseyRepositories underTest;

    @Inject
    private Set<RepositoryFactory> repositoryFactories;

    @Mock
    private WebResource.Builder builder;

    @Mock
    private JerseyNexusClient nexusClient;

    @Before
    public void prepare()
    {
        underTest = new JerseyRepositories( nexusClient, repositoryFactories );
    }

    @Test
    public void createGenericRepository()
    {
        final GenericRepository repository = underTest.create( GenericRepository.class, "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( nullValue() ) );
    }

    @Test
    public void createHostedRepository()
    {
        final HostedRepository repository = underTest.create( HostedRepository.class, "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "hosted" ) );
    }

    @Test
    public void createMavenHostedRepository()
    {
        final MavenHostedRepository repository = underTest.create( MavenHostedRepository.class, "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "hosted" ) );
        assertThat( repository.settings().getProvider(), is( "maven2" ) );
    }

    @Test
    public void createProxyRepository()
    {
        final ProxyRepository repository = underTest.create( ProxyRepository.class, "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "proxy" ) );
    }

    @Test
    public void createMavenProxyRepository()
    {
        final MavenProxyRepository repository = underTest.create( MavenProxyRepository.class, "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "proxy" ) );
        assertThat( repository.settings().getProvider(), is( "maven2" ) );
    }

    @Test
    public void createGroupRepository()
    {
        final GroupRepository repository = underTest.create( GroupRepository.class, "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
    }

    @Test
    public void createMavenGroupRepository()
    {
        final MavenGroupRepository repository = underTest.create( MavenGroupRepository.class, "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getProvider(), is( "maven2" ) );
    }

    @Test
    public void getGenericRepository()
    {
        final RepositoryResource data = new RepositoryResource();
        data.setId( "test" );

        when( nexusClient.serviceResource( "repositories/test" ) ).thenReturn( builder );
        when( builder.get( RepositoryResourceResponse.class ) ).thenReturn( response( data ) );

        final GenericRepository repository = underTest.get( "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
    }

    @Test
    public void getHostedRepository()
    {
        final RepositoryResource data = new RepositoryResource();
        data.setId( "test" );
        data.setRepoType( "hosted" );

        when( nexusClient.serviceResource( "repositories/test" ) ).thenReturn( builder );
        when( builder.get( RepositoryResourceResponse.class ) ).thenReturn( response( data ) );

        final HostedRepository repository = underTest.get( "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "hosted" ) );
    }

    @Test
    public void getMavenHostedRepository()
    {
        final RepositoryResource data = new RepositoryResource();
        data.setId( "test" );
        data.setRepoType( "hosted" );
        data.setProvider( "maven2" );

        when( nexusClient.serviceResource( "repositories/test" ) ).thenReturn( builder );
        when( builder.get( RepositoryResourceResponse.class ) ).thenReturn( response( data ) );

        final HostedRepository repository = underTest.get( "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "hosted" ) );
        assertThat( repository.settings().getProvider(), is( "maven2" ) );
    }

    @Test
    public void getProxyRepository()
    {
        final RepositoryProxyResource data = new RepositoryProxyResource();
        data.setId( "test" );
        data.setRepoType( "proxy" );

        when( nexusClient.serviceResource( "repositories/test" ) ).thenReturn( builder );
        when( builder.get( RepositoryResourceResponse.class ) ).thenReturn( response( data ) );

        final ProxyRepository repository = underTest.get( "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "proxy" ) );
    }

    @Test
    public void getMavenProxyRepository()
    {
        final RepositoryProxyResource data = new RepositoryProxyResource();
        data.setId( "test" );
        data.setRepoType( "proxy" );
        data.setProvider( "maven2" );

        when( nexusClient.serviceResource( "repositories/test" ) ).thenReturn( builder );
        when( builder.get( RepositoryResourceResponse.class ) ).thenReturn( response( data ) );

        final MavenProxyRepository repository = underTest.get( "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getRepoType(), is( "proxy" ) );
        assertThat( repository.settings().getProvider(), is( "maven2" ) );
    }

    @Test
    public void getGroupRepository()
    {
        final RepositoryGroupResource data = new RepositoryGroupResource();
        data.setId( "test" );

        when( nexusClient.serviceResource( "repo_groups/test" ) ).thenReturn( builder );
        when( builder.get( RepositoryGroupResourceResponse.class ) ).thenReturn( response( data ) );

        final GroupRepository repository = underTest.getGroup( "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
    }

    @Test
    public void getMavenGroupRepository()
    {
        final RepositoryGroupResource data = new RepositoryGroupResource();
        data.setId( "test" );
        data.setProvider( "maven2" );

        when( nexusClient.serviceResource( "repo_groups/test" ) ).thenReturn( builder );
        when( builder.get( RepositoryGroupResourceResponse.class ) ).thenReturn( response( data ) );

        final GroupRepository repository = underTest.getGroup( "test" );
        assertThat( repository, is( notNullValue() ) );
        assertThat( repository.settings(), is( notNullValue() ) );
        assertThat( repository.settings().getId(), is( "test" ) );
        assertThat( repository.settings().getProvider(), is( "maven2" ) );
    }

    private RepositoryResourceResponse response( RepositoryBaseResource data )
    {
        final RepositoryResourceResponse response = new RepositoryResourceResponse();
        response.setData( data );
        return response;
    }

    private RepositoryGroupResourceResponse response( RepositoryGroupResource data )
    {
        final RepositoryGroupResourceResponse response = new RepositoryGroupResourceResponse();
        response.setData( data );
        return response;
    }

}
