package org.sonatype.nexus.restlight.testharness;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class POSTFixture
    extends AbstractRESTTestFixture
{
    
    private Document requestDocument;
    private Document responseDocument;
    private String uriPattern;
    private String exactURI;
    private boolean strictHeaders;
    
    private int responseStatus = HttpServletResponse.SC_OK;

    public Document getResponseDocument()
    {
        return responseDocument;
    }

    public void setResponseDocument( Document responseDocument )
    {
        this.responseDocument = responseDocument;
    }

    public int getResponseStatus()
    {
        return responseStatus;
    }

    public void setResponseStatus( int responseStatus )
    {
        this.responseStatus = responseStatus;
    }

    public Document getRequestDocument()
    {
        return requestDocument;
    }

    public void setRequestDocument( Document requestDocument )
    {
        this.requestDocument = requestDocument;
    }

    public String getURIPattern()
    {
        return uriPattern;
    }

    public void setURIPattern( String uriPattern )
    {
        this.uriPattern = uriPattern;
    }

    public String getExactURI()
    {
        return exactURI;
    }

    public void setExactURI( String exactURI )
    {
        this.exactURI = exactURI;
    }

    public boolean isStrictHeaders()
    {
        return strictHeaders;
    }

    public void setStrictHeaders( boolean strictHeaders )
    {
        this.strictHeaders = strictHeaders;
    }

    public Handler getTestHandler()
    {
        return new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                Logger logger = LogManager.getLogger( POSTFixture.class );
                
                if ( !"post".equalsIgnoreCase( request.getMethod() ) )
                {
                    logger.error( "Not a POST request: " + request.getMethod() );
                    
                    response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Wrong method: " + request.getMethod() );
                }
                
                if ( !checkExpectedRequestHeaders( request, isStrictHeaders() ) )
                {
                    logger.error( "Request headers are wrong." );
                    
                    response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Wrong headers." );
                }
                
                if ( requestDocument != null )
                {
                    try
                    {
                        Document input = new SAXBuilder().build( request.getInputStream() );
                        
                        XMLOutputter outputter = new XMLOutputter( Format.getCompactFormat() );
                        String expected = outputter.outputString( getRequestDocument() ).trim();
                        String actual = outputter.outputString( input ).trim();
                        
                        if ( !expected.equals( actual ) )
                        {
                            logger.error( "Request body is wrong.\n\nExpected:\n\n" + expected + "\n\nActual:\n\n" + actual + "\n\n" );
                            
                            response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Invalid body: doesn't match expected content." );
                        }
                    }
                    catch ( JDOMException e )
                    {
                        logger.error( "Request body cannot be parsed." );
                        
                        response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Invalid body: " + e.getMessage() );
                    }
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
                
                response.setContentType( "application/xml" );
                response.setStatus( responseStatus );
                Document responseDoc = getResponseDocument();
                
                if ( responseDoc != null )
                {
                    new XMLOutputter( Format.getPrettyFormat() ).output( responseDoc, response.getOutputStream() );

                    response.flushBuffer();
                }

                ( (Request) request ).setHandled( true );
            }
        };
    }

    public POSTFixture copy()
    {
        POSTFixture fixture = new POSTFixture();
        
        fixture.requestDocument = requestDocument;
        fixture.responseDocument = responseDocument;
        fixture.uriPattern = uriPattern;
        fixture.exactURI = exactURI;
        fixture.strictHeaders = strictHeaders;
        fixture.responseStatus = responseStatus;
        
        return fixture;
    }

}
