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
package org.sonatype.nexus.security.ldap.realms.testharness;

import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationResponse;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequestDTO;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.sonatype.security.ldap.realms.persist.model.CUserAndGroupAuthConfiguration;
import org.testng.Assert;

import com.thoughtworks.xstream.XStream;

public class LdapUserGroupMessageUtil
{

    private static final String SERVICE_PART = RequestFacade.SERVICE_LOCAL + "ldap/user_group_conf";

    private XStream xstream;

    private MediaType mediaType;

    private static final Logger LOG = LoggerFactory.getLogger( GroupMessageUtil.class );

    public LdapUserGroupMessageUtil( XStream xstream, MediaType mediaType )
    {
        super();
        this.xstream = xstream;
        this.mediaType = mediaType;
    }

    public LdapUserAndGroupConfigurationDTO getUserGroupConfig()
        throws IOException
    {
        Response response = this.sendMessage( Method.GET, null );
        return this.getResourceFromResponse( response );
    }

    public LdapUserAndGroupConfigurationDTO updateUserGroupConfig( LdapUserAndGroupConfigurationDTO userGroupConfig )
        throws Exception
    {
        Response response = this.sendMessage( Method.PUT, userGroupConfig );

        if ( !response.getStatus().isSuccess() )
        {
            String responseText = response.getEntity().getText();
            Assert.fail( "Could not create Repository: " + response.getStatus() + ":\n" + responseText );
        }
        LdapUserAndGroupConfigurationDTO responseResource = this.getResourceFromResponse( response );

        this.validateResourceResponse( userGroupConfig, responseResource );

        return responseResource;
    }

    public Response sendMessage( Method method, LdapUserAndGroupConfigurationDTO resource )
        throws IOException
    {

        XStreamRepresentation representation = new XStreamRepresentation( xstream, "", mediaType );

        String serviceURI = SERVICE_PART;

        LdapUserAndGroupConfigurationResponse repoResponseRequest = new LdapUserAndGroupConfigurationResponse();
        repoResponseRequest.setData( resource );

        // now set the payload
        representation.setPayload( repoResponseRequest );

        LOG.debug( "sendMessage: " + representation.getText() );

        return RequestFacade.sendMessage( serviceURI, method, representation );
    }

    public Response sendTestMessage( LdapUserAndGroupConfigTestRequestDTO resource )
        throws IOException
    {

        XStreamRepresentation representation = new XStreamRepresentation( xstream, "", mediaType );

        String serviceURI = RequestFacade.SERVICE_LOCAL + "ldap/test_user_conf";

        LdapUserAndGroupConfigTestRequest repoResponseRequest = new LdapUserAndGroupConfigTestRequest();
        repoResponseRequest.setData( resource );

        // now set the payload
        representation.setPayload( repoResponseRequest );

        LOG.debug( "sendMessage: " + representation.getText() );

        return RequestFacade.sendMessage( serviceURI, Method.PUT, representation );
    }

    public LdapUserAndGroupConfigurationDTO getResourceFromResponse( Response response )
        throws IOException
    {
        String responseString = response.getEntity().getText();
        LOG.debug( " getResourceFromResponse: " + responseString );

        XStreamRepresentation representation = new XStreamRepresentation( xstream, responseString, mediaType );
        LdapUserAndGroupConfigurationResponse resourceResponse = (LdapUserAndGroupConfigurationResponse) representation
            .getPayload( new LdapUserAndGroupConfigurationResponse() );

        return resourceResponse.getData();
    }

    @SuppressWarnings( "unchecked" )
    public void validateLdapConfig( LdapUserAndGroupConfigurationDTO connInfo )
        throws Exception
    {
        CUserAndGroupAuthConfiguration fileConfig = LdapConfigurationUtil.getConfiguration().getUserAndGroupConfig();
        Assert.assertEquals(connInfo.getGroupBaseDn(), fileConfig.getGroupBaseDn());
        Assert.assertEquals(connInfo.getGroupIdAttribute(), fileConfig.getGroupIdAttribute());
        Assert.assertEquals(connInfo.getGroupMemberAttribute(), fileConfig.getGroupMemberAttribute());
        Assert.assertEquals(connInfo.getGroupMemberFormat(), fileConfig.getGroupMemberFormat());
        Assert.assertEquals(connInfo.getGroupObjectClass(), fileConfig.getGroupObjectClass());
        Assert.assertEquals(connInfo.getUserBaseDn(), fileConfig.getUserBaseDn());
        Assert.assertEquals(connInfo.getUserIdAttribute(), fileConfig.getUserIdAttribute());
        Assert.assertEquals(connInfo.getUserObjectClass(), fileConfig.getUserObjectClass());
        Assert.assertEquals(connInfo.getUserPasswordAttribute(), fileConfig.getUserPasswordAttribute());
        Assert.assertEquals(connInfo.getUserRealNameAttribute(), fileConfig.getUserRealNameAttribute());
        Assert.assertEquals(connInfo.getEmailAddressAttribute(), fileConfig.getEmailAddressAttribute());
        Assert.assertEquals(connInfo.isLdapGroupsAsRoles(), fileConfig.isLdapGroupsAsRoles() );
        Assert.assertEquals(connInfo.getUserMemberOfAttribute(), fileConfig.getUserMemberOfAttribute() );
        Assert.assertEquals(connInfo.isGroupSubtree(), fileConfig.isGroupSubtree() );
        Assert.assertEquals(connInfo.isUserSubtree(), fileConfig.isUserSubtree() );
    }

    public void validateResourceResponse( LdapUserAndGroupConfigurationDTO expected, LdapUserAndGroupConfigurationDTO actual )
        throws Exception
    {
        // this object has an equals method, but it makes for not so easy debuging, so call it after each field compare to make
        // sure we didn't forget anything
        Assert.assertEquals(expected.getGroupBaseDn(), StringEscapeUtils.unescapeHtml( actual.getGroupBaseDn() ) );
        Assert.assertEquals(expected.getGroupIdAttribute(), StringEscapeUtils.unescapeHtml( actual.getGroupIdAttribute() ) );
        Assert.assertEquals(expected.getGroupMemberAttribute(), StringEscapeUtils.unescapeHtml( actual.getGroupMemberAttribute() ) );
        Assert.assertEquals(expected.getGroupMemberFormat(), StringEscapeUtils.unescapeHtml( actual.getGroupMemberFormat() ) );
        Assert.assertEquals(expected.getGroupObjectClass(), StringEscapeUtils.unescapeHtml( actual.getGroupObjectClass() ) );
        Assert.assertEquals(expected.getUserBaseDn(), StringEscapeUtils.unescapeHtml( actual.getUserBaseDn() ) );
        Assert.assertEquals(expected.getUserIdAttribute(), StringEscapeUtils.unescapeHtml( actual.getUserIdAttribute() ) );
        Assert.assertEquals(expected.getUserObjectClass(), StringEscapeUtils.unescapeHtml( actual.getUserObjectClass() ) );
        Assert.assertEquals(expected.getUserPasswordAttribute(), StringEscapeUtils.unescapeHtml( actual.getUserPasswordAttribute() ) );
        Assert.assertEquals(expected.getUserRealNameAttribute(), StringEscapeUtils.unescapeHtml( actual.getUserRealNameAttribute() ) );
        Assert.assertEquals(expected.getEmailAddressAttribute(), StringEscapeUtils.unescapeHtml( actual.getEmailAddressAttribute() ) );
        Assert.assertEquals(expected.getUserMemberOfAttribute(), StringEscapeUtils.unescapeHtml( actual.getUserMemberOfAttribute() ) );

        Assert.assertEquals(expected.isLdapGroupsAsRoles(), actual.isLdapGroupsAsRoles() );
        Assert.assertEquals(expected.isGroupSubtree(), actual.isGroupSubtree() );
        Assert.assertEquals(expected.isUserSubtree(), actual.isUserSubtree() );

        // also validate the file config
        this.validateLdapConfig( expected );
    }

}
