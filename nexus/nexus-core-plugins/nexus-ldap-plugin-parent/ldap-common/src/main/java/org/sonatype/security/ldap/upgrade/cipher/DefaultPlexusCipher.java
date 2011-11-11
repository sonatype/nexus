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
package org.sonatype.security.ldap.upgrade.cipher;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 */
@Component( role = PlexusCipher.class )
public class DefaultPlexusCipher
    implements PlexusCipher
{
    private static final int SALT_SIZE = 8;

    private static final String STRING_ENCODING = "UTF8";

    private Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Encryption algorithm to use by this instance. Needs protected scope for tests
     */
    @Configuration( value = "PBEWithSHAAnd128BitRC4" )
    protected String algorithm = "PBEWithSHAAnd128BitRC4";

    /**
     * Number of iterations when generationg the key
     * 
     * @plexus.configuration default-value="23"
     */
    @Configuration( value = "23" )
    protected int iterationCount = 23;

    private final BouncyCastleProvider bouncyCastleProvider;

    protected Logger getLogger()
    {
        return logger;
    }

    public DefaultPlexusCipher()
    {
        this.bouncyCastleProvider = new BouncyCastleProvider();
    }

    // /**
    // * Salt to init this cypher
    // *
    // * @plexus.configuration default-value="maven.rules.in.this"
    // */
    // protected String salt = "maven.rules.in.this";
    // protected byte [] saltData = new byte[8];
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    private Cipher init( String passPhrase, byte[] salt, boolean encrypt )
        throws PlexusCipherException
    {
        int mode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        try
        {
            KeySpec keySpec = new PBEKeySpec( passPhrase.toCharArray() );
            SecretKey key = SecretKeyFactory.getInstance( algorithm, bouncyCastleProvider ).generateSecret( keySpec );
            Cipher cipher = Cipher.getInstance( algorithm, bouncyCastleProvider );

            PBEParameterSpec paramSpec = new PBEParameterSpec( salt, iterationCount );

            cipher.init( mode, key, paramSpec );
            return cipher;

        }
        catch ( Exception e )
        {
            throw new PlexusCipherException( e );
        }
    }

    // ---------------------------------------------------------------
    private byte[] getSalt( int saltSize )
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        // SecureRandom sr = SecureRandom.getInstance( SECURE_RANDOM_ALGORITHM,
        // SECURITY_PROVIDER );
        SecureRandom sr = new SecureRandom();
        sr.setSeed( System.currentTimeMillis() );
        return sr.generateSeed( saltSize );
    }

    // ---------------------------------------------------------------
    public String encrypt( String str, String passPhrase )
        throws PlexusCipherException
    {
        try
        {
            byte[] salt = getSalt( SALT_SIZE );
            Cipher cipher = init( passPhrase, salt, true );

            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes( STRING_ENCODING );

            // Encrypt it
            byte[] enc = cipher.doFinal( utf8 );

            // Encode bytes to base64 to get a string
            Base64Encoder b64 = new Base64Encoder();
            byte saltLen = (byte) ( salt.length & 0x00ff );
            int encLen = enc.length;
            byte[] res = new byte[salt.length + encLen + 1];
            res[0] = saltLen;
            System.arraycopy( salt, 0, res, 1, saltLen );
            System.arraycopy( enc, 0, res, saltLen + 1, encLen );

            ByteArrayOutputStream bout = new ByteArrayOutputStream( res.length * 2 );
            b64.encode( res, 0, res.length, bout );

            return bout.toString( STRING_ENCODING );

        }
        catch ( Exception e )
        {
            throw new PlexusCipherException( e );
        }
    }

    // ---------------------------------------------------------------
    public String encryptAndDecorate( String str, String passPhrase )
        throws PlexusCipherException
    {
        return decorate( encrypt( str, passPhrase ) );
    }

    // ---------------------------------------------------------------
    public String decrypt( String str, String passPhrase )
        throws PlexusCipherException
    {
        if ( StringUtils.isEmpty( str ) )
            return str;

        try
        {
            // Decode base64 to get bytes
            Base64Encoder decoder = new Base64Encoder();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            decoder.decode( str, baos );
            byte[] res = baos.toByteArray();

            int saltLen = res[0] & 0x00ff;
            if ( saltLen != SALT_SIZE )
                throw new Exception( "default.plexus.cipher.encryptedStringCorruptedStructure" );

            if ( res.length < ( saltLen + 2 ) )
                throw new Exception( "default.plexus.cipher.encryptedStringCorruptedLength" );

            byte[] salt = new byte[saltLen];
            System.arraycopy( res, 1, salt, 0, saltLen );

            int decLen = res.length - saltLen - 1;
            if ( decLen < 1 )
                throw new Exception( "default.plexus.cipher.encryptedStringCorruptedSize" );

            byte[] dec = new byte[decLen];
            System.arraycopy( res, saltLen + 1, dec, 0, decLen );

            // Decrypt
            Cipher cipher = init( passPhrase, salt, false );
            byte[] utf8 = cipher.doFinal( dec );

            // Decode using utf-8
            return new String( utf8, "UTF8" );

        }
        catch ( Exception e )
        {
            throw new PlexusCipherException( e );
        }
    }

    // ---------------------------------------------------------------
    public String decryptDecorated( String str, String passPhrase )
        throws PlexusCipherException
    {
        if ( StringUtils.isEmpty( str ) )
            return str;

        if ( isEncryptedString( str ) )
            return decrypt( unDecorate( str ), passPhrase );

        return decrypt( str, passPhrase );
    }

    // ----------------------------------------------------------------------------
    // -------------------
    public boolean isEncryptedString( String str )
    {
        if ( StringUtils.isEmpty( str ) )
            return false;

        int start = str.indexOf( ENCRYPTED_STRING_DECORATION_START );
        int stop = str.indexOf( ENCRYPTED_STRING_DECORATION_STOP );
        if ( start != -1 && stop != -1 && stop > start + 1 )
            return true;
        return false;
    }

    // ----------------------------------------------------------------------------
    // -------------------
    public String unDecorate( String str )
        throws PlexusCipherException
    {
        if ( !isEncryptedString( str ) )
            throw new PlexusCipherException( "default.plexus.cipher.badEncryptedPassword" );

        int start = str.indexOf( ENCRYPTED_STRING_DECORATION_START );
        int stop = str.indexOf( ENCRYPTED_STRING_DECORATION_STOP );
        return str.substring( start + 1, stop );
    }

    // ----------------------------------------------------------------------------
    // -------------------
    public String decorate( String str )
    {
        return ENCRYPTED_STRING_DECORATION_START + ( str == null ? "" : str ) + ENCRYPTED_STRING_DECORATION_STOP;
    }

    // ---------------------------------------------------------------
    // ---------------------------------------------------------------
    // ***************************************************************

}
