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
package org.sonatype.security.ldap.realms;

import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.ldap.AbstractLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.subject.PrincipalCollection;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.security.ldap.dao.LdapDAOException;
import org.sonatype.security.ldap.dao.NoLdapUserRolesFoundException;

//@Component( role = AbstractLdapAuthenticatingRealm.class, hint = "ConfigurableLdapRealm" )
public abstract class AbstractLdapAuthenticatingRealm
    extends AbstractLdapRealm
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement
    private LdapManager ldapManager;

    @Override
    protected AuthenticationInfo queryForAuthenticationInfo( AuthenticationToken token,
        LdapContextFactory ldapContextFactory )
        throws NamingException
    {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();
        String pass = String.valueOf( upToken.getPassword() );

        try
        {
            this.ldapManager.authenticateUser( username, pass );
            return this.buildAuthenticationInfo( username, null );
        }
        catch ( org.sonatype.security.authentication.AuthenticationException e )
        {
            if ( this.logger.isDebugEnabled() )
            {
                this.logger.debug( "User: " + username + " could not be authenticated ", e );
            }
            throw new org.apache.shiro.authc.AuthenticationException( e.getMessage() );
        }
    }

    @Override
    protected AuthorizationInfo queryForAuthorizationInfo( PrincipalCollection principals,
        LdapContextFactory ldapContextFactory )
        throws NamingException
    {
        // only authorize users from this realm
        if( principals.getRealmNames().contains( this.getName() ))
        {
        
            Set<String> roles = new HashSet<String>();
            String username = principals.getPrimaryPrincipal().toString();
            try
            {
                roles = this.ldapManager.getUserRoles(username  );
            }
            catch ( LdapDAOException e )
            {
                this.logger.error( e.getMessage(), e );
                throw new NamingException(e.getMessage());
            }
            catch ( NoLdapUserRolesFoundException e )
            {
                this.logger.debug( "User: " + username + " does not have any ldap roles.", e );
            }
            
            return new SimpleAuthorizationInfo( roles );
        }
        return null;
        
    }

    protected AuthenticationInfo buildAuthenticationInfo( String username, char[] password )
    {
        return new SimpleAuthenticationInfo( username, password, getName() );
    }


    @Override
    public String getName()
    {
        return "LdapAuthenticatingRealm";
    }

    /*
     * (non-Javadoc)
     * @see org.apache.shiro.realm.AuthenticatingRealm#getCredentialsMatcher()
     */
    @Override
    public CredentialsMatcher getCredentialsMatcher()
    {
        return new AllowAllCredentialsMatcher();
    }
}
