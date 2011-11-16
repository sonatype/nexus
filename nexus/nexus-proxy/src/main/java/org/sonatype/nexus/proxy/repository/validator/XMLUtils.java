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
package org.sonatype.nexus.proxy.repository.validator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Scanner;

import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.validator.FileTypeValidator.FileTypeValidity;

/**
 * Some static helper classes to make "XML like" files probation for some patterns or expected content.
 * 
 * @author cstamas
 */
public class XMLUtils
{
    /**
     * Validate an "XML like file" using at most 200 lines. See
     * {@link #validateXmlLikeFile(StorageFileItem, String, int)} for details.
     * 
     * @param file
     * @param expectedPattern
     * @return
     * @throws IOException
     */
    public static FileTypeValidity validateXmlLikeFile( final StorageFileItem file, final String expectedPattern )
        throws IOException
    {
        return validateXmlLikeFile( file, expectedPattern, 200 );
    }

    /**
     * Validate an "XML like file" by searching for passed in patterns (using plain string matching), consuming at most
     * lines as passed in as parameter.
     * 
     * @param file
     * @param expectedPattern
     * @param linesToCheck
     * @return
     * @throws IOException
     */
    public static FileTypeValidity validateXmlLikeFile( final StorageFileItem file, final String expectedPattern,
                                                        final int linesToCheck )
        throws IOException
    {
        int lineCount = 0; // only process a few lines
        BufferedInputStream bis = null;
        try
        {
            bis = new BufferedInputStream( file.getInputStream() );
            Scanner scanner = new Scanner( bis );
            while ( scanner.hasNextLine() && lineCount < linesToCheck )
            {
                lineCount++;
                String line = scanner.nextLine();
                if ( line.contains( expectedPattern ) )
                {
                    return FileTypeValidity.VALID;
                }
            }
        }
        finally
        {
            IOUtil.close( bis );
        }

        return FileTypeValidity.INVALID;
    }

}
