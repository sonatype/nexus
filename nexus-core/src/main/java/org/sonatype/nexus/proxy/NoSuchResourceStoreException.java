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
 * Thrown if the specifically requested ResourceStore does not exists.
 * 
 * @author cstamas
 */
public abstract class NoSuchResourceStoreException
    extends Exception
{
    private static final long serialVersionUID = 299346983704055394L;

    /**
     * Constructs a new exception with the specified detail message.
     * 
     * @param msg message
     */
    public NoSuchResourceStoreException( final String msg )
    {
        super( msg );
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param msg message
     * @param t the cause
     */
    public NoSuchResourceStoreException( final String msg, final Throwable t )
    {
        super( msg, t );
    }

    /**
     * Deprecated constructor that pre-assembles a message that does not make sense in some cases.
     * 
     * @param type
     * @param id
     * @deprecated Use any other "usual" exception constructor instead.
     */
    @Deprecated
    public NoSuchResourceStoreException( String type, String id )
    {
        super( "ResourceStore of type " + type + " with id='" + id + "' not found!" );
    }
}
