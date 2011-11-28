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
package org.sonatype.security.ldap.dao.password;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.digest.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.Base64;

/**
 * @author cstamas
 */
@Component(role=PasswordEncoder.class, hint="sha")
public class SHA1PasswordEncoder
    implements PasswordEncoder
{

    final private static Logger log = LoggerFactory.getLogger( SHA1PasswordEncoder.class );

    public String getMethod()
    {
        return "SHA";
    }

    public String encodePassword( String password, Object salt )
    {
        return "{SHA}" + encodeString( password );
    }

    public boolean isPasswordValid( String encPassword, String inputPassword, Object salt )
    {
        String encryptedPassword = encPassword;
        if ( encryptedPassword.startsWith( "{SHA}" ) || encryptedPassword.startsWith( "{sha}" ) )
        {
            encryptedPassword = encryptedPassword.substring( "{sha}".length() );
        }

        String check = encodePassword( inputPassword, salt ).substring( "{sha}".length() );

        return check.equals( encryptedPassword );
    }

    protected String encodeString( String input )
    {
        InputStream is = new ByteArrayInputStream( input.getBytes() );
        String result = null;
        try
        {
            byte[] buffer = new byte[1024];
            MessageDigest md = MessageDigest.getInstance( "SHA1" );
            int numRead;
            do
            {
                numRead = is.read( buffer );
                if ( numRead > 0 )
                {
                    md.update( buffer, 0, numRead );
                }
            }
            while ( numRead != -1 );
            result = new String( Base64.encodeBase64( md.digest() ) );
        }
        catch ( Exception e )
        {
            // Exception is NOT logged because it may contain users password.
            log.warn( "Exception thrown while encoding password." );

        }
        return result;
    }

}
