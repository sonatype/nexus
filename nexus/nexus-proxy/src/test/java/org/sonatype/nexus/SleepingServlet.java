/*
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

package org.sonatype.nexus;

import org.mortbay.jetty.servlet.DefaultServlet;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * An extension of the jetty DefaultServlet that sleeps for the amount of time specified in the servlet parameter: <code>sleepTime</code>.
 */
public class SleepingServlet extends DefaultServlet
{
    private long sleepTime = 0;

    @Override
    public void init()
        throws UnavailableException
    {
        super.init();

        String sleepTimeString = getInitParameter( "sleepTime" );
        if( sleepTimeString != null )
        {
            sleepTime = Long.parseLong( sleepTimeString );
        }
    }

    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {
        try
        {
            Thread.sleep( sleepTime );
        }
        catch ( InterruptedException e )
        {
            log( "Thread interrupted", e );
        }

        super.service( req, resp );
    }
}
