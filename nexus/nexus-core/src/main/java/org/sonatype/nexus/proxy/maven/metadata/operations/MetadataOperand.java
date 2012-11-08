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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * @author Oleg Gusakov
 * @version $Id: MetadataOperand.java 726701 2008-12-15 14:31:34Z hboutemy $
 */
public class MetadataOperand
    extends AbstractOperand
{
    private final Metadata metadata;

    public MetadataOperand( final Metadata data )
    {
        super( data == null ? ModelVersionUtility.LATEST_MODEL_VERSION : ModelVersionUtility.getModelVersion( data ) );

        if ( data == null )
        {
            this.metadata = new Metadata();
        }
        else
        {
            this.metadata = data;
        }
    }

    public Metadata getOperand()
    {
        return metadata;
    }
}
