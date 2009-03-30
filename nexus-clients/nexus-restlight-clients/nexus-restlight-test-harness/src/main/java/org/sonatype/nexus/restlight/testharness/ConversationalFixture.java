package org.sonatype.nexus.restlight.testharness;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConversationalFixture
    extends AbstractRESTTestFixture
{
    
    
    private List<RESTTestFixture> conversation;
    
    private List<RESTTestFixture> traversedConversation;

    public List<RESTTestFixture> getConversation()
    {
        return conversation;
    }

    public void setConversation( List<RESTTestFixture> conversation )
    {
        this.conversation = conversation;
        this.traversedConversation = new ArrayList<RESTTestFixture>();
    }
    
    public List<RESTTestFixture> verifyConversationWasFinished()
    {
        if ( conversation == null )
        {
            return Collections.emptyList();
        }
        
        List<RESTTestFixture> remaining = new ArrayList<RESTTestFixture>( conversation );
        remaining.removeAll( traversedConversation );
        
        return remaining;
    }

    /**
     * REQ:
     * 1. verify method
     * 2. verify request headers, if present
     * 3. verify request body, if present and method is POST/PUT
     * 
     * RESP:
     * 4. set response headers, if present
     * 5. set response code
     * 6. set response body, if present
     */
    public Handler getTestHandler()
    {
        return new AbstractHandler()
        {
            
            private int conversationIndex = 0;

            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                Logger logger = LogManager.getLogger( ConversationalFixture.class );
                
                if ( conversation == null || conversation.isEmpty() )
                {
                    logger.error( "Missing conversation." );
                    
                    response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No conversation specified." );
                    
                    ((Request) request).setHandled( true );
                }
                else if ( conversation.size() <= conversationIndex )
                {
                    logger.error( "Out of conversation elements. No conversation element at index: " + conversationIndex );
                    
                    response.sendError( HttpServletResponse.SC_NOT_IMPLEMENTED, "Out of conversation elements. No conversation element at index: " + conversationIndex );
                    
                    ((Request) request).setHandled( true );
                }
                else
                {
                    RESTTestFixture fixture = conversation.get( conversationIndex++ );
                    
                    fixture.getTestHandler().handle( target, request, response, dispatch );
                    
                    traversedConversation.add( fixture );
                }
            }
            
        };
    }

    public ConversationalFixture copy()
    {
        ConversationalFixture fixture = new ConversationalFixture();
        
        fixture.conversation = conversation == null ? null : new ArrayList<RESTTestFixture>( conversation );
        fixture.traversedConversation = conversation == null ? null : new ArrayList<RESTTestFixture>();
        
        return fixture;
    }

}
