/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.proxy.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.nexus.proxy.EventMulticasterComponent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchRepositoryGroupException;
import org.sonatype.nexus.proxy.events.AbstractEvent;
import org.sonatype.nexus.proxy.events.EventListener;
import org.sonatype.nexus.proxy.events.EventMulticaster;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventUpdate;
import org.sonatype.nexus.proxy.events.RepositoryRegistryGroupEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryGroupEventRemove;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.DefaultGroupRepository;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryStatusCheckerThread;

/**
 * The repo registry. It holds handles to registered repositories and sorts them properly. This class is used to get a
 * grip on repositories.
 * <p>
 * Getting reposes from here and changing repo attributes like group, id and rank have no effect on repo registry! For
 * that kind of change, you have to: 1) get repository, 2) remove repository from registry, 3) change repo attributes
 * and 4) add repository.
 * <p>
 * ProximityEvents: this component just "concentrates" the repositiry events of all known repositories by it. It can be
 * used as single point to access all repository events.
 * 
 * TODO this is not a good place to keep group repository management code
 * 
 * @author cstamas
 */
@Component ( role = RepositoryRegistry.class )
public class DefaultRepositoryRegistry
    extends EventMulticasterComponent
    implements RepositoryRegistry, EventListener
{
    /** The repo register, [Repository.getId, Repository] */
    private Map<String, Repository> repositories = new HashMap<String, Repository>();

    /** The repo register, [Repository.getId, Repository] */
    private Map<String, GroupRepository> groupRepositories = new HashMap<String, GroupRepository>();

    /** The repo status checkrs */
    private Map<String, RepositoryStatusCheckerThread> repositoryStatusCheckers = new HashMap<String, RepositoryStatusCheckerThread>();
    
    @Requirement
    private PlexusContainer plexus;

    private void insertRepository( Repository repository, boolean newlyAdded )
    {
        repositories.put( repository.getId(), repository );

        RepositoryStatusCheckerThread thread = new RepositoryStatusCheckerThread( repository );

        repositoryStatusCheckers.put( repository.getId(), thread );

        if ( newlyAdded )
        {
            if ( repository instanceof EventMulticaster )
            {
                ( (EventMulticaster) repository ).addProximityEventListener( this );
            }

            notifyProximityEventListeners( new RepositoryRegistryEventAdd( this, repository ) );
        }

        thread.setDaemon( true );

        thread.start();
    }

    public void addRepository( Repository repository )
    {
        if ( repository instanceof GroupRepository )
        {
            groupRepositories.put( repository.getId(), (GroupRepository) repository );
        }
        else
        {
            insertRepository( repository, true );
        }

        getLogger().info(
            "Added repository ID=" + repository.getId() + " (contentClass="
                + repository.getRepositoryContentClass().getId() + ")" );
    }

    public void updateRepository( Repository repository )
        throws NoSuchRepositoryException
    {
        if ( repositories.containsKey( repository.getId() ) )
        {
            RepositoryStatusCheckerThread thread = repositoryStatusCheckers.get( repository.getId() );

            thread.interrupt();

            insertRepository( repository, false );

            notifyProximityEventListeners( new RepositoryRegistryEventUpdate( this, repository ) );

            getLogger().info(
                "Updated repository ID=" + repository.getId() + " (contentClass="
                    + repository.getRepositoryContentClass().getId() + ")" );
        }
        else
        {
            throw new NoSuchRepositoryException( repository.getId() );
        }
    }

    public void removeRepository( String repoId )
        throws NoSuchRepositoryException
    {
        Repository repository = getRepository( repoId );

        notifyProximityEventListeners( new RepositoryRegistryEventRemove( this, repository ) );
        
        removeRepositorySilently( repoId );
    }

    public void removeRepositorySilently( String repoId )
        throws NoSuchRepositoryException
    {
        Repository repository = (Repository) repositories.get( repoId );

        if ( repository == null )
        {
            throw new NoSuchRepositoryException( repoId );
        }

        for ( GroupRepository group : groupRepositories.values() )
        {
            // XXX mutable group repository
            AbstractGroupRepository hack = (AbstractGroupRepository) group;
            hack.removeMemberRepository( repoId );
        }

        repositories.remove( repository.getId() );

        RepositoryStatusCheckerThread thread = repositoryStatusCheckers.remove( repository.getId() );

        thread.interrupt();

        if ( repository instanceof EventMulticaster )
        {
            ( (EventMulticaster) repository ).removeProximityEventListener( this );
        }

        getLogger().info( "Removed repository id=" + repository.getId() );
    }

    public void addRepositoryGroup( String groupId, List<String> memberRepositories )
        throws NoSuchRepositoryException,
            InvalidGroupingException
    {
        List<String> groupOrder = new ArrayList<String>();

        ContentClass contentClass = null;

        if ( memberRepositories != null )
        {
            for ( String repoId : memberRepositories )
            {
                Repository repository = getRepository( repoId );

                if ( contentClass == null )
                {
                    contentClass = repository.getRepositoryContentClass();
                }
                else if ( !contentClass.isCompatible( repository.getRepositoryContentClass() ) )
                {
                    throw new InvalidGroupingException( contentClass, repository.getRepositoryContentClass() );
                }

                groupOrder.add( repository.getId() );
            }
        }

        GroupRepository group;
        try
        {
            group = (GroupRepository) plexus.lookup( GroupRepository.class, contentClass.getId() );
        }
        catch ( ComponentLookupException e )
        {
            group = new DefaultGroupRepository( contentClass );
        }

        // XXX mutable group repository
        AbstractGroupRepository hack = (AbstractGroupRepository) group;
        hack.setMemberRepositories( memberRepositories );

        groupRepositories.put( groupId, group );

        notifyProximityEventListeners( new RepositoryRegistryGroupEventAdd( this, groupId ) );

        getLogger().info(
            "Added repository group ID=" + groupId + " (contentClass="
                + ( contentClass != null ? contentClass.getId() : "null" )
                + ") with repository members of (in processing order) " + memberRepositories );
    }

    public void removeRepositoryGroup( String groupId )
        throws NoSuchRepositoryGroupException
    {
        removeRepositoryGroup( groupId, false );
    }

    public void removeRepositoryGroup( String groupId, boolean withRepositories )
        throws NoSuchRepositoryGroupException
    {
        GroupRepository group = getRepositoryGroupRepository( groupId );

        notifyProximityEventListeners( new RepositoryRegistryGroupEventRemove( this, groupId ) );

        if ( withRepositories )
        {
            for ( Repository repository : group.getMemberRepositories() )
            {
                try
                {
                    removeRepository( repository.getId() );
                }
                catch ( NoSuchRepositoryException ex )
                {
                    // this should not happen
                    getLogger().warn(
                        "Got NoSuchRepositoryException while removing group " + groupId + ", ignoring it.",
                        ex );
                }
            }
        }
        
        repositories.remove( groupId );
    }

    public Repository getRepository( String repoId )
        throws NoSuchRepositoryException
    {
        if ( repositories.containsKey( repoId ) )
        {
            return repositories.get( repoId );
        }
        else
        {
            throw new NoSuchRepositoryException( repoId );
        }
    }

    public List<Repository> getRepositories()
    {
        return Collections.unmodifiableList( new ArrayList<Repository>( repositories.values() ) );
    }

    public List<String> getRepositoryGroupIds()
    {
        return Collections.unmodifiableList( new ArrayList<String>( groupRepositories.keySet() ) );
    }

    public List<Repository> getRepositoryGroup( String groupId )
        throws NoSuchRepositoryGroupException
    {
        GroupRepository group = getRepositoryGroupRepository( groupId );
        
        return Collections.unmodifiableList( group.getMemberRepositories() );
    }

    private GroupRepository getRepositoryGroupRepository( String groupId )
        throws NoSuchRepositoryGroupException
    {
        GroupRepository group = groupRepositories.get( groupId );
        if ( group != null )
        {
            return group;
        }
        else
        {
            throw new NoSuchRepositoryGroupException( groupId );
        }
    }

    public ContentClass getRepositoryGroupContentClass( String groupId )
        throws NoSuchRepositoryGroupException
    {
        GroupRepository repository = getRepositoryGroupRepository( groupId );
        
        return repository.getRepositoryContentClass();
    }

    public boolean repositoryIdExists( String repositoryId )
    {
        return repositories.containsKey( repositoryId );
    }

    public boolean repositoryGroupIdExists( String repositoryGroupId )
    {
        return groupRepositories.containsKey( repositoryGroupId );
    }

    public List<String> getGroupsOfRepository( String repositoryId )
    {
        ArrayList<String> result = new ArrayList<String>();
        
        GroupRepository group = groupRepositories.get( repositoryId );

        if ( group != null )
        {
            for ( Repository member : group.getMemberRepositories() )
            {
                if ( member.getId().equals( repositoryId ) )
                {
                    result.add( group.getId() );
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Simply "aggregating" repo events, and passing them over.
     */
    public void onProximityEvent( AbstractEvent evt )
    {
        this.notifyProximityEventListeners( evt );
    }

}
