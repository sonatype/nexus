/**
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions/.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.configuration;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

@Component( role = PasswordHelper.class )
public class PasswordHelper
{

    private static final String ENC = "CMMDwoV";

    @Requirement
    private PlexusCipher plexusCipher;

    public String encrypt( String password )
        throws PlexusCipherException
    {
        if ( password != null )
        {
            return password;

            // TURNED OFF, on Linux causes trouble
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6521844
            // return plexusCipher.encrypt( password, ENC );
        }
        return null;
    }

    public String decrypt( String encodedPassword )
        throws PlexusCipherException
    {
        if ( encodedPassword != null )
        {
            return encodedPassword;
            
            // TURNED OFF, on Linux causes trouble
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6521844
            // return plexusCipher.decrypt( encodedPassword, ENC );
        }
        return null;
    }
}
