/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.proxy.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.jsecurity.subject.Subject;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.target.TargetMatch;
import org.sonatype.nexus.proxy.target.TargetSet;
import org.sonatype.security.SecuritySystem;

/**
 * Default implementation of Nexus Authorizer, that relies onto JSecurity.
 */
@Component( role = NexusItemAuthorizer.class )
public class DefaultNexusItemAuthorizer
    extends AbstractLogEnabled
    implements NexusItemAuthorizer
{
    @Requirement
    private SecuritySystem securitySystem;

    @Requirement
    private RepositoryRegistry repoRegistry;

    public boolean authorizePath( Repository repository, ResourceStoreRequest request, Action action )
    {
        Set<Repository> repos = new HashSet<Repository>();
        repos.add( repository );
        repos.addAll( getGroupsOfRepo( repository ) );

        for ( Repository repo : repos )
        {
            if ( authorizePath( repo.getTargetsForRequest( request ), action ) )
            {
                return true;
            }
        }

        return false;
    }

    public boolean authorizePermission( String permission )
    {
        return isPermitted( Collections.singletonList( permission ) );
    }

    protected Set<GroupRepository> getGroupsOfRepo( Repository repository )
    {
        List<GroupRepository> groups = repoRegistry.getGroupsOfRepository( repository );
        Set<GroupRepository> all = new HashSet<GroupRepository>();
        all.addAll( groups );

        for ( GroupRepository group : groups )
        {
            all.addAll( getGroupsOfRepo( group ) );
        }

        return all;
    }

    // ===

    public TargetSet getGroupsTargetSet( Repository repository, ResourceStoreRequest request )
    {
        TargetSet targetSet = new TargetSet();

        for ( GroupRepository group : repoRegistry.getGroupsOfRepository( repository ) )
        {
            // are the perms transitively inherited from the groups where it is member?
            // if ( !group.isExposed() )
            TargetSet groupMatched = group.getTargetsForRequest( request );

            targetSet.addTargetSet( groupMatched );

            // now that we have groups of groups, this needs to be a recursive check
            targetSet.addTargetSet( getGroupsTargetSet( group, request ) );
        }

        return targetSet;
    }

    public boolean authorizePath( TargetSet matched, Action action )
    {
        // did we hit repositories at all?
        if ( matched.getMatchedRepositoryIds().size() > 0 )
        {
            // we had reposes affected, check the targets
            // make perms from TargetSet
            return isPermitted( getTargetPerms( matched, action ) );
        }
        else
        {
            // we hit no repos, it is a virtual path, allow access
            return true;
        }
    }

    protected List<String> getTargetPerms( TargetSet matched, Action action )
    {
        List<String> perms = new ArrayList<String>( matched.getMatches().size() );

        // nexus : 'target' + targetId : repoId : read
        for ( TargetMatch match : matched.getMatches() )
        {
            perms.add( "nexus:target:" + match.getTarget().getId() + ":" + match.getRepository().getId() + ":" + action );
        }

        return perms;
    }

    protected boolean isPermitted( List<String> perms )
    {
        // Get the current user
        Subject subject = this.securitySystem.getSubject();

        if ( this.securitySystem.isSecurityEnabled() )
        {
            if ( subject != null )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Checking isPermitted() with perms: " + perms.toString() );
                }

                // And finally check each of the target permissions and see if the user
                // has access, all it takes is one
                for ( String perm : perms )
                {
                    if ( subject.isPermitted( perm ) )
                    {
                        return true;
                    }
                }

                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Subject is authenticated, but has none of the needed permissions, rejecting." );
                }

                return false;
            }
            else
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Subject is not authenticated, rejecting." );
                }

                // security is enabled, but we have nobody authenticated? Fail!
                return false;
            }
        }
        else
        {
            // sec is disabled, simply say YES
            return true;
        }
    }

    public boolean isViewable( String objectType, String objectId )
    {
        return authorizePermission( "nexus:view:" + objectType + ":" + objectId );
    }
}
