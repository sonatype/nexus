package org.sonatype.nexus.mock;

import junit.framework.TestCase;

import org.restlet.Client;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Response;

public class SimpleTest
    extends TestCase
{
    protected MockNexusEnvironment mockNexusEnvironment;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        mockNexusEnvironment = new MockNexusEnvironment( 12345, "/nexus" );

        mockNexusEnvironment.start();
    }

    protected void tearDown()
        throws Exception
    {
        mockNexusEnvironment.stop();

        super.tearDown();
    }

    public void testStatus()
        throws Exception
    {
        Client client = new Client( Protocol.HTTP );

        Response response = client.get( new Reference( "http://localhost:12345/nexus/service/local/status" ) );

        assertEquals( 200, response.getStatus().getCode() );
        
        Thread.sleep( 100000 );

        // test content etc
    }
}
