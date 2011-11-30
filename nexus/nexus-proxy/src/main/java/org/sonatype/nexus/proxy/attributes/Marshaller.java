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
package org.sonatype.nexus.proxy.attributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Component handling the marshalling and unmarshalling of attributes.
 * 
 * @author cstamas
 * @since 1.10.0
 */
public interface Marshaller
{
    /**
     * Marshals the attributes into bytes written into output stream.
     * 
     * @param attributes
     * @param outputStream
     * @throws IOException
     */
    void marshal( Attributes attributes, OutputStream outputStream )
        throws IOException;

    /**
     * Unmarshalls the attributes from bytes provided on inputStream.
     * 
     * @param inputStream
     * @return
     * @throws IOException
     * @throws InvalidInputException When the content is available, but it's corrupted, not expected, etc.
     */
    Attributes unmarshal( InputStream inputStream )
        throws IOException, InvalidInputException;
}
