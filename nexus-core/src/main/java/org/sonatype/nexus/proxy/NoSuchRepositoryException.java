/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy;

/**
 * Thrown if the specifically requested Repository does not exists.
 * 
 * @author cstamas
 */
public class NoSuchRepositoryException
    extends NoSuchResourceStoreException
{
    private static final long serialVersionUID = 299346983704055394L;

    /**
     * Constructs a new exception with message based on passed in Repository ID.
     * 
     * @param repoId
     */
    public NoSuchRepositoryException( final String repoId )
    {
        super( "Repository with ID=\"" + repoId + "\" not found" );
    }

    /**
     * Constructs a new exception with the specified detail message and cause. Usable in cases where repository ID is
     * unknown from the current context, only the fact is known it is not (yet) present.
     * 
     * @param msg message
     * @param t the cause
     * @since 2.1
     */
    public NoSuchRepositoryException( final String msg, final Throwable t )
    {
        super( msg, t );
    }
}
