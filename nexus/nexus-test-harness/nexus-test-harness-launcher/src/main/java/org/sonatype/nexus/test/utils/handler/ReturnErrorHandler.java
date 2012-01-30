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
package org.sonatype.nexus.test.utils.handler;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.restlet.data.Method;

/**
 * Utility handler for tests needing a misbehaving proxy.
 * 
 * @since 2.0
 */
public class ReturnErrorHandler
        extends AbstractHandler
    {
        private final int status;

        private boolean headOk = true;

        public ReturnErrorHandler( final int status )
        {
            this.status = status;
        }

        public ReturnErrorHandler( final int status, final boolean headOk )
        {
            this( status );
            this.headOk = headOk;
        }

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException,
            ServletException
        {

            if ( isHeadOk() && request.getMethod().equals( Method.HEAD ) )
            {
                response.setContentType( "text/html" );
                response.setStatus( HttpServletResponse.SC_OK );
                response.getWriter().println( "ok" );
                ( (Request) request ).setHandled( true );
            }
            else
            {
                response.setContentType( "text/html" );
                response.setStatus( status );
                response.getWriter().println( "error" );
                ( (Request) request ).setHandled( true );
            }
        }

        public boolean isHeadOk()
        {
            return headOk;
        }
    }
