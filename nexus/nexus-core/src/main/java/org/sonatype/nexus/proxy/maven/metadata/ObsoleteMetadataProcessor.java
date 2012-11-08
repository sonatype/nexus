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
package org.sonatype.nexus.proxy.maven.metadata;

import java.io.IOException;

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * Used to remove metadata files
 * 
 * @author juven
 */
public class ObsoleteMetadataProcessor
    extends AbstractMetadataProcessor
{

    public ObsoleteMetadataProcessor( AbstractMetadataHelper metadataHelper )
    {
        super( metadataHelper );
    }

    /**
     * always return false, so the metadata will be removed
     */
    @Override
    protected boolean isMetadataCorrect( Metadata oldMd, String path )
        throws IOException
    {
        return false;
    }

    @Override
    public void postProcessMetadata( String path )
    {
        // do nothing
    }

    @Override
    protected void processMetadata( String path )
        throws IOException
    {
        // do nothing
    }

    @Override
    protected boolean shouldProcessMetadata( String path )
    {
        return true;
    }

}
