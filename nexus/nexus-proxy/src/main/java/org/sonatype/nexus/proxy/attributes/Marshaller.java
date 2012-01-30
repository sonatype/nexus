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
package org.sonatype.nexus.proxy.attributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Component handling the marshalling and unmarshalling of attributes.
 * 
 * @author cstamas
 * @since 2.0
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
