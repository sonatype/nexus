/*
 * Copyright (c) 2007-2011 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package org.sonatype.nexus.bootstrap;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread which listens for command messages to control the JVM.
 *
 * @since 2.2
 */
public class ShutdownIfNotAliveThread
    extends Thread
{

    static final String LOCALHOST = "127.0.0.1";

    private static Logger log = LoggerFactory.getLogger( ShutdownIfNotAliveThread.class );

    private final Launcher launcher;

    private int port;

    private int pingInterval;

    private int timeout;

    private boolean running;

    private Socket socket;

    public ShutdownIfNotAliveThread( final Launcher launcher,
                                     final int port,
                                     final int pingInterval,
                                     final int timeout )
        throws IOException
    {
        if ( launcher == null )
        {
            throw new NullPointerException();
        }
        this.launcher = launcher;
        this.port = port;
        this.pingInterval = pingInterval;
        this.timeout = timeout;
        this.running = true;

        this.setDaemon( true );
        setName( "Shutdown if not alive" );
    }

    @Override
    public void run()
    {
        log.debug( "Shutdown thread pinging on port {} every {} milliseconds", port, pingInterval );

        while ( running )
        {
            try
            {
                ping();
                sleep( pingInterval );
            }
            catch ( InterruptedException ignore )
            {
                ping();
            }
        }

        log.debug( "Done" );
    }

    private void ping()
    {
        try
        {
            log.debug( "Pinging on port {} ...", port );

            socket = new Socket();
            socket.connect( new InetSocketAddress( LOCALHOST, port ), timeout );

        }
        catch ( ConnectException e )
        {
            // TODO this check is quite a hack. we need a better way to determine that connection was refused
            if ( "Connection refused".equals( e.getMessage() ) )
            {
                running = false;
                shutdown();
            }
        }
        catch ( IOException e )
        {
            // log.debug( "Skipped {}:{}", e.getClass().getName(), e.getMessage() );
        }
        finally
        {
            close( socket );
        }
    }

    void shutdown()
    {
        log.info( "Shutting down as there is no ping response on port {}", port );
        launcher.commandStop();
    }

    public void stopRunning()
    {
        running = false;
        close( socket );
    }

    private void close( final Socket socket )
    {
        if ( socket != null )
        {
            try
            {
                socket.close();
            }
            catch ( IOException ignore )
            {
                // ignore
            }
        }
    }

}
