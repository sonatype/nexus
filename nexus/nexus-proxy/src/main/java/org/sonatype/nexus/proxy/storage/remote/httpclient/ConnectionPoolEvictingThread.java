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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;

import com.google.common.base.Preconditions;

/**
 * Low priority daemon thread responsible to evic connection manager pools of HttpClient instances that were created
 * with {@link HttpClientUtil} class.
 * 
 * @author cstamas
 * @since 2.2
 */
class ConnectionPoolEvictingThread
    extends Thread
{
    private final ClientConnectionManager clientConnectionManager;

    private final long idleTimeMillis;

    ConnectionPoolEvictingThread( final ClientConnectionManager clientConnectionManager, final long idleTimeMillis )
    {
        super( "HC4x-ConnectionPoolEvictingThread" );
        Preconditions.checkArgument( idleTimeMillis > -1, "Keep alive period in milliseconds cannot be negative." );
        this.clientConnectionManager = Preconditions.checkNotNull( clientConnectionManager );
        this.idleTimeMillis = idleTimeMillis;
        setDaemon( true );
        setPriority( MIN_PRIORITY );
    }

    @Override
    public void run()
    {
        try
        {
            while ( true )
            {
                Thread.sleep( 5000 );
                synchronized ( this )
                {
                    clientConnectionManager.closeExpiredConnections();
                    clientConnectionManager.closeIdleConnections( idleTimeMillis, TimeUnit.MILLISECONDS );
                }
            }
        }
        catch ( InterruptedException e )
        {
            // bye bye
        }
    }
}
