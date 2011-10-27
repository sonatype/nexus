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

import org.codehaus.plexus.context.Context;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.AbstractNexusLdapTestCase;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

public class LdapConnNotConfiguredTest
    extends AbstractNexusLdapTestCase
{

    private PlexusResource getResource()
        throws Exception
    {
        return this.lookup( PlexusResource.class, "LdapConnectionInfoPlexusResource" );
    }

    @Test
    public void testGetNotConfigured()
        throws Exception
    {
        PlexusResource resource = getResource();

        // none of these args are used, but if they start being used, we will need to change this.
        LdapConnectionInfoResponse response = (LdapConnectionInfoResponse) resource.get( null, null, null, null );

        // asssert an empty data is returned
        Assert.assertEquals( new LdapConnectionInfoDTO(), response.getData() );
    }

    @Test
    public void testPutNotConfigured()
        throws Exception
    {
        PlexusResource resource = getResource();

        LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
        LdapConnectionInfoDTO connectionInfo = new LdapConnectionInfoDTO();
        response.setData( connectionInfo );
        connectionInfo.setHost( "localhost" );
        connectionInfo.setPort( this.getLdapPort() );
        connectionInfo.setSearchBase( "o=sonatype" );
        connectionInfo.setSystemPassword( "secret" );
        connectionInfo.setSystemUsername( "uid=admin,ou=system" );
        connectionInfo.setProtocol( "ldap" );
        connectionInfo.setAuthScheme( "simple" );

        LdapConnectionInfoResponse result = (LdapConnectionInfoResponse) resource.put( null, null, null, response );
        this.validateConnectionDTO( connectionInfo, result.getData() );

        // now how about that get
        result = (LdapConnectionInfoResponse) resource.get( null, null, null, null );
        this.validateConnectionDTO( connectionInfo, result.getData() );
    }

    @Test
    public void testSetPasswordToFake()
        throws Exception
    {

        PlexusResource resource = getResource();

        LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
        LdapConnectionInfoDTO connectionInfo = new LdapConnectionInfoDTO();
        response.setData( connectionInfo );
        connectionInfo.setHost( "localhost" );
        connectionInfo.setPort( this.getLdapPort() );
        connectionInfo.setSearchBase( "o=sonatype" );
        connectionInfo.setSystemPassword( LdapRealmPlexusResourceConst.FAKE_PASSWORD );
        connectionInfo.setSystemUsername( "uid=admin,ou=system" );
        connectionInfo.setProtocol( "ldap" );
        connectionInfo.setAuthScheme( "simple" );

        //this is the same as not setting the password so it should throw an exception

        try
        {
            resource.put( null, null, null, response );
            Assert.fail( "Expected PlexusResourceException" );
        }
        catch ( PlexusResourceException e )
        {
            ErrorResponse errorResponse = (ErrorResponse) e.getResultObject();
            Assert.assertEquals( 1, errorResponse.getErrors().size() );

            Assert.assertTrue( this.getErrorString( errorResponse, 0 ).toLowerCase().contains( "password" ) );
        }
    }

    @Test
    public void testGetPasswordNullWhenNotSet()
        throws Exception
    {
        PlexusResource resource = getResource();

        LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
        LdapConnectionInfoDTO connectionInfo = new LdapConnectionInfoDTO();
        response.setData( connectionInfo );
        connectionInfo.setHost( "localhost" );
        connectionInfo.setPort( this.getLdapPort() );
        connectionInfo.setSearchBase( "o=sonatype" );
//        connectionInfo.setSystemPassword( "secret" );
//        connectionInfo.setSystemUsername( "uid=admin,ou=system" );
        connectionInfo.setProtocol( "ldap" );
        connectionInfo.setAuthScheme( "none" );

        LdapConnectionInfoResponse result = (LdapConnectionInfoResponse) resource.put( null, null, null, response );
        this.validateConnectionDTO( connectionInfo, result.getData() );

        // now how about that get
        result = (LdapConnectionInfoResponse) resource.get( null, null, null, null );
        this.validateConnectionDTO( connectionInfo, result.getData() );
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
