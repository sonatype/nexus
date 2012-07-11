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
package org.sonatype.security.ldap.realms;

import java.net.MalformedURLException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.sonatype.security.ldap.realms.persist.LdapConfiguration;
import org.sonatype.security.ldap.realms.tools.LdapURL;

import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;

@Component( role = LdapContextFactory.class, hint = "PlexusLdapContextFactory" )
public class PlexusLdapContextFactory
    implements LdapContextFactory
{

    @Requirement
    private Logger logger;

    @Requirement
    private LdapConfiguration ldapConfig;

    /**
     * The Sun LDAP property used to enable connection pooling. This is used in the default implementation to enable
     * LDAP connection pooling.
     */
    protected static final String SUN_CONNECTION_POOLING_PROPERTY = "com.sun.jndi.ldap.connect.pool";
    protected static final String SUN_CONNECTION_POOLING_PROTOCOL_PROPERTY = "com.sun.jndi.ldap.connect.pool.protocol";

    protected String contextFactoryClassName = "com.sun.jndi.ldap.LdapCtxFactory";

    private boolean pooling = true;

    public LdapContext getSystemLdapContext()
        throws NamingException
    {   
        // if we do not have a connectionInfo object in the config we are in an unconfigured state. A naming exception
        // is the best we can do.
        if ( ldapConfig.readConnectionInfo() == null )
        {
            throw new NamingException( "LDAP Realm is not configured." );
        }
        
        return this.getLdapContext( ldapConfig.readConnectionInfo().getSystemUsername(), ldapConfig
            .readConnectionInfo().getSystemPassword(), true );
    }

    @Override
    public LdapContext getLdapContext( String username, String password )
        throws NamingException
    {
        return this.getLdapContext( username, password, false );
    }
    
    
    public LdapContext getLdapContext( Object principal, Object credentials )
        throws NamingException
    {
        return this.getLdapContext( principal.toString(), credentials.toString(), false );
    }

    private LdapContext getLdapContext( String username, String password, boolean systemLogin )
        throws NamingException
    {
        
        // if we do not have a connectionInfo objcet in the config we are in an unconfigured state. A naming exception
        // is the best we can do.
        if ( ldapConfig.readConnectionInfo() == null )
        {
            throw new NamingException( "LDAP Realm is not configured." );
        }

        CConnectionInfo connInfo = this.ldapConfig.readConnectionInfo();

        if ( connInfo.getSearchBase() == null )
        {
            throw new IllegalStateException( "A search base must be specified." );
        }

        String url;
        try
        {
            url = new LdapURL( connInfo.getProtocol(), connInfo.getHost(), connInfo.getPort(), connInfo.getSearchBase() )
                .toString();
        }
        catch ( MalformedURLException e )
        {
            // log an error, because the user could still log in and fix the config.
            this.logger.error( "LDAP Configuration is Invalid." );
            throw new NamingException( "Invalid LDAP URL: " + e.getMessage() );
        }

        if ( url == null )
        {
            throw new IllegalStateException( "An LDAP URL must be specified of the form ldap://<hostname>:<port>" );
        }

        // if (username != null && principalSuffix != null) {
        // username += principalSuffix;
        // }

        Hashtable<String, String> env = new Hashtable<String, String>();

        // if the Authentication scheme is none, and this is not the system ctx we need to set the scheme to 'simple'
        if ( "none".equals( connInfo.getAuthScheme() ) && !systemLogin )
        {
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        }
        else
        {
            env.put( Context.SECURITY_AUTHENTICATION, connInfo.getAuthScheme() );
        }
        // username
        if ( username != null )
        {
            env.put( Context.SECURITY_PRINCIPAL, username );
        }
        // password
        if ( password != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, password );
        }

        if ( connInfo.getRealm() != null )
        {
            env.put( "java.naming.security.sasl.realm", connInfo.getRealm() );
        }
        env.put( Context.INITIAL_CONTEXT_FACTORY, contextFactoryClassName );
        env.put( Context.PROVIDER_URL, url );
        // env.put(Context.REFERRAL, referral);

        // Only pool connections for system contexts
        if ( pooling && systemLogin )
        {
            // Enable connection pooling
            env.put( SUN_CONNECTION_POOLING_PROPERTY, "true" );
            // Enable pooling for plain and ssl connections
            env.put( SUN_CONNECTION_POOLING_PROTOCOL_PROPERTY, "plain ssl" );
        }

        // if (additionalEnvironment != null) {
        // env.putAll(additionalEnvironment);
        // }

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Initializing LDAP context using URL [" + url + "] and username [" + username + "] "
                + "with pooling [" + ( pooling ? "enabled" : "disabled" ) + "]" );
        }

        return new InitialLdapContext( env, null );
    }

    /**
     * @param ldapConfig the ldapConfig to set
     */
    public void setLdapConfiguration( LdapConfiguration ldapConfig )
    {
        this.ldapConfig = ldapConfig;
    }
}
