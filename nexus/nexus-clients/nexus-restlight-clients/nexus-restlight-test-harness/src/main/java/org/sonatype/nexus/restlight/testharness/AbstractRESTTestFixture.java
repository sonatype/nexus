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
package org.sonatype.nexus.restlight.testharness;

import java.net.ServerSocket;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base implementation for {@link RESTTestFixture} that supplies the methods for managing and retrieving information
 * about the test-harness HTTP {@link Server} instance, the debug flag, and basic expectations about the expected client
 * request headers. Additionally, this base class manages the response headers which will be injected into the response
 * if the client request validates.
 */
public abstract class AbstractRESTTestFixture
    implements RESTTestFixture
{

    private static final int MAX_PORT_TRIES = 10;

    private static final String TEST_PORT_SYSPROP = "test.port";

    private Server server;

    private int port;

    private boolean debugEnabled;

    private String authUser;

    private String authPassword;

    protected AbstractRESTTestFixture( final String user, final String password )
    {
        this.authUser = user;
        this.authPassword = password;
    }

    /**
     * {@inheritDoc}
     */
    public Server getServer()
    {
        return server;
    }

    /**
     * {@inheritDoc}
     */
    public int getPort()
    {
        return port;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public void setDebugEnabled( final boolean debugEnabled )
    {
        this.debugEnabled = debugEnabled;
    }

    /**
     * This is currently broken anyways!
     */
    protected void setupLogging()
    {
//        if ( !LogManager.getRootLogger().getAllAppenders().hasMoreElements() )
//        {
//            LogManager.getRootLogger().addAppender( new ConsoleAppender( new SimpleLayout() ) );
//        }
//
//        if ( isDebugEnabled() )
//        {
//            LogManager.getRootLogger().setLevel( Level.DEBUG );
//        }
//        else
//        {
//            LogManager.getRootLogger().setLevel( Level.INFO );
//        }
    }

    /**
     * {@inheritDoc}
     */
    public void startServer()
        throws Exception
    {
        setupLogging();

        Logger logger = LoggerFactory.getLogger( getClass() );

        String portStr = System.getProperty( TEST_PORT_SYSPROP );

        if ( portStr != null )
        {
            port = Integer.parseInt( portStr );
            logger.info( "Using port: " + port + ", given by system property '" + TEST_PORT_SYSPROP + "'." );
        }
        else
        {
            logger.info( "Randomly looking for an open port..." );

            ServerSocket ss = new ServerSocket( 0 );
            try
            {
                port = ss.getLocalPort();
            }
            finally
            {
                ss.close();
            }
        }

        logger.info( "Starting test server on port: " + port );

        server = new Server( port );

        Constraint constraint = new Constraint();

        constraint.setRoles( new String[] { "allowed" } );
        constraint.setAuthenticate( true );

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint( constraint );
        cm.setPathSpec( "/*" );

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthMethod( NxBasicAuthenticator.AUTH_TYPE );
        securityHandler.setAuthenticator( new NxBasicAuthenticator() );

        HashLoginService loginService = new HashLoginService( "Nexus REST Test Fixture" );

        loginService.putUser( authUser, Credential.getCredential( authPassword ), new String[] { "allowed" } );

        securityHandler.setLoginService( loginService );
        securityHandler.setConstraintMappings( new ConstraintMapping[] { cm } );
        securityHandler.setStrict( false );

        securityHandler.setHandler( getTestHandler() );
        server.setHandler( securityHandler );

        server.start();
    }

    /**
     * {@inheritDoc}
     */
    public void stopServer()
        throws Exception
    {
        LoggerFactory.getLogger( getClass() ).info( "Stopping test server." );

        if ( server != null && server.isStarted() )
        {
            server.stop();
        }
    }

    public String getAuthUser()
    {
        return authUser;
    }

    public void setAuthUser( final String authUser )
    {
        this.authUser = authUser;
    }

    public String getAuthPassword()
    {
        return authPassword;
    }

    public void setAuthPassword( final String authPassword )
    {
        this.authPassword = authPassword;
    }

}
