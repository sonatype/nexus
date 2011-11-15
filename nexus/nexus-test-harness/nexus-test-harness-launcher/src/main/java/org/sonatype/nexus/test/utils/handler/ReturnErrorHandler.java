/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
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
 * @since 1.10.0
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
