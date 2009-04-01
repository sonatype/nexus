package org.sonatype.nexus.restlight.testharness;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link RESTTestFixture} implementation meant to capture a single HTTP GET exchange. It has the ability to capture
 * expectations about the request URI (exact or via regex pattern) along with the base expectations captured in
 * {@link AbstractRESTTestFixture}. It can also manage a response {@link Document} for sending back to the client upon
 * successful request validation. This implementation also provides a GET-validating {@link Handler} implementation for
 * use in the HTTP {@link Server} instance, which is managed by the abstract base class.
 */
public class GETFixture
extends AbstractRESTTestFixture
{

    private Document responseDocument;

    private String uriPattern;

    private String exactURI;

    private boolean strictHeaders;

    public String getExactURI()
    {
        return exactURI;
    }

    public void setExactURI( final String exactURI )
    {
        this.exactURI = exactURI;
    }

    public String getURIPattern()
    {
        return uriPattern;
    }

    public void setURIPattern( final String uriPattern )
    {
        this.uriPattern = uriPattern;
    }

    public Document getResponseDocument()
    {
        return responseDocument;
    }

    public void setResponseDocument( final Document doc )
    {
        this.responseDocument = doc;
    }

    public boolean isHeaderCheckStrict()
    {
        return strictHeaders;
    }

    public void setStrictHeaders( final boolean strictHeaders )
    {
        this.strictHeaders = strictHeaders;
    }

    public Handler getTestHandler()
    {
        Handler handler = new AbstractHandler()
        {
            public void handle( final String target, final HttpServletRequest request, final HttpServletResponse response, final int dispatch )
            throws IOException, ServletException
            {
                Logger logger = LogManager.getLogger( GETFixture.class );

                if ( !"get".equalsIgnoreCase( request.getMethod() ) )
                {
                    logger.error( "Not a GET method: " + request.getMethod() );

                    response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Wrong method: " + request.getMethod() );
                }

                if ( !checkExpectedRequestHeaders( request, isHeaderCheckStrict() ) )
                {
                    logger.error( "Wrong request headers." );

                    response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Wrong headers." );
                }

                addResponseHeaders( response );

                String uri = getExactURI();
                String matchUri = getURIPattern();
                if ( uri != null )
                {
                    if ( !request.getRequestURI().equals( uri ) )
                    {
                        logger.error( "Exact URI check is wrong.\nExpected: " + uri + "\nActual: " + request.getRequestURI() );

                        response.sendError( HttpServletResponse.SC_NOT_FOUND );
                    }
                }
                else if ( matchUri != null )
                {
                    if ( !request.getRequestURI().matches( matchUri ) )
                    {
                        logger.error( "URI pattern check is wrong.\nExpected: " + matchUri + "\nActual: " + request.getRequestURI() );

                        response.sendError( HttpServletResponse.SC_NOT_FOUND );
                    }
                }

                Document doc = getResponseDocument();
                if ( doc == null )
                {
                    logger.info( "No response document set. Returning HTTP 404 status." );

                    response.sendError( HttpServletResponse.SC_NOT_FOUND );
                }
                else
                {
                    response.setContentType( "application/xml" );
                    response.setStatus( HttpServletResponse.SC_OK );
                    new XMLOutputter( Format.getPrettyFormat() ).output( doc, response.getOutputStream() );

                    response.flushBuffer();
                }

                ( (Request) request ).setHandled( true );
            }
        };

        return handler;
    }

    public GETFixture copy()
    {
        GETFixture fixture = new GETFixture();

        fixture.responseDocument = responseDocument;
        fixture.uriPattern = uriPattern;
        fixture.exactURI = exactURI;
        fixture.strictHeaders = strictHeaders;

        return fixture;
    }

}
