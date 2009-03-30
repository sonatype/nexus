package org.sonatype.nexus.restlight.testharness;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;

public interface RESTTestFixture
{

    Server getServer();

    int getPort();

    Handler getTestHandler();
    
    boolean isDebugEnabled();

    void setDebugEnabled( boolean debugEnabled );

    void startServer()
        throws Exception;

    void stopServer()
        throws Exception;
    
    RESTTestFixture copy();

}