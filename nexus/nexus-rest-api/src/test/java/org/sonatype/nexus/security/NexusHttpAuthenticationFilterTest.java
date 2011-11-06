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
package org.sonatype.nexus.security;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.DelegatingSession;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.security.filter.NexusJSecurityFilter;
import org.sonatype.nexus.security.filter.authc.NexusHttpAuthenticationFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

/**
 * There is a problem either with Shiro (or how we are using it) that effects logging and logging out when using a DelegatingSession (Nexus).</BR>
 * On logout the actual session is expired (see nexus-4378), but the DelegatingSession is not handling this.  Same for login (NEXUS-4257), if the RUNAS user attribute is set to a session that is expired, the user will NOT be able to login.</BR>
 * </BR>
 * NOTE: I don't know why the session has the RUNAS attribute set, we do not use this in nexus.
 */
public class NexusHttpAuthenticationFilterTest
{
    private DelegatingSubject subject;

    private EnterpriseCacheSessionDAO sessionDAO;

    private SimpleSession simpleSession;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private NexusConfiguration nexusConfiguration;

    @Before
    public void bindSubjectToThread()
    {
        // setup a simple realm for authc
        SimpleAccountRealm simpleAccountRealm = new SimpleAccountRealm();
        simpleAccountRealm.addAccount( "anonymous", "anonymous" );
        DefaultSecurityManager securityManager = new DefaultSecurityManager();
        securityManager.setRealm( simpleAccountRealm );

        DefaultSessionManager sessionManager = (DefaultSessionManager) securityManager.getSessionManager();
        sessionDAO = new EnterpriseCacheSessionDAO();
        sessionManager.setSessionDAO( sessionDAO );

        simpleSession = new SimpleSession();
        sessionDAO.create( simpleSession );

        List<PrincipalCollection> principalCollectionList = new ArrayList<PrincipalCollection>();
        principalCollectionList.add( new SimplePrincipalCollection( "other Principal", "some-realm" ) );

        simpleSession.setAttribute( DelegatingSubject.class.getName() + ".RUN_AS_PRINCIPALS_SESSION_KEY",
                                    principalCollectionList );

        DelegatingSession delegatingSession =
            new DelegatingSession( sessionManager, new DefaultSessionKey( simpleSession.getId() ) );

        // set the user

        subject = new DelegatingSubject( new SimplePrincipalCollection( "anonymous", "realmName" ), true, null,
                                         delegatingSession, securityManager );
        ThreadContext.bind( subject );
    }

    @Before
    public void setupMockResponseAndRequest()
    {
        // setup the MOCK
        request = mock( HttpServletRequest.class );
        when( request.getAttribute( eq( NexusHttpAuthenticationFilter.ANONYMOUS_LOGIN ) ) ).thenReturn( "true" );
        when( request.getAttribute( eq( NexusJSecurityFilter.REQUEST_IS_AUTHZ_REJECTED ) ) ).thenReturn( null );
        // end fun with mocks

        response = mock( HttpServletResponse.class );
    }

    @Before
    public void setupNexusConfig()
    {
        nexusConfiguration = Mockito.mock( NexusConfiguration.class );
        Mockito.when( nexusConfiguration.getAnonymousUsername() ).thenReturn( "anonymous" );
        Mockito.when( nexusConfiguration.getAnonymousPassword() ).thenReturn( "anonymous" );
    }

    @After
    public void unbindSubjectFromThread()
    {
        ThreadContext.remove();
    }

    /**
     * Test post handles does not throw an exception if the anonymous users session has expired.
     *
     * @throws Exception
     */
    @Test
    public void testPostHandleForExpiredSessions()
        throws Exception
    {

        // make sure the subject is returned, then expire the session
        assertThat( SecurityUtils.getSubject(), equalTo( (Subject) subject ) );
        subject.getSession().setTimeout( 0 ); // expire the session


        // Verify this does not throw an exception when the session is expired
        NexusHttpAuthenticationFilter filter = new NexusHttpAuthenticationFilter();
        filter.postHandle( request, response );

        // verify the session is nulled out
        assertThat( subject.getSession( false ), nullValue() );
    }

    /**
     * Test that executeAnonymousLogin will attempt to recover after an UnknownSessionException is thrown.
     * @throws Exception
     */
    @Test
    public void testExecuteAnonymousLoginForAnonUserWithInvalidSession()
        throws Exception
    {
        // ******
        // Delete the session directly to mimic what I think is the cause of the Unknown SessionException
        // ******
        sessionDAO.delete( simpleSession );

        // Verify this does not throw an exception when the session is expired
        assertThat( "Anonymous user was not logged in after UnknownSessionException", callExecuteAnonymousLogin() );
    }

    /**
     * Test the typical anonymous login path.  executeAnonymousLogin should return true.
     * @throws Exception
     */
    @Test
    public void testExecuteAnonymousLoginHappyPath()
        throws Exception
    {
        // Verify this does not throw an exception when the session is expired
        assertThat( "Anonymous user should have been logged in", callExecuteAnonymousLogin() );
    }

    /**
     * Calls a protected the method 'executeAnonymousLogin' in NexusHttpAuthenticationFilter, and returns the result.
     * @return
     */
    private boolean callExecuteAnonymousLogin()
    {
        return new NexusHttpAuthenticationFilter()
        {
            // expose protected method
            @Override
            public boolean executeAnonymousLogin( ServletRequest request, ServletResponse response )
            {
                return super.executeAnonymousLogin( request, response );
            }

            @Override
            protected NexusConfiguration getNexusConfiguration()
            {
                return nexusConfiguration;
            }
        }.executeAnonymousLogin( request, response );
        // what a hack... just to call a protected method
    }


}
