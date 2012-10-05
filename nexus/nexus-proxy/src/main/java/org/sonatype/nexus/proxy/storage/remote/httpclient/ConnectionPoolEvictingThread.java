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

import java.util.HashMap;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final static ConnectionPoolEvictingThread INSTANCE = new ConnectionPoolEvictingThread();

    private final HashMap<String, HttpClient> managedClients;

    private final Logger logger;

    private ConnectionPoolEvictingThread()
    {
        super( "HC4x-ConnectionPoolEvictingThread" );
        setDaemon( true );
        setPriority( MIN_PRIORITY );
        this.logger = LoggerFactory.getLogger( getClass() );
        this.managedClients = new HashMap<String, HttpClient>();
        logger.info( "Starting connection pool evicting thread..." );
        start();
    }

    public synchronized HttpClient register( final HttpClient client )
    {
        final Object id = client.getParams().getParameter( HttpClientUtil.CLIENT_ID_KEY );
        if ( id != null )
        {
            logger.debug( "Instance {} registered for eviction.", HttpClientUtil.getHttpClientDescription( client ) );
            return managedClients.put( String.valueOf( id ), client );
        }
        else
        {
            throw new IllegalArgumentException( "This is not a managed HTTPClient instance!" );
        }
    }

    public synchronized HttpClient unregister( final HttpClient client )
    {
        final Object id = client.getParams().getParameter( HttpClientUtil.CLIENT_ID_KEY );
        if ( id != null )
        {
            logger.debug( "Instance {} unregistered from eviction.", HttpClientUtil.getHttpClientDescription( client ) );
            return managedClients.remove( String.valueOf( id ) );
        }
        else
        {
            throw new IllegalArgumentException( "This is not a managed HTTPClient instance!" );
        }
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
                    for ( HttpClient managedClient : managedClients.values() )
                    {
                        if ( logger.isTraceEnabled() )
                        {
                            logger.trace( "Evicting pool of instance {}.",
                                HttpClientUtil.getHttpClientDescription( managedClient ) );
                        }
                        HttpClientUtil.evictConnectionManagerPool( managedClient );
                    }
                }
            }
        }
        catch ( InterruptedException e )
        {
            // bye bye
        }
    }
}
