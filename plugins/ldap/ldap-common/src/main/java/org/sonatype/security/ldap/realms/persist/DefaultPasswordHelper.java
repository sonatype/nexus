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
package org.sonatype.security.ldap.realms.persist;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.security.ldap.upgrade.cipher.PlexusCipher;
import org.sonatype.security.ldap.upgrade.cipher.PlexusCipherException;


@Component( role = PasswordHelper.class )
public class DefaultPasswordHelper
    implements PasswordHelper
{

    private static final String ENC = "CMMDwoV";

    @Requirement
    private PlexusCipher plexusCipher;

    public String encrypt( String password )
        throws PlexusCipherException
    {
        if ( password != null )
        {

            return plexusCipher.encrypt( password, ENC );
        }
        return null;
    }

    public String decrypt( String encodedPassword )
        throws PlexusCipherException
    {
        if ( encodedPassword != null )
        {
            return plexusCipher.decrypt( encodedPassword, ENC );
        }
        return null;
    }
}
