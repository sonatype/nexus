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
package org.sonatype.nexus.util;import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.codehaus.plexus.util.IOUtil;
/** * A util class to calculate various digests on Strings. Usaful for some simple password management. *  * @author cstamas */
public class DigesterUtils{    // Streams
    /**
     * Hex Encodes the digest value.
     * 
     * @param md
     * @return
     */
    public static String getDigestAsString( byte[] digest )
    {
        return new String( encodeHex( digest ) );
    }
    /**     * Calculates a digest for a String user the requested algorithm.     *      * @param alg     * @param content     * @return     * @throws NoSuchAlgorithmException     */    private static String getDigest( String alg, InputStream is )        throws NoSuchAlgorithmException    {        String result = null;        try        {            try            {                byte[] buffer = new byte[1024];                MessageDigest md = MessageDigest.getInstance( alg );                int numRead;                do                {                    numRead = is.read( buffer );                    if ( numRead > 0 )                    {                        md.update( buffer, 0, numRead );                    }                }                while ( numRead != -1 );                result = getDigestAsString( md.digest() );            }            finally            {                is.close();            }        }        catch ( IOException e )        {            // hrm            result = null;        }        return result;    }    // SHA1    /**     * Calculates a SHA1 digest for a string.     *      * @param content     * @return     */    public static String getSha1Digest( String content )    {        try        {            InputStream is = new ByteArrayInputStream( content.getBytes( "UTF-8" ) );            return getDigest( "SHA1", is );        }        catch ( NoSuchAlgorithmException e )        {            // will not happen            return null;        }        catch ( UnsupportedEncodingException e )        {            // will not happen            return null;        }    }    /**     * Calculates a SHA1 digest for a stream.     *      * @param content     * @return     */    public static String getSha1Digest( InputStream is )    {        try        {            return getDigest( "SHA1", is );        }        catch ( NoSuchAlgorithmException e )        {            // will not happen            return null;        }    }    /**     * Calculates a SHA1 digest for a file.     *      * @param content     * @return     */    public static String getSha1Digest( File file )    {        FileInputStream fis = null;        try        {            fis = new FileInputStream( file );            return getDigest( "SHA1", fis );        }        catch ( NoSuchAlgorithmException e )        {            // will not happen            return null;        }        catch ( FileNotFoundException e )        {            // will not happen            return null;        }        finally        {            IOUtil.close( fis );        }    }    // MD5    /**     * Calculates a SHA1 digest for a string.     *      * @param content     * @return     */    public static String getMd5Digest( String content )    {        try        {            InputStream fis = new ByteArrayInputStream( content.getBytes( "UTF-8" ) );            return getDigest( "MD5", fis );        }        catch ( NoSuchAlgorithmException e )        {            // will not happen            return null;        }        catch ( UnsupportedEncodingException e )        {            // will not happen            return null;        }    }    /**     * Calculates a SHA1 digest for a stream.     *      * @param content     * @return     */    public static String getMd5Digest( InputStream is )    {        try        {            return getDigest( "MD5", is );        }        catch ( NoSuchAlgorithmException e )        {            // will not happen            return null;        }    }    /**     * Calculates a SHA1 digest for a file.     *      * @param content     * @return     */    public static String getMd5Digest( File file )    {        FileInputStream fis = null;        try        {            fis = new FileInputStream( file );            return getDigest( "MD5", fis );        }        catch ( NoSuchAlgorithmException e )        {            // will not happen            return null;        }        catch ( FileNotFoundException e )        {            // will not happen            return null;        }        finally        {            IOUtil.close( fis );        }    }    // --    private static final char[] DIGITS =        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };    /**     * Blatantly copied from commons-codec version 1.3     *      * @param data     * @return     */    public static char[] encodeHex( byte[] data )    {        int l = data.length;        char[] out = new char[l << 1];        // two characters form the hex value.        for ( int i = 0, j = 0; i < l; i++ )        {            out[j++] = DIGITS[( 0xF0 & data[i] ) >>> 4];            out[j++] = DIGITS[0x0F & data[i]];        }        return out;    }

    public static String getMd5Digest( byte[] byteArray )
    {
        return getMd5Digest( new ByteArrayInputStream( byteArray ) );
    }

    public static String getSha1Digest( byte[] byteArray )
    {
        return getSha1Digest( new ByteArrayInputStream( byteArray ) );
    }

}