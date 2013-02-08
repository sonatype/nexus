/*
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
package org.sonatype.nexus.apachehttpclient;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Low priority daemon thread responsible to evict connection manager pooled connections/
 *
 * @author cstamas
 * @since 2.2
 */
class EvictingThread
    extends Thread
{

    private static final Logger LOGGER = LoggerFactory.getLogger( EvictingThread.class );

    private final Collection<ClientConnectionManager> clientConnectionManagers;

    private final long idleTimeMillis;

    private final long delay;

    EvictingThread( final long idleTimeMillis, final long delay )
    {
        super( "HC4x-EvictingThread" );
        Preconditions.checkArgument( idleTimeMillis > -1, "Keep alive period in milliseconds cannot be negative." );
        clientConnectionManagers = Lists.newCopyOnWriteArrayList();
        this.idleTimeMillis = idleTimeMillis;
        this.delay = delay;
        setDaemon( true );
        setPriority( MIN_PRIORITY );
    }

    EvictingThread( final long idleTimeMillis )
    {
        this( idleTimeMillis, 5000 );
    }

    /**
     * Registers a connection manager.
     *
     * @param clientConnectionManager to be registered
     */
    public void register( final ClientConnectionManager clientConnectionManager )
    {
        clientConnectionManagers.add( checkNotNull( clientConnectionManager ) );
    }

    /**
     * Unregisters a connection manager.
     *
     * @param clientConnectionManager to be unregistered
     */
    public void unregister( final ClientConnectionManager clientConnectionManager )
    {
        clientConnectionManagers.remove( checkNotNull( clientConnectionManager ) );
    }

    @Override
    public void run()
    {
        LOGGER.debug( "Starting '{}' (delay {} millis)", getName(), delay );
        try
        {
            while ( true )
            {
                synchronized ( this )
                {
                    wait( delay );
                    for ( final ClientConnectionManager clientConnectionManager : clientConnectionManagers )
                    {
                        try
                        {
                            clientConnectionManager.closeExpiredConnections();
                        }
                        catch ( final Exception e )
                        {
                            LOGGER.warn( "Failed to close expired connections", e );
                        }
                        try
                        {
                            clientConnectionManager.closeIdleConnections( idleTimeMillis, TimeUnit.MILLISECONDS );
                        }
                        catch ( final Exception e )
                        {
                            LOGGER.warn( "Failed to close expired connections", e );
                        }
                    }
                }
            }
        }
        catch ( InterruptedException e )
        {
            // bye bye
        }
        LOGGER.debug( "Stopped '{}'", getName(), delay );
    }

}
