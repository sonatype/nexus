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
package org.sonatype.nexus.proxy.registry;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractNexusTestEnvironment;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;

public class DefaultRepositoryRegistryTest
    extends AbstractNexusTestEnvironment
{

    private RepositoryRegistry repositoryRegistry;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        this.repositoryRegistry = lookup( RepositoryRegistry.class );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testSimple()
        throws Exception
    {
        HostedRepository repoA = createMock( HostedRepository.class );
        HostedRepository repoB = createMock( HostedRepository.class );
        HostedRepository repoC = createMock( HostedRepository.class );

        EasyMock.makeThreadSafe( repoA, true );
        EasyMock.makeThreadSafe( repoB, true );
        EasyMock.makeThreadSafe( repoC, true );

        // id will be called twice
        expect( repoA.getProviderRole() ).andReturn( Repository.class.getName() ).anyTimes();
        expect( repoB.getProviderRole() ).andReturn( Repository.class.getName() ).anyTimes();
        expect( repoC.getProviderRole() ).andReturn( Repository.class.getName() ).anyTimes();
        expect( repoA.getProviderHint() ).andReturn( "maven2" ).anyTimes();
        expect( repoB.getProviderHint() ).andReturn( "maven2" ).anyTimes();
        expect( repoC.getProviderHint() ).andReturn( "maven2" ).anyTimes();

        expect( repoA.getId() ).andReturn( "A" ).anyTimes();
        expect( repoB.getId() ).andReturn( "B" ).anyTimes();
        expect( repoC.getId() ).andReturn( "C" ).anyTimes();
        expect( repoA.getName() ).andReturn( "AName" ).anyTimes();
        expect( repoB.getName() ).andReturn( "BName" ).anyTimes();
        expect( repoC.getName() ).andReturn( "CName" ).anyTimes();

        expect( repoA.getRepositoryContentClass() ).andReturn( new Maven2ContentClass() ).anyTimes();
        expect( repoB.getRepositoryContentClass() ).andReturn( new Maven2ContentClass() ).anyTimes();
        expect( repoC.getRepositoryContentClass() ).andReturn( new Maven2ContentClass() ).anyTimes();

        expect( repoA.getRepositoryKind() ).andReturn( new DefaultRepositoryKind( HostedRepository.class, null ) ).anyTimes();
        expect( repoB.getRepositoryKind() ).andReturn( new DefaultRepositoryKind( HostedRepository.class, null ) ).anyTimes();
        expect( repoC.getRepositoryKind() ).andReturn( new DefaultRepositoryKind( HostedRepository.class, null ) ).anyTimes();

        expect( repoA.adaptToFacet( HostedRepository.class ) ).andReturn( repoA ).anyTimes();
        expect( repoB.adaptToFacet( HostedRepository.class ) ).andReturn( repoB ).anyTimes();
        expect( repoC.adaptToFacet( HostedRepository.class ) ).andReturn( repoC ).anyTimes();
        expect( repoA.adaptToFacet( ProxyRepository.class ) ).andReturn( null ).anyTimes();
        expect( repoB.adaptToFacet( ProxyRepository.class ) ).andReturn( null ).anyTimes();
        expect( repoC.adaptToFacet( ProxyRepository.class ) ).andReturn( null ).anyTimes();

        replay( repoA, repoB, repoC );

        repositoryRegistry.addRepository( repoA );
        repositoryRegistry.addRepository( repoB );
        repositoryRegistry.addRepository( repoC );

        List<String> gl = new ArrayList<String>();
        gl.add( "A" );
        gl.add( "B" );
        gl.add( "C" );

        M2GroupRepository groupRepository = (M2GroupRepository) getContainer().lookup( GroupRepository.class, "maven2" );

        CRepository repoGroupConf = new DefaultCRepository();

        repoGroupConf.setProviderRole( GroupRepository.class.getName() );
        repoGroupConf.setProviderHint( "maven2" );
        repoGroupConf.setId( "ALL" );

        repoGroupConf.setLocalStorage( new CLocalStorage() );
        repoGroupConf.getLocalStorage().setProvider( "file" );

        Xpp3Dom exGroupRepo = new Xpp3Dom( "externalConfiguration" );
        repoGroupConf.setExternalConfiguration( exGroupRepo );
        M2GroupRepositoryConfiguration exGroupRepoConf = new M2GroupRepositoryConfiguration( exGroupRepo );
        exGroupRepoConf.setMemberRepositoryIds( gl );
        exGroupRepoConf.setMergeMetadata( true );

        groupRepository.configure( repoGroupConf );

        repositoryRegistry.addRepository( groupRepository );

        List<Repository> repoMembers =
            repositoryRegistry.getRepositoryWithFacet( "ALL", GroupRepository.class ).getMemberRepositories();

        assertEquals( 3, repoMembers.size() );

        assertEquals( "A", repoMembers.get( 0 ).getId() );
        assertEquals( "B", repoMembers.get( 1 ).getId() );
        assertEquals( "C", repoMembers.get( 2 ).getId() );

        // recheck the group
        GroupRepository group = repositoryRegistry.getRepositoryWithFacet( "ALL", GroupRepository.class );

        assertEquals( 3, group.getMemberRepositories().size() );

        // and remove them all
        List<? extends Repository> repositories = repositoryRegistry.getRepositoriesWithFacet( HostedRepository.class );

        for ( Repository repo : repositories )
        {
            repositoryRegistry.removeRepository( repo.getId() );
        }

        try
        {
            repoMembers =
                repositoryRegistry.getRepositoryWithFacet( "ALL", GroupRepository.class ).getMemberRepositories();

            assertEquals( 0, repoMembers.size() );
        }
        catch ( NoSuchRepositoryException e )
        {
            fail( "Repo group should remain as empty group!" );
        }

        repoMembers = repositoryRegistry.getRepositories();

        assertEquals( 1, repoMembers.size() );

        // the group is there alone, recheck it again
        group = repositoryRegistry.getRepositoryWithFacet( "ALL", GroupRepository.class );

        assertEquals( 0, group.getMemberRepositories().size() );
    }
}
