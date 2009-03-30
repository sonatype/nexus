package org.sonatype.nexus.restlight.testharness;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.mortbay.jetty.Server;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractRESTTestFixture
    implements RESTTestFixture
{

    private static final int MAX_PORT_TRIES = 10;

    private static final String TEST_PORT_SYSPROP = "test.port";

    private Server server;

    private int port;

    private boolean debugEnabled;

    private Map<String, Set<String>> expectedRequestHeaders;

    private Map<String, Set<String>> responseHeaders;

    public Map<String, Set<String>> getExpectedRequestHeaders()
    {
        return expectedRequestHeaders;
    }

    public void setExpectedRequestHeaders( Map<String, Set<String>> requestHeaders )
    {
        this.expectedRequestHeaders = requestHeaders;
    }

    public Map<String, Set<String>> getResponseHeaders()
    {
        return responseHeaders;
    }

    public void setResponseHeaders( Map<String, Set<String>> responseHeaders )
    {
        this.responseHeaders = responseHeaders;
    }

    public Server getServer()
    {
        return server;
    }

    public int getPort()
    {
        return port;
    }

    public boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public void setDebugEnabled( boolean debugEnabled )
    {
        this.debugEnabled = debugEnabled;
    }

    protected void addResponseHeaders( HttpServletResponse response )
    {
        if ( getResponseHeaders() != null )
        {
            for ( Map.Entry<String, Set<String>> headers : getResponseHeaders().entrySet() )
            {
                String key = headers.getKey();
                for ( String value : headers.getValue() )
                {
                    response.addHeader( key, value );
                }
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    protected boolean checkExpectedRequestHeaders( HttpServletRequest request, boolean strict )
    {
        if ( getExpectedRequestHeaders() != null )
        {
            Map<String, Set<String>> requestHeaders = new HashMap<String, Set<String>>( getExpectedRequestHeaders() );
            for ( Map.Entry<String, Set<String>> headerValues : requestHeaders.entrySet() )
            {
                Set<String> values = new HashSet<String>( headerValues.getValue() );

                Enumeration<String> detected = request.getHeaders( headerValues.getKey() );
                if ( detected != null )
                {
                    while ( detected.hasMoreElements() )
                    {
                        if ( strict && !values.remove( detected.nextElement() ) )
                        {
                            return false;
                        }
                    }

                    if ( !values.isEmpty() )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    protected void setupLogging()
    {
        if ( !LogManager.getRootLogger().getAllAppenders().hasMoreElements() )
        {
            LogManager.getRootLogger().addAppender( new ConsoleAppender( new SimpleLayout() ) );
        }

        if ( isDebugEnabled() )
        {
            LogManager.getRootLogger().setLevel( Level.DEBUG );
        }
        else
        {
            LogManager.getRootLogger().setLevel( Level.INFO );
        }
    }

    public void startServer()
        throws Exception
    {
        setupLogging();

        Logger logger = LogManager.getLogger( getClass() );

        String portStr = System.getProperty( TEST_PORT_SYSPROP );

        if ( portStr != null )
        {
            port = Integer.parseInt( portStr );
            logger.info( "Using port: " + port + ", given by system property '" + TEST_PORT_SYSPROP + "'." );
        }
        else
        {
            logger.info( "Randomly looking for an open port..." );
            int tries = 0;

            // loop until we can't connect to the given port.
            while ( tries < MAX_PORT_TRIES )
            {
                port = ( Math.abs( new Random().nextInt() ) % 63000 ) + 1024;

                logger.info( "(try " + ( tries + 1 ) + "/" + MAX_PORT_TRIES + ") Checking whether port: " + port
                    + " is available..." );

                Socket sock = new Socket();
                sock.setSoTimeout( 1 );
                sock.setSoLinger( true, 1 );

                try
                {
                    sock.connect( new InetSocketAddress( "127.0.0.1", port ) );
                }
                catch ( SocketException e )
                {
                    if ( e.getMessage().indexOf( "Connection refused" ) > -1 )
                    {
                        logger.info( "Port: " + port + " appears to be available!" );
                        break;
                    }
                }
                finally
                {
                    sock.close();
                }

                tries++;
            }

            if ( tries >= MAX_PORT_TRIES )
            {
                throw new IllegalStateException( "Cannot find open port after " + tries + " tries. Giving up." );
            }
        }

        logger.info( "Starting test server on port: " + port );

        server = new Server( port );
        
        server.addHandler( getTestHandler() );

        server.start();
    }

    public void stopServer()
        throws Exception
    {
        LogManager.getLogger( getClass() ).info( "Stopping test server." );
        
        if ( server != null && server.isStarted() )
        {
            server.stop();
        }
    }

}
