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
package org.sonatype.security.realms;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.junit.Test;

public class MemoryAuthenticationOnlyRealmTest
    extends AbstractRealmTest
{
    private MemoryAuthenticationOnlyRealm realm;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        realm = ( MemoryAuthenticationOnlyRealm ) lookup( Realm.class, "MemoryAuthenticationOnlyRealm" );
    }

    @Test
    public void testSuccessfulAuthentication()
        throws Exception
    {
        UsernamePasswordToken upToken = new UsernamePasswordToken( "admin", "admin321" );

        AuthenticationInfo ai = realm.getAuthenticationInfo( upToken );

        String password = ( String ) ai.getCredentials();

        assertEquals( "admin321", password );
    }

    @Test
    public void testFailedAuthentication()
        throws Exception
    {
        UsernamePasswordToken upToken = new UsernamePasswordToken( "admin", "admin123" );

        try
        {
            realm.getAuthenticationInfo( upToken );

            fail( "Authentication should have failed" );
        }
        catch( AuthenticationException e )
        {
            // good
        }
    }
}
