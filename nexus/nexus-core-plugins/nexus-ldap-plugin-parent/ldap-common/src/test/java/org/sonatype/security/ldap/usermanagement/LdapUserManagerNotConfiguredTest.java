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
package org.sonatype.security.ldap.usermanagement;

import java.io.FileOutputStream;

import junit.framework.Assert;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;
import org.sonatype.security.ldap.AbstractLdapTest;
import org.sonatype.security.usermanagement.UserManager;

public class LdapUserManagerNotConfiguredTest
    extends AbstractLdapTest
{
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        IOUtil.copy( getClass().getResourceAsStream( "/test-conf/conf/security-configuration-no-ldap.xml" ),
            new FileOutputStream( getNexusSecurityConfiguration() ) );

        IOUtil.copy( getClass().getResourceAsStream( "/test-conf/conf/security-configuration.xml" ),
            new FileOutputStream( getSecurityConfiguration() ) );
        
        getLdapRealmConfig().delete();

        // IOUtil.copy(
        // getClass().getResourceAsStream( "/test-conf/conf/ldap.xml" ),
        // new FileOutputStream( new File( CONF_HOME, "ldap.xml" ) ) );
    }

    @Test
    public void testNotConfigured()
        throws Exception
    {
        UserManager userManager = this.lookup( UserManager.class, "LDAP" );
        Assert.assertNull( userManager.getUser( "cstamas" ) );
    }
}
