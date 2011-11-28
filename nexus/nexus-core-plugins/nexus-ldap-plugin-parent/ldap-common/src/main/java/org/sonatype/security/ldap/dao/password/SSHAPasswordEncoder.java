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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.plexus.components.cipher.Base64;

/**
 * @author bdemers
 */
@Component( role = PasswordEncoder.class, hint = "ssha" )
public class SSHAPasswordEncoder
    implements PasswordEncoder
{
    private static final String SSHA_PREFIX = "{SSHA}";

    private Random random = new Random();

    public String getMethod()
    {
        return "SSHA";
    }

    public String encodePassword( String password, Object salt )
    {
        try
        {
            byte[] saltBytes;

            // needs to be null, byteArray, or a String
            if ( salt == null )
            {
                // 4 bytes
                saltBytes = new BigInteger( ( 4 * 8 ), random ).toString( 32 ).getBytes( "UTF-8" );
            }
            else if ( byte[].class.isInstance( salt ) )
            {
                saltBytes = (byte[]) salt;
            }
            else
            {
                throw new IllegalArgumentException( "salt must be of type: byte[]." );
            }

            // check to make sure we can get the algorithm
            MessageDigest md;
            try
            {
                md = MessageDigest.getInstance( "SHA1" );
            }
            catch ( NoSuchAlgorithmException e )
            {
                throw new RuntimeException( "Digest SHA not supported on this JVM." );
            }
            
            // digest
            md.update( password.getBytes( "UTF-8" ) );

            md.update( saltBytes );
            byte[] digested = md.digest();

            // toBeEncoded = digest + salt
            byte[] toBeEncoded = new byte[digested.length + saltBytes.length];
            System.arraycopy( digested, 0, toBeEncoded, 0, digested.length );
            System.arraycopy( saltBytes, 0, toBeEncoded, digested.length, saltBytes.length );

            return "{SSHA}" + new String( Base64.encodeBase64( toBeEncoded ) );

        }
        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException( "This JVM failed to get bytes in UTF-8 from String: " + salt, e );
        }
    }

    public boolean isPasswordValid( String encPassword, String inputPassword, Object salt )
    {
        // check for null
        if ( inputPassword == null )
        {
            return false;
        }

        String encryptedPassword = encPassword;
        // strip off the prefix
        if ( encryptedPassword.startsWith( SSHA_PREFIX ) || encryptedPassword.startsWith( SSHA_PREFIX.toLowerCase() ) )
        {
            encryptedPassword = encryptedPassword.substring( SSHA_PREFIX.length() );
        }

        try
        {
            byte[] decodedBytes = Base64.decodeBase64( encryptedPassword.getBytes( "UTF-8" ) );

            // strip the first 20 char, but make sure it is valie
            if ( decodedBytes.length - 20 <= 0 )
            {
                return false;
            }

            byte[] decryptSalt = new byte[decodedBytes.length - 20];
            System.arraycopy( decodedBytes, 20, decryptSalt, 0, decryptSalt.length );

            String check = this.encodePassword( inputPassword, decryptSalt );
            return check.substring( SSHA_PREFIX.length() ).equals( encryptedPassword );

        }
        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException( "This JVM failed to get bytes in UTF-8 from String: " + salt, e );
        }
    }
}
