/*
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
package org.sonatype.nexus.restlight.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonatype.nexus.restlight.testharness.AbstractRESTTest;
import org.sonatype.nexus.restlight.testharness.ConversationalFixture;
import org.sonatype.nexus.restlight.testharness.DELETEFixture;
import org.sonatype.nexus.restlight.testharness.GETFixture;
import org.sonatype.nexus.restlight.testharness.POSTFixture;
import org.sonatype.nexus.restlight.testharness.PUTFixture;
import org.sonatype.nexus.restlight.testharness.RESTTestFixture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreClientTest
    extends AbstractRESTTest
{

    private final ConversationalFixture fixture = new ConversationalFixture( getExpectedUser(), getExpectedPassword() );

    @BeforeClass
    public static void setUpClass()
        throws Exception
    {
        System.getProperties().put( TEST_NX_API_VERSION_SYSPROP, "1.4" );
    }

    @Override
    protected RESTTestFixture getTestFixture()
    {
        return fixture;
    }

    @Test
    public void listUser()
        throws Exception
    {
        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        GETFixture userListGetFixture = new GETFixture( getExpectedUser(), getExpectedPassword() );
        userListGetFixture.setExactURI( CoreClient.USER_PATH );
        userListGetFixture.setResponseDocument( readTestDocumentResource( "user-list.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( userListGetFixture );
        fixture.setConversation( conversation );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        List<User> users = client.listUser();

        List<RESTTestFixture> unused = fixture.verifyConversationWasFinished();
        if ( unused != null && !unused.isEmpty() )
        {
            System.out.println( unused );
            fail( "Conversation was not finished. Didn't traverse:\n" + unused );
        }

        assertNotNull( users );

        assertEquals( 4, users.size() );
        assertEquals( "http://localhost:8081/nexus/service/local/users/admin", users.get( 0 ).getResourceURI() );
        assertEquals( "admin", users.get( 0 ).getUserId() );
        assertEquals( "Administrator", users.get( 0 ).getName() );
        assertEquals( "active", users.get( 0 ).getStatus() );
        assertEquals( "changeme@yourcompany.com", users.get( 0 ).getEmail() );
        assertEquals( true, users.get( 0 ).isUserManaged() );
    }

    @Test
    public void getUser()
        throws Exception
    {
        final String userId = "deployment";

        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        GETFixture userListGetFixture = new GETFixture( getExpectedUser(), getExpectedPassword() );
        userListGetFixture.setExactURI( CoreClient.USER_PATH + "/" + userId );
        userListGetFixture.setResponseDocument( readTestDocumentResource( "user-get.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( userListGetFixture );
        fixture.setConversation( conversation );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        User user = client.getUser( userId );

        assertNotNull( user );

        assertEquals( "http://localhost:8081/nexus/service/local/users/deployment", user.getResourceURI() );
        assertEquals( "deployment", user.getUserId() );
        assertEquals( "Deployment User", user.getName() );
        assertEquals( "active", user.getStatus() );
        assertEquals( "changeme1@yourcompany.com", user.getEmail() );
        assertEquals( true, user.isUserManaged() );
        List<String> roles = new ArrayList<String>( 2 );
        roles.add( "deployment" );
        roles.add( "repo-all-full" );
        assertEquals( roles, user.getRoles() );
    }

    @Test
    public void postUser()
        throws Exception
    {
        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        POSTFixture userPostFixture = new POSTFixture( getExpectedUser(), getExpectedPassword() );
        userPostFixture.setExactURI( CoreClient.USER_PATH );
        userPostFixture.setRequestDocument( readTestDocumentResource( "user-post-req.xml" ) );
        userPostFixture.setResponseDocument( readTestDocumentResource( "user-post-resp.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( userPostFixture );
        fixture.setConversation( conversation );

        User user = new User();
        user.setUserId( "bbb" );
        user.setName( "bbb" );
        user.setStatus( "active" );
        user.setEmail( "b@b.b" );
        user.setUserManaged( true );
        user.getRoles().add( "admin" );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        User userResp = client.postUser( user );

        assertEquals( "http://localhost:8081/nexus/service/local/users/bbb", userResp.getResourceURI() );
        assertEquals( "bbb", userResp.getUserId() );
        assertEquals( "bbb", userResp.getName() );
        assertEquals( "active", userResp.getStatus() );
        assertEquals( "b@b.b", userResp.getEmail() );
        assertEquals( true, user.isUserManaged() );
        List<String> roles = new ArrayList<String>( 1 );
        roles.add( "admin" );
        assertEquals( roles, user.getRoles() );
    }

    @Test
    public void putUser()
        throws Exception
    {
        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        PUTFixture userPutFixture = new PUTFixture( getExpectedUser(), getExpectedPassword() );
        userPutFixture.setExactURI( CoreClient.USER_PATH + "/bbb" );
        userPutFixture.setResponseDocument( readTestDocumentResource( "user-put.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( userPutFixture );
        fixture.setConversation( conversation );

        User user = new User();
        user.setUserId( "bbb" );
        user.setName( "bbb" );
        user.setStatus( "active" );
        user.setEmail( "b@b.b" );
        user.setUserManaged( true );
        user.getRoles().add( "admin" );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        User userResp = client.putUser( user );

        assertEquals( "http://localhost:8081/nexus/service/local/users/bbb", userResp.getResourceURI() );
        assertEquals( "bbb", userResp.getUserId() );
        assertEquals( "bbb", userResp.getName() );
        assertEquals( "active", userResp.getStatus() );
        assertEquals( "b@b.b", userResp.getEmail() );
        assertEquals( true, user.isUserManaged() );
        List<String> roles = new ArrayList<String>( 1 );
        roles.add( "admin" );
        assertEquals( roles, user.getRoles() );
    }

    @Test
    public void deleteUser()
        throws Exception
    {
        String userId = "user-test";

        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        DELETEFixture userDeleteFixture = new DELETEFixture( getExpectedUser(), getExpectedPassword() );
        userDeleteFixture.setExactURI( CoreClient.USER_PATH + "/" + userId );
        conversation.add( getVersionCheckFixture() );
        conversation.add( userDeleteFixture );
        fixture.setConversation( conversation );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        client.deleteUser( userId );
    }

    @Test
    public void listRole()
        throws Exception
    {
        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        GETFixture userListGetFixture = new GETFixture( getExpectedUser(), getExpectedPassword() );
        userListGetFixture.setExactURI( CoreClient.ROLE_PATH );
        userListGetFixture.setResponseDocument( readTestDocumentResource( "role-list.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( userListGetFixture );
        fixture.setConversation( conversation );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        List<Role> roles = client.listRole();

        assertNotNull( roles );
        assertEquals( 4, roles.size() );
        assertEquals( "http://localhost:8080/nexus/service/local/roles/anonymous", roles.get( 3 ).getResourceURI() );
        assertEquals( "anonymous", roles.get( 3 ).getId() );
        assertEquals( "Nexus Anonymous Role", roles.get( 3 ).getName() );
        assertEquals( "Anonymous role for Nexus", roles.get( 3 ).getDescription() );
        assertEquals( 60, roles.get( 3 ).getSessionTimeout() );
        assertEquals( false, roles.get( 3 ).isUserManaged() );
        List<String> subRoles = new ArrayList<String>( 3 );
        subRoles.add( "ui-repo-browser" );
        subRoles.add( "ui-search" );
        subRoles.add( "ui-system-feeds" );
        assertEquals( subRoles, roles.get( 3 ).getRoles() );
        List<String> privileges = new ArrayList<String>( 6 );
        privileges.add( "1" );
        privileges.add( "54" );
        privileges.add( "57" );
        privileges.add( "58" );
        privileges.add( "70" );
        privileges.add( "74" );
        assertEquals( privileges, roles.get( 3 ).getPrivileges() );
    }

    @Test
    public void postRole()
        throws Exception
    {
        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        POSTFixture postFixture = new POSTFixture( getExpectedUser(), getExpectedPassword() );
        postFixture.setExactURI( CoreClient.ROLE_PATH );
        postFixture.setRequestDocument( readTestDocumentResource( "role-post-req.xml" ) );
        postFixture.setResponseDocument( readTestDocumentResource( "role-post-resp.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( postFixture );
        fixture.setConversation( conversation );

        Role role = new Role();
        role.setId( "a1" );
        role.setName( "a11" );
        role.setDescription( "a111" );
        role.setSessionTimeout( 100 );
        role.getRoles().add( "anonymous" );
        role.getPrivileges().add( "18" );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        Role roleResp = client.postRole( role );

        assertNotNull( roleResp );
        assertEquals( "http://localhost:8080/nexus/service/local/roles/a1", roleResp.getResourceURI() );
        assertEquals( "a1", roleResp.getId() );
        assertEquals( "a11", roleResp.getName() );
        assertEquals( "a111", roleResp.getDescription() );
        assertEquals( 100, roleResp.getSessionTimeout() );
        assertEquals( true, roleResp.isUserManaged() );
        assertEquals( 1, roleResp.getRoles().size() );
        assertEquals( "anonymous", roleResp.getRoles().get( 0 ) );
        assertEquals( 1, roleResp.getPrivileges().size() );
        assertEquals( "18", roleResp.getPrivileges().get( 0 ) );
    }

    @Test
    public void putUserToRole()
        throws Exception
    {
        String source = "url";
        String userId = "test";

        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        PUTFixture putFixture = new PUTFixture( getExpectedUser(), getExpectedPassword() );
        putFixture.setExactURI( CoreClient.USER_TO_ROLE_PATH + "/" + source + "/" + userId );
        putFixture.setRequestDocument( readTestDocumentResource( "user-to-role-put.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( putFixture );
        fixture.setConversation( conversation );

        UserToRole userToRole = new UserToRole();
        userToRole.setUserId( userId );
        userToRole.setSource( source );
        userToRole.getRoles().add( "anonymous" );
        userToRole.getRoles().add( "developer" );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );
        client.putUserToRole( userToRole );
    }
    
    @Test
    public void getUserToRole()
        throws Exception
    {
        String source = "url";
        String userId = "juven";

        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        GETFixture getFixture = new GETFixture( getExpectedUser(), getExpectedPassword() );
        getFixture.setExactURI( CoreClient.USER_TO_ROLE_PATH + "/" + source + "/" + userId );
        getFixture.setResponseDocument( readTestDocumentResource( "user-to-role-get.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( getFixture );
        fixture.setConversation( conversation );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );
        UserToRole result = client.getUserToRole( userId, source );

        assertNotNull( result );
        assertEquals( "url", result.getSource() );
        assertEquals( "juven", result.getUserId() );
        String[] roles = { "ui-basic", "ui-logs-config-files" };
        assertEquals( Arrays.asList( roles ), result.getRoles() );
    }

    @Test
    public void getPlexusUser()
        throws Exception
    {
        final String userId = "deployment";

        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();
        GETFixture getFixture = new GETFixture( getExpectedUser(), getExpectedPassword() );
        getFixture.setExactURI( CoreClient.PLEXUS_USER_PATH + "/" + userId );
        getFixture.setResponseDocument( readTestDocumentResource( "plexus-user-get.xml" ) );
        conversation.add( getVersionCheckFixture() );
        conversation.add( getFixture );
        fixture.setConversation( conversation );

        CoreClient client = new CoreClient( getBaseUrl(), getExpectedUser(), getExpectedPassword() );

        PlexusUser plexusUser = client.getPlexusUser( userId );

        assertNotNull( plexusUser );

        assertEquals( "deployment", plexusUser.getUserId() );
        assertEquals( "Deployment User", plexusUser.getName() );
        assertEquals( "changeme1@yourcompany.com", plexusUser.getEmail() );
        assertEquals( "default", plexusUser.getSource() );

        assertEquals( 2, plexusUser.getPlexusRoles().size() );
        assertEquals( "repo-all-full", plexusUser.getPlexusRoles().get( 0 ).getRoleId() );
        assertEquals( "Repo: All Repositories (Full Control)", plexusUser.getPlexusRoles().get( 0 ).getName() );
        assertEquals( "default", plexusUser.getPlexusRoles().get( 0 ).getSource() );
        assertEquals( "deployment", plexusUser.getPlexusRoles().get( 1 ).getRoleId() );
        assertEquals( "Nexus Deployment Role", plexusUser.getPlexusRoles().get( 1 ).getName() );
        assertEquals( "default", plexusUser.getPlexusRoles().get( 1 ).getSource() );
    }
}
