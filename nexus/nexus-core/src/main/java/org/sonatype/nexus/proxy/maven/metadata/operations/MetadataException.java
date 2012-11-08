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

/**
 * Thrown when some logical error applies to operation (like bad params, etc). TODO: this should be runtime exception.
 * 
 * @author cstamas
 */
public class MetadataException
    extends Exception
{

    private static final long serialVersionUID = -5336177865089762129L;

    public MetadataException( String msg )
    {
        super( msg );
    }

    public MetadataException( String msg, Exception e )
    {
        super( msg, e );
    }

}
