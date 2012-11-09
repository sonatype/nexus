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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;

@Component( role = RepositoryRegistry.class, hint = "protected" )
public class ProtectedRepositoryRegistry
    extends AbstractLoggingComponent
    implements RepositoryRegistry
{

    @Requirement
    private RepositoryRegistry defaultRepositoryRegistry;

    @Requirement
    private NexusItemAuthorizer nexusItemAuthorizer;

    public void addRepository( Repository repository )
    {
        this.defaultRepositoryRegistry.addRepository( repository );
    }

    public List<String> getGroupsOfRepository( String repositoryId )
    {
        return this.defaultRepositoryRegistry.getGroupsOfRepository( repositoryId );
    }

    public List<GroupRepository> getGroupsOfRepository( Repository repository )
    {
        return this.defaultRepositoryRegistry.getGroupsOfRepository( repository );
    }

    public List<Repository> getRepositories()
    {
        return this.filterRepositoriesList( this.defaultRepositoryRegistry.getRepositories() );
    }

    public <T> List<T> getRepositoriesWithFacet( Class<T> f )
    {
        return this.filterRepositoriesList( this.defaultRepositoryRegistry.getRepositoriesWithFacet( f ), f );
    }

    public Repository getRepository( String repoId )
        throws NoSuchRepositoryException
    {
        Repository repository = this.defaultRepositoryRegistry.getRepository( repoId );
        this.checkAccessToRepository( repository.getId() );
        return repository;
    }

    public <T> T getRepositoryWithFacet( String repoId, Class<T> f )
        throws NoSuchRepositoryException
    {
        T repository = this.defaultRepositoryRegistry.getRepositoryWithFacet( repoId, f );
        this.checkAccessToRepository( repository, f );
        return repository;
    }

    public void removeRepository( String repoId )
        throws NoSuchRepositoryException
    {
        this.checkAccessToRepository( repoId );
        this.defaultRepositoryRegistry.removeRepository( repoId );
    }

    public void removeRepositorySilently( String repoId )
        throws NoSuchRepositoryException
    {
        this.checkAccessToRepository( repoId );
        this.defaultRepositoryRegistry.removeRepositorySilently( repoId );
    }

    public boolean repositoryIdExists( String repositoryId )
    {
        return this.defaultRepositoryRegistry.repositoryIdExists( repositoryId );
    }

    @SuppressWarnings( "unchecked" )
    private <T> List<T> filterRepositoriesList( List<T> repositories, Class<T> facetClass )
    {
        // TODO: there has to be a better way to check to see if one class implements/extends another class
        if ( this.isRepository( facetClass ) )
        {
            return (List<T>) this.filterRepositoriesList( (List<Repository>) repositories );
        }
        else
        {
            this.getLogger().debug(
                "Failed to cast Repository facet class: " + facetClass
                    + " to repository, this list will not be filtered based on the users permissions." );
            return repositories;
        }
    }

    private List<Repository> filterRepositoriesList( List<Repository> repositories )
    {
        // guard against npe
        if ( repositories == null )
        {
            return null;
        }

        List<Repository> filteredRepositories = new ArrayList<Repository>();

        for ( Repository repository : repositories )
        {
            if ( this.nexusItemAuthorizer.isViewable( NexusItemAuthorizer.VIEW_REPOSITORY_KEY, repository.getId() ) )
            {
                filteredRepositories.add( repository );
            }
        }

        return filteredRepositories;
    }

    private void checkAccessToRepository( String repositoryId )
        throws NoSuchRepositoryAccessException
    {
        if ( !this.nexusItemAuthorizer.isViewable( NexusItemAuthorizer.VIEW_REPOSITORY_KEY, repositoryId ) )
        {
            throw new NoSuchRepositoryAccessException( repositoryId );
        }
    }

    private <T> void checkAccessToRepository( T repository, Class facetClass )
        throws NoSuchRepositoryAccessException
    {
        if ( this.isRepository( facetClass ) )
        {
            this.checkAccessToRepository( ((Repository) repository).getId() );
        }
        else
        {
            this.getLogger().debug(
                "Failed to cast Repository facet class: " + facetClass
                    + " to repository, repository cannot be filtered based on the users permissions." );
        }
    }

    @SuppressWarnings( "unchecked" )
    private boolean isRepository( Class facetClass )
    {
        List<Class> interfaces = Arrays.asList( facetClass.getInterfaces() );
        return interfaces.contains( Repository.class ) || Repository.class.equals( facetClass );
    }
}
