/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.security.ldap.dao;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cstamas
 */
@Component( role = LdapGroupDAO.class )
public class DefaultLdapGroupDAO
    implements LdapGroupDAO
{
    @Requirement
    private LdapUserDAO ldapUserManager;

    private Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    private static boolean isGroupsEnabled( LdapAuthConfiguration configuration )
    {
        return configuration.isLdapGroupsAsRoles();
    }

    public Set<String> getGroupMembership( String username, LdapContext context, LdapAuthConfiguration configuration )
        throws LdapDAOException,
            NoLdapUserRolesFoundException
    {
        
        boolean dynamicGroups = !StringUtils.isEmpty( configuration.getUserMemberOfAttribute() );
        boolean groupsEnabled = isGroupsEnabled( configuration );
        
        Set<String> roleIds = new HashSet<String>();
        if( groupsEnabled )
        {
            if ( !dynamicGroups )
            {
                roleIds = this.getGroupMembershipFromGroups( username, context, configuration );
            }
            else
            {
                try
                {
                    roleIds = this.getGroupMembershipFromUser( username, context, configuration );
                }
                catch ( NoSuchLdapUserException e )
                {
                    throw new NoLdapUserRolesFoundException( username );
                }
            }
    
            if ( roleIds == null || roleIds.isEmpty() )
            {
                throw new NoLdapUserRolesFoundException( username );
            }
        }
        else if ( dynamicGroups && !groupsEnabled )
        {
            throw new NoLdapUserRolesFoundException( username );
        }
        
        return roleIds;
    }

    public Set<String> getAllGroups( LdapContext context, LdapAuthConfiguration configuration )
        throws LdapDAOException
    {
        Set<String> groups = new HashSet<String>();

        if( isGroupsEnabled( configuration ) )
        {
            try
            {
    
                if ( StringUtils.isEmpty( configuration.getUserMemberOfAttribute() ) )
                {
                    // static groups
                    String groupIdAttribute = configuration.getGroupIdAttribute();
                    String groupBaseDn = StringUtils.defaultString( configuration.getGroupBaseDn(), "" );
    
                    String filter = "(objectClass=" + configuration.getGroupObjectClass() + ")";
    
                    getLogger().debug(
                        "Searching for groups in group DN: " + groupBaseDn + "\nUsing filter: \'" + filter + "\'" );
    
                    SearchControls ctls = this.getBaseSearchControls( new String[] { groupIdAttribute }, configuration
                        .isGroupSubtree() );
    
                    groups = this.getGroupIdsFromSearch(
                        context.search( groupBaseDn, filter, ctls ),
                        groupIdAttribute,
                        configuration );
                }
                else
                {
                    // dynamic groups
                    String memberOfAttribute = configuration.getUserMemberOfAttribute();
    
                    String filter = "(objectClass=" + configuration.getUserObjectClass() + ")";
    
                    SearchControls ctls = this.getBaseSearchControls( new String[] { memberOfAttribute }, true );
    
                    String userBaseDn = StringUtils.defaultString( configuration.getUserBaseDn(), "" );
    
                    Set<String> roles = this.getGroupIdsFromSearch(
                        context.search( userBaseDn, filter, ctls ),
                        memberOfAttribute,
                        configuration );
                    
                    for ( String groupDN : roles )
                    {
                        groups.add( this.getGroupFromString( groupDN ) );
                    }
                }
    
            }
            catch ( NamingException e )
            {
                String message = "Failed to get list of groups.";
    
                throw new LdapDAOException( message, e );
            }
        }
        return groups;
    }

    private SearchControls getBaseSearchControls( String[] returningAttributes, boolean subtree )
    {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( returningAttributes );
        ctls.setSearchScope( subtree ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE );
        return ctls;
    }

    private Set<String> getGroupIdsFromSearch( NamingEnumeration searchResults, String groupIdAttribute,
        LdapAuthConfiguration configuration )
        throws NamingException
    {
        Set<String> roles = new LinkedHashSet<String>();

        Map<String, Set<String>> mappings = configuration.getGroupReverseMappings();

        while ( searchResults.hasMoreElements() )
        {
            SearchResult result = (SearchResult) searchResults.nextElement();
            Attribute groupIdAttr = result.getAttributes().get( groupIdAttribute );

            // some users might not have any groups, (no memberOf attribute)
            if ( groupIdAttr != null )
            {
                // get all the attributes
                NamingEnumeration attributes = groupIdAttr.getAll();
                while ( attributes.hasMoreElements() )
                {
                    String group = String.valueOf( attributes.nextElement() );

                    Set<String> mappedRoles = mappings.get( group );
                    if ( mappedRoles == null )
                    {
                        roles.add( group );
                    }
                    else
                    {
                        roles.addAll( mappedRoles );
                    }
                }
            }
        }

        return roles;
    }

    private Set<String> getGroupMembershipFromUser( String username, LdapContext context,
        LdapAuthConfiguration configuration )
        throws LdapDAOException,
            NoSuchLdapUserException
    {
        LdapUser user = this.ldapUserManager.getUser( username, context, configuration );
        return Collections.unmodifiableSet( user.getMembership() );
    }

    private Set<String> getGroupMembershipFromGroups( String username, LdapContext context,
        LdapAuthConfiguration configuration )
        throws LdapDAOException
    {
        String groupIdAttribute = configuration.getGroupIdAttribute();
        String groupMemberAttribute = configuration.getGroupMemberAttribute();
        String groupBaseDn = StringUtils.defaultString( configuration.getGroupBaseDn(), "" );

        String groupMemberFormat = configuration.getGroupMemberFormat();

        String filter = "(&(objectClass=" + configuration.getGroupObjectClass() + ")(&(" + groupIdAttribute + "=*)(";

        if ( groupMemberFormat != null )
        {
            String member = StringUtils.replace( groupMemberFormat, "${username}", username );
            
            if( groupMemberFormat.contains( "${dn}" ) )
            {
                LdapUser user;
                try
                {
                    user = this.ldapUserManager.getUser( username, context, configuration );
                }
                catch ( NoSuchLdapUserException e )
                {
                    String message = "Failed to retrieve role information from ldap for user: " + username;
                    throw new LdapDAOException( message, e );
                }
                member = StringUtils.replace( member, "${dn}",user.getDn()  );   
            }
            
            filter += groupMemberAttribute + "=" + member + ")))";
        }
        else
        {
            filter += groupMemberAttribute + "=" + username + ")))";
        }

        getLogger().debug(
            "Searching for group membership of: " + username + " in group DN: " + groupBaseDn + "\nUsing filter: \'"
                + filter + "\'" );

        try
        {
            SearchControls ctls = this.getBaseSearchControls( new String[] { groupIdAttribute }, configuration
                .isGroupSubtree() );
            
            return this.getGroupIdsFromSearch(
                context.search( groupBaseDn, filter, ctls ),
                groupIdAttribute,
                configuration );

        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve role information from ldap for user: " + username;

            throw new LdapDAOException( message, e );
        }
    }

    private String getGroupFromString( String dnString )
    {
        String result = dnString;
        try
        {
            LdapName dn = new LdapName( dnString );
            result = String.valueOf( dn.getRdn( dn.size() - 1 ).getValue() );
        }
        catch ( InvalidNameException e )
        {
            this.getLogger().debug( "Expected a Group DN but found: " + dnString );
        }
        return result;
    }

    public String getGroupName( String groupId, LdapContext context, LdapAuthConfiguration configuration )
        throws LdapDAOException,
            NoSuchLdapGroupException
    {
        if( !isGroupsEnabled( configuration ) )
        {
            throw new NoSuchLdapGroupException( groupId, groupId );
        }
        
        if ( StringUtils.isEmpty( configuration.getUserMemberOfAttribute() ) )
        {
            // static groups
            String groupIdAttribute = configuration.getGroupIdAttribute();
            String groupBaseDn = StringUtils.defaultString( configuration.getGroupBaseDn(), "" );

            String filter = "(&(objectClass=" + configuration.getGroupObjectClass() + ") (" + groupIdAttribute
                + "=" + groupId +"))";

            SearchControls ctls = this.getBaseSearchControls( new String[] { groupIdAttribute }, configuration
                .isGroupSubtree() );

            Set<String> groups;
            try
            {
                groups = this.getGroupIdsFromSearch(
                    context.search( groupBaseDn, filter, ctls ),
                    groupIdAttribute,
                    configuration );
            }
            catch ( NamingException e )
            {
                throw new LdapDAOException( "Failed to find group: " + groupId, e );
            }
            if ( groups.size() <= 0 )
            {
                throw new NoSuchLdapGroupException( groupId, groupId );
            }
            if ( groups.size() > 1 )
            {
                throw new NoSuchLdapGroupException( groupId, "More then one group found for group: " + groupId );
            }
            else
            {
                return groups.iterator().next();
            }
        }
        else
        {
            String memberOfAttribute = configuration.getUserMemberOfAttribute();

            String filter = "(objectClass=" + configuration.getUserObjectClass() + ")";

            SearchControls ctls = this.getBaseSearchControls( new String[] { memberOfAttribute }, true );

            String userBaseDn = StringUtils.defaultString( configuration.getUserBaseDn(), "" );

            Set<String> roles;
            try
            {
                roles = this.getGroupIdsFromSearch(
                    context.search( userBaseDn, filter, ctls ),
                    memberOfAttribute,
                    configuration );

                for ( String groupDN : roles )
                {
                    if ( groupId.equals( this.getGroupFromString( groupDN ) ) )
                    {
                        return groupId;
                    }
                }
            }
            catch ( NamingException e )
            {
                throw new LdapDAOException( "Failed to find group: " + groupId, e );
            }
        }
        throw new NoSuchLdapGroupException( groupId, groupId );
    }

}
