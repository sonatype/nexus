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
package org.sonatype.nexus.proxy.storage;

import java.util.HashMap;

import org.sonatype.nexus.logging.AbstractLoggingComponent;

/**
 * The abstract storage context.
 * 
 * @author cstamas
 */
public abstract class AbstractStorageContext
    extends AbstractLoggingComponent
    implements StorageContext
{
    private final HashMap<String, Object> context = new HashMap<String, Object>();

    private long lastChanged = System.currentTimeMillis();

    private StorageContext parent;

    public AbstractStorageContext( StorageContext parent )
    {
        setParentStorageContext( parent );
    }

    public long getLastChanged()
    {
        if ( parent != null )
        {
            return parent.getLastChanged() > lastChanged ? parent.getLastChanged() : lastChanged;
        }
        else
        {
            return lastChanged;
        }
    }

    protected void setLastChanged( long ts )
    {
        lastChanged = ts;
    }

    public StorageContext getParentStorageContext()
    {
        return parent;
    }

    public void setParentStorageContext( StorageContext parent )
    {
        this.parent = parent;
    }

    public Object getContextObject( String key )
    {
        return getContextObject( key, true );
    }

    public Object getContextObject( final String key, final boolean fallbackToParent )
    {
        if ( context.containsKey( key ) )
        {
            return context.get( key );
        }
        else if ( fallbackToParent && parent != null )
        {
            return parent.getContextObject( key );
        }
        else
        {
            return null;
        }
    }

    public void putContextObject( String key, Object value )
    {
        final Object previous = context.put( key, value );

        setLastChanged( System.currentTimeMillis() );
        getLogger().debug( "Context entry '{}' updated: {} -> {}", new Object[]{ key, previous, value } );
    }

    public void removeContextObject( String key )
    {
        final Object removed = context.remove( key );

        setLastChanged( System.currentTimeMillis() );
        getLogger().debug( "Context entry '{}' removed. Existent value: ", key, removed );
    }

    public boolean hasContextObject( String key )
    {
        return context.containsKey( key );
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "{lastChanged=" + lastChanged + ", parent=" + parent + "}";
    }

}
