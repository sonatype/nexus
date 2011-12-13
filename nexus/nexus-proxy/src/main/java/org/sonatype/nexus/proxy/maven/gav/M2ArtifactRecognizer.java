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

/**
 * Utility methods for basic "detection" of artifact kind in M2 repository.
 */
public class M2ArtifactRecognizer
{
    /**
     * Is this item M2 Checksum?
     */
    public static boolean isChecksum( String path )
    {
        return path.endsWith( ".sha1" ) || path.endsWith( ".md5" );
    }

    /**
     * Is this item M2 POM?
     */
    public static boolean isPom( String path )
    {
        return path.endsWith( ".pom" ) || path.endsWith( ".pom.sha1" ) || path.endsWith( ".pom.md5" );
    }

    /**
     * Is this item M2 Snapshot?
     * 
     * @param path
     * @return
     */
    public static boolean isSnapshot( String path )
    {
        return path.indexOf( "SNAPSHOT" ) != -1;
    }

    /**
     * Is this item M2 metadata?
     */
    public static boolean isMetadata( String path )
    {
        return path.endsWith( "maven-metadata.xml" ) || path.endsWith( "maven-metadata.xml.sha1" )
            || path.endsWith( "maven-metadata.xml.md5" );
    }

    public static boolean isSignature( String path )
    {
        return path.endsWith( ".asc" );
    }
}
