package org.sonatype.nexus.mock.rest;

import org.jsecurity.io.ResourceException;

/**
 * This class is the gate to "mock"
 * @author cstamas
 *
 */
public class MockHelper
{
    public static Object getMockContentFor( String method, String uri )
        throws ResourceException
    {
        return "dummy";
    }

}
