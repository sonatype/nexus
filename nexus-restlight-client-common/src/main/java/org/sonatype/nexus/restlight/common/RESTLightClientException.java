package org.sonatype.nexus.restlight.common;

/**
 * Exception indicating a failure to communicate with the Nexus server. This normally means either an 
 * I/O failure or a failure to parse the response message.
 */
public class RESTLightClientException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    public RESTLightClientException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public RESTLightClientException( String message )
    {
        super( message );
    }

}
