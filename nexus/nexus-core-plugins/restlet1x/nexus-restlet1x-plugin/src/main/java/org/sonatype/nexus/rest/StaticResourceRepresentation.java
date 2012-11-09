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
package org.sonatype.nexus.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.codehaus.plexus.util.IOUtil;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;
import org.sonatype.nexus.plugins.rest.StaticResource;

public class StaticResourceRepresentation
    extends OutputRepresentation
{
    private final StaticResource resource;

    public StaticResourceRepresentation( StaticResource resource )
    {
        super( MediaType.valueOf( resource.getContentType() ) );

        setSize( resource.getSize() );
        setModificationDate( new Date( resource.getLastModified() ) );

        setAvailable( true );

        this.resource = resource;
    }

    @Override
    public void write( OutputStream outputStream )
        throws IOException
    {
        InputStream is = null;

        try
        {
            is = resource.getInputStream();

            IOUtil.copy( is, outputStream );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

}
