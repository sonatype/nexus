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
package org.sonatype.nexus.proxy.maven.gav;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.codehaus.plexus.component.annotations.Component;

/**
 * An M2 <code>GavCalculator</code>.
 * 
 * @author Jason van Zyl
 * @author Tamas Cservenak
 */
@Component( role = GavCalculator.class, hint = "maven2" )
public class M2GavCalculator
    implements GavCalculator
{
    public Gav pathToGav( String str )
    {
        try
        {
            String s = str.startsWith( "/" ) ? str.substring( 1 ) : str;

            int vEndPos = s.lastIndexOf( '/' );

            if ( vEndPos == -1 )
            {
                return null;
            }

            int aEndPos = s.lastIndexOf( '/', vEndPos - 1 );

            if ( aEndPos == -1 )
            {
                return null;
            }

            int gEndPos = s.lastIndexOf( '/', aEndPos - 1 );

            if ( gEndPos == -1 )
            {
                return null;
            }

            String groupId = s.substring( 0, gEndPos ).replace( '/', '.' );
            String artifactId = s.substring( gEndPos + 1, aEndPos );
            String version = s.substring( aEndPos + 1, vEndPos );
            String fileName = s.substring( vEndPos + 1 );

            boolean checksum = false;
            boolean signature = false;
            Gav.HashType checksumType = null;
            Gav.SignatureType signatureType = null;
            if ( s.endsWith( ".md5" ) )
            {
                checksum = true;
                checksumType = Gav.HashType.md5;
                s = s.substring( 0, s.length() - 4 );
            }
            else if ( s.endsWith( ".sha1" ) )
            {
                checksum = true;
                checksumType = Gav.HashType.sha1;
                s = s.substring( 0, s.length() - 5 );
            }

            if ( s.endsWith( ".asc" ) )
            {
                signature = true;
                signatureType = Gav.SignatureType.gpg;
                s = s.substring( 0, s.length() - 4 );
            }

            if ( s.endsWith( "maven-metadata.xml" ) )
            {
                return null;
            }

            boolean snapshot = version.endsWith( "SNAPSHOT" );

            if ( snapshot )
            {
                return getSnapshotGav( s, vEndPos, groupId, artifactId, version, fileName, checksum, signature,
                    checksumType, signatureType );
            }
            else
            {
                return getReleaseGav( s, vEndPos, groupId, artifactId, version, fileName, checksum, signature,
                    checksumType, signatureType );
            }
        }
        catch ( NumberFormatException e )
        {
            return null;
        }
        catch ( StringIndexOutOfBoundsException e )
        {
            return null;
        }
    }

    private Gav getReleaseGav( String s, int vEndPos, String groupId, String artifactId, String version,
                               String fileName, boolean checksum, boolean signature, Gav.HashType checksumType,
                               Gav.SignatureType signatureType )
    {
        if ( !fileName.startsWith( artifactId + "-" + version + "." )
            && !fileName.startsWith( artifactId + "-" + version + "-" ) )
        {
            // The path does not represents an artifact (filename does not match artifactId-version)!
            return null;
        }

        int nTailPos = vEndPos + artifactId.length() + version.length() + 2;

        String tail = s.substring( nTailPos );

        int nExtPos = tail.indexOf( '.' );

        if ( nExtPos == -1 )
        {
            // NX-563: not allowing extensionless paths to be interpreted as artifact
            return null;
        }

        String ext = tail.substring( nExtPos + 1 );

        String classifier = tail.charAt( 0 ) == '-' ? tail.substring( 1, nExtPos ) : null;

        return new Gav( groupId, artifactId, version, classifier, ext, null, null, fileName, checksum, checksumType,
            signature, signatureType );
    }

    private Gav getSnapshotGav( String s, int vEndPos, String groupId, String artifactId, String version,
                                String fileName, boolean checksum, boolean signature, Gav.HashType checksumType,
                                Gav.SignatureType signatureType )
    {

        Integer snapshotBuildNo = null;

        Long snapshotTimestamp = null;

        int vSnapshotStart = vEndPos + artifactId.length() + version.length() - 9 + 3;

        String vSnapshot = s.substring( vSnapshotStart, vSnapshotStart + 8 );

        String classifier = null;

        String ext = null;

        if ( "SNAPSHOT".equals( vSnapshot ) )
        {
            int nTailPos = vEndPos + artifactId.length() + version.length() + 2;

            String tail = s.substring( nTailPos );

            int nExtPos = tail.indexOf( '.' );

            if ( nExtPos == -1 )
            {
                // NX-563: not allowing extensionless paths to be interpreted as artifact
                return null;
            }

            ext = tail.substring( nExtPos + 1 );

            classifier = tail.charAt( 0 ) == '-' ? tail.substring( 1, nExtPos ) : null;
        }
        else
        {
            StringBuffer sb = new StringBuffer( vSnapshot );
            sb.append( s.substring( vSnapshotStart + sb.length(), vSnapshotStart + sb.length() + 8 ) );

            try
            {
                SimpleDateFormat df = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
                snapshotTimestamp = Long.valueOf( df.parse( sb.toString() ).getTime() );
            }
            catch ( ParseException e )
            {
            }

            int buildNumberPos = vSnapshotStart + sb.length();
            StringBuffer bnr = new StringBuffer();
            while ( s.charAt( buildNumberPos ) >= '0' && s.charAt( buildNumberPos ) <= '9' )
            {
                sb.append( s.charAt( buildNumberPos ) );
                bnr.append( s.charAt( buildNumberPos ) );
                buildNumberPos++;
            }
            String snapshotBuildNumber = sb.toString();
            snapshotBuildNo = Integer.parseInt( bnr.toString() );

            int n = version.length() > 9 ? version.length() - 9 + 1 : 0;

            String tail = s.substring( vEndPos + artifactId.length() + n + snapshotBuildNumber.length() + 2 );

            int nExtPos = tail.indexOf( '.' );

            if ( nExtPos == -1 )
            {
                // NX-563: not allowing extensionless paths to be interpreted as artifact
                return null;
            }

            ext = tail.substring( nExtPos + 1 );

            classifier = tail.charAt( 0 ) == '-' ? tail.substring( 1, nExtPos ) : null;

            version = version.substring( 0, version.length() - 8 ) + snapshotBuildNumber;
        }

        return new Gav( groupId, artifactId, version, classifier, ext, snapshotBuildNo, snapshotTimestamp, fileName,
            checksum, checksumType, signature, signatureType );
    }

    public String gavToPath( Gav gav )
    {
        StringBuffer path = new StringBuffer( "/" );

        path.append( gav.getGroupId().replaceAll( "(?m)(.)\\.", "$1/" ) ); // replace all '.' except the first char

        path.append( "/" );

        path.append( gav.getArtifactId() );

        path.append( "/" );

        path.append( gav.getBaseVersion() );

        path.append( "/" );

        path.append( calculateArtifactName( gav ) );

        return path.toString();
    }

    public String calculateArtifactName( Gav gav )
    {
        if ( gav.getName() != null && gav.getName().trim().length() > 0 )
        {
            return gav.getName();
        }
        else
        {
            StringBuffer path = new StringBuffer( gav.getArtifactId() );

            path.append( "-" );

            path.append( gav.getVersion() );

            if ( gav.getClassifier() != null && gav.getClassifier().trim().length() > 0 )
            {
                path.append( "-" );

                path.append( gav.getClassifier() );
            }

            if ( gav.getExtension() != null )
            {
                path.append( "." );

                path.append( gav.getExtension() );
            }

            if ( gav.isSignature() )
            {
                path.append( "." );

                path.append( gav.getSignatureType().toString() );
            }

            if ( gav.isHash() )
            {
                path.append( "." );

                path.append( gav.getHashType().toString() );
            }

            return path.toString();
        }
    }

}
