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
package org.sonatype.nexus.security.ldap.realms.api;

import java.io.File;
import java.io.FileInputStream;

import org.codehaus.plexus.context.Context;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.AbstractNexusLdapTestCase;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.ldap.realms.persist.model.CUserAndGroupAuthConfiguration;
import org.sonatype.security.ldap.realms.persist.model.Configuration;
import org.sonatype.security.ldap.realms.persist.model.io.xpp3.LdapConfigurationXpp3Reader;

public class LdapUserGroupConfNotConfiguredTest
    extends AbstractNexusLdapTestCase
{

    private PlexusResource getResource()
        throws Exception
    {
        return this.lookup( PlexusResource.class, "LdapUserAndGroupsConfigurationPlexusResource" );
    }

    @Test
    public void testGetNotConfigured()
        throws Exception
    {
        PlexusResource resource = getResource();

        // none of these args are used, but if they start being used, we will need to change this.
        LdapUserAndGroupConfigurationResponse response =
            (LdapUserAndGroupConfigurationResponse) resource.get( null, null, null, null );

        // the default configuration is returned.
        LdapUserAndGroupConfigurationDTO dto = response.getData();
        Assert.assertNotNull( dto );

        Assert.assertEquals( "ou=groups", dto.getGroupBaseDn() );
        Assert.assertEquals( "cn", dto.getGroupIdAttribute() );
        Assert.assertEquals( "uniqueMember", dto.getGroupMemberAttribute() );
        Assert.assertEquals( "${username}", dto.getGroupMemberFormat() );
        Assert.assertEquals( "groupOfUniqueNames", dto.getGroupObjectClass() );
        Assert.assertEquals( "ou=people", dto.getUserBaseDn() );
        Assert.assertEquals( "uid", dto.getUserIdAttribute() );
        Assert.assertEquals( "inetOrgPerson", dto.getUserObjectClass() );
        Assert.assertNull( dto.getUserPasswordAttribute() );
        // Assert.assertEquals("userPassword", dto.getUserPasswordAttribute());
        Assert.assertEquals( "cn", dto.getUserRealNameAttribute() );
        Assert.assertEquals( "mail", dto.getEmailAddressAttribute() );
        Assert.assertNull( dto.getUserMemberOfAttribute() );
        Assert.assertTrue( dto.isLdapGroupsAsRoles() );
        Assert.assertFalse( dto.isGroupSubtree() );
        Assert.assertFalse( dto.isUserSubtree() );
    }

    private void validateConfigFile( LdapUserAndGroupConfigurationDTO dto )
        throws Exception
    {
        // this.getNexusLdapConfiguration();
        String configFileName = getLdapXml().getAbsolutePath();

        LdapConfigurationXpp3Reader reader = new LdapConfigurationXpp3Reader();
        FileInputStream fis = new FileInputStream( configFileName );
        Configuration config = reader.read( fis );

        CUserAndGroupAuthConfiguration userGroupConfig = config.getUserAndGroupConfig();

        Assert.assertEquals( dto.getGroupBaseDn(), userGroupConfig.getGroupBaseDn() );
        Assert.assertEquals( dto.getGroupIdAttribute(), userGroupConfig.getGroupIdAttribute() );
        Assert.assertEquals( dto.getGroupMemberAttribute(), userGroupConfig.getGroupMemberAttribute() );
        Assert.assertEquals( dto.getGroupMemberFormat(), userGroupConfig.getGroupMemberFormat() );
        Assert.assertEquals( dto.getGroupObjectClass(), userGroupConfig.getGroupObjectClass() );
        Assert.assertEquals( dto.getUserBaseDn(), userGroupConfig.getUserBaseDn() );
        Assert.assertEquals( dto.getUserIdAttribute(), userGroupConfig.getUserIdAttribute() );
        Assert.assertEquals( dto.getUserObjectClass(), userGroupConfig.getUserObjectClass() );
        Assert.assertEquals( dto.getUserPasswordAttribute(), userGroupConfig.getUserPasswordAttribute() );
        Assert.assertEquals( dto.getUserRealNameAttribute(), userGroupConfig.getUserRealNameAttribute() );
        Assert.assertEquals( dto.getEmailAddressAttribute(), userGroupConfig.getEmailAddressAttribute() );
        Assert.assertEquals( dto.getUserMemberOfAttribute(), userGroupConfig.getUserMemberOfAttribute() );
        Assert.assertEquals( dto.isLdapGroupsAsRoles(), userGroupConfig.isLdapGroupsAsRoles() );
        Assert.assertEquals( dto.isGroupSubtree(), userGroupConfig.isGroupSubtree() );
        Assert.assertEquals( dto.isUserSubtree(), userGroupConfig.isUserSubtree() );

    }

    @Test
    public void testPutNotConfigured()
        throws Exception
    {
        PlexusResource resource = getResource();

        LdapUserAndGroupConfigurationResponse response = new LdapUserAndGroupConfigurationResponse();
        LdapUserAndGroupConfigurationDTO userGroupConf = new LdapUserAndGroupConfigurationDTO();
        response.setData( userGroupConf );
        userGroupConf.setGroupMemberFormat( "uid=${username},ou=people,o=sonatype" );
        userGroupConf.setGroupObjectClass( "groupOfUniqueNames" );
        userGroupConf.setGroupBaseDn( "ou=groups" );
        userGroupConf.setGroupIdAttribute( "cn" );
        userGroupConf.setGroupMemberAttribute( "uniqueMember" );
        userGroupConf.setUserObjectClass( "inetOrgPerson" );
        userGroupConf.setUserBaseDn( "ou=people" );
        userGroupConf.setUserIdAttribute( "uid" );
        userGroupConf.setUserPasswordAttribute( "userPassword" );
        userGroupConf.setUserRealNameAttribute( "cn" );
        userGroupConf.setEmailAddressAttribute( "mail" );
        userGroupConf.setGroupSubtree( false );
        userGroupConf.setUserSubtree( true );

        LdapUserAndGroupConfigurationResponse result =
            (LdapUserAndGroupConfigurationResponse) resource.put( null, null, null, response );
        Assert.assertEquals( userGroupConf, result.getData() );

        // now how about that get
        result = (LdapUserAndGroupConfigurationResponse) resource.get( null, null, null, null );
        Assert.assertEquals( userGroupConf, result.getData() );

        this.validateConfigFile( userGroupConf );
    }

    @Override
    protected void customizeContext( Context ctx )
    {
        super.customizeContext( ctx );

        ctx.put( CONF_DIR_KEY, getLdapXml().getParentFile().getAbsolutePath() );
    }

    private File getLdapXml()
    {
        return new File( getConfHomeDir(), "no-conf/ldap.xml" );
    }

}
