/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.security.ldap.realms.connector;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.security.ldap.dao.LdapAuthConfiguration;
import org.sonatype.security.ldap.dao.LdapDAOException;
import org.sonatype.security.ldap.dao.LdapGroupDAO;
import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.dao.LdapUserDAO;
import org.sonatype.security.ldap.dao.NoLdapUserRolesFoundException;
import org.sonatype.security.ldap.dao.NoSuchLdapGroupException;
import org.sonatype.security.ldap.dao.NoSuchLdapUserException;

public class DefaultLdapConnector
    implements LdapConnector
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    
    private LdapUserDAO ldapUserManager;

    private LdapGroupDAO ldapGroupManager;

    private LdapContextFactory ldapContextFactory;
    
    private LdapAuthConfiguration ldapAuthConfiguration;
    
    private String identifier;
    
    public DefaultLdapConnector( String identifier, LdapUserDAO ldapUserManager, LdapGroupDAO ldapGroupManager,
        LdapContextFactory ldapContextFactory, LdapAuthConfiguration ldapAuthConfiguration )
    {
        super();
        this.identifier = identifier;
        this.ldapUserManager = ldapUserManager;
        this.ldapGroupManager = ldapGroupManager;
        this.ldapContextFactory = ldapContextFactory;
        this.ldapAuthConfiguration = ldapAuthConfiguration;
    }

    public Set<String> getUserRoles( String userId )
        throws LdapDAOException, NoLdapUserRolesFoundException
    {
        LdapContext context = null;
        try
        {
            context = this.getLdapContextFactory().getSystemLdapContext();
            return this.getUserRoles( userId, context, this
                .getLdapAuthConfiguration() );
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap user roles for user" + userId;
            throw new LdapDAOException( message, e );
        }
        finally
        {
            this.closeContext( context );
        }
    }

    private Set<String> getUserRoles( String userId, LdapContext context, LdapAuthConfiguration conf )
        throws LdapDAOException, NoLdapUserRolesFoundException
    {
        Set<String> roles = new HashSet<String>();

        if ( this.getLdapAuthConfiguration().isLdapGroupsAsRoles() )
        {
            roles.addAll( this.getGroupMembership( userId, context, conf ) );
        }

        return roles;
    }

    public SortedSet<LdapUser> getAllUsers()
        throws LdapDAOException
    {
        return this.getUsers( -1 );
    }

    public SortedSet<LdapUser> getUsers( int count )
        throws LdapDAOException
    {
        LdapContext context = null;
        try
        {
            context = this.getLdapContextFactory().getSystemLdapContext();
            LdapAuthConfiguration conf = this.getLdapAuthConfiguration();

            SortedSet<LdapUser> users = this.ldapUserManager.getUsers( context, conf, count );

            for ( LdapUser ldapUser : users )
            {
                if ( this.getLdapAuthConfiguration().isLdapGroupsAsRoles() )
                {
                    try
                    {
                        ldapUser.setMembership( this.getGroupMembership( ldapUser.getUsername(), context, conf ) );
                    }
                    catch ( NoLdapUserRolesFoundException e )
                    {
                        this.logger.debug( "No roles found for user: "+ ldapUser.getUsername() );
                    }
                }
            }
            return users;
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap information for users.";
            throw new LdapDAOException( message, e );
        }
        finally
        {
            this.closeContext( context );
        }
    }

    public LdapUser getUser( String username )
        throws NoSuchLdapUserException,
            LdapDAOException
    {
        LdapContext context = null;
        try
        {
            context = this.getLdapContextFactory().getSystemLdapContext();    
            LdapAuthConfiguration conf = this.getLdapAuthConfiguration();

            LdapUser ldapUser = this.ldapUserManager.getUser( username, context, conf );

            if ( this.getLdapAuthConfiguration().isLdapGroupsAsRoles() )
            {
                try
                {
                    ldapUser.setMembership( this.getGroupMembership( ldapUser.getUsername(), context, conf ) );
                }
                catch ( NoLdapUserRolesFoundException e )
                {
                    this.logger.debug( "No roles found for user: "+ username );
                }
            }

            return ldapUser;
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap information for users.";
            throw new LdapDAOException( message, e );
        }
        finally
        {
            this.closeContext( context );
        }
    }

    public SortedSet<LdapUser> searchUsers( String username )
        throws LdapDAOException
    {
        LdapContext context = null;
        try
        {
            context = this.getLdapContextFactory().getSystemLdapContext();
            LdapAuthConfiguration conf = this.getLdapAuthConfiguration();

            // make sure the username is at least an empty string
            if ( username == null )
            {
                username = "";
            }

            SortedSet<LdapUser> users = this.ldapUserManager.getUsers( username + "*", context, conf, -1 );

            for ( LdapUser ldapUser : users )
            {
                if ( this.getLdapAuthConfiguration().isLdapGroupsAsRoles() )
                {
                    try
                    {
                        ldapUser.setMembership( this.getGroupMembership( ldapUser.getUsername(), context, conf ) );
                    }
                    catch ( NoLdapUserRolesFoundException e )
                    {
                       this.logger.debug( "No roles found for user: "+ username );
                    }
                }
            }
            return users;
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap information for users.";
            throw new LdapDAOException( message, e );
        }
        finally
        {
            this.closeContext( context );
        }
    }

    private Set<String> getGroupMembership( String username, LdapContext context, LdapAuthConfiguration conf )
        throws LdapDAOException, NoLdapUserRolesFoundException
    {
        return this.ldapGroupManager.getGroupMembership( username, context, conf );
    }

    public SortedSet<String> getAllGroups()
        throws LdapDAOException
    {
        LdapContext context = null;
        
        try
        {
            SortedSet<String> results = new TreeSet<String>();

            context = this.getLdapContextFactory().getSystemLdapContext();
            LdapAuthConfiguration conf = this.getLdapAuthConfiguration();

            results.addAll( this.ldapGroupManager.getAllGroups( context, conf ) );

            return results;
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap information for users.";
            throw new LdapDAOException( message, e );
        }
        finally
        {
            this.closeContext( context );
        }
    }

    public String getGroupName( String groupId )
        throws LdapDAOException,
            NoSuchLdapGroupException
    {
        LdapContext context = null;
        
        try
        {
            context = this.getLdapContextFactory().getSystemLdapContext();
            LdapAuthConfiguration conf = this.getLdapAuthConfiguration();

            return this.ldapGroupManager.getGroupName( groupId, context, conf );
        }
        catch ( NamingException e )
        {
            String message = "Failed to retrieve ldap information for users.";
            throw new LdapDAOException( message, e );
        }
        finally
        {
            this.closeContext( context );
        }
    }
    
    public LdapContextFactory getLdapContextFactory()
    {
        return ldapContextFactory;
    }

    private LdapAuthConfiguration getLdapAuthConfiguration()
    {
        return ldapAuthConfiguration;
    }

    public String getIdentifier()
    {
        return this.identifier;
    }
    
    private void closeContext( LdapContext context )
    {
        try
        {
            if( context != null)
            {
                context.close();
            }
        }
        catch ( NamingException e )
        {
            this.logger.debug( "Error closing connection: "+ e.getMessage(), e );
        }
    }
}
