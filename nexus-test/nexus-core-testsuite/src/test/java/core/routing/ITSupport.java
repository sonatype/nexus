/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package core.routing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.content.Content.Directive;
import org.sonatype.nexus.client.core.subsystem.routing.Routing;
import org.sonatype.nexus.testsuite.client.RoutingTest;

import com.google.common.io.Closeables;

import core.NexusCoreITSupport;

/**
 * Support class for Automatic Routing Core feature (NEXUS-5472), aka "proxy404".
 * 
 * @author cstamas
 * @since 2.4
 */
public abstract class ITSupport
    extends NexusCoreITSupport
{
    protected ITSupport( final String nexusBundleCoordinates )
    {
        super( nexusBundleCoordinates );
    }

    /**
     * Returns {@link Routing} client subsystem.
     * 
     * @return client for routing.
     */
    public Routing routing()
    {
        return client().getSubsystem( Routing.class );
    }

    /**
     * Returns {@link RoutingTest} client subsystem.
     * 
     * @return client for routing ITs.
     */
    public RoutingTest routingTest()
    {
        return client().getSubsystem( RoutingTest.class );
    }

    /**
     * Does HTTP GET against given URL.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    protected HttpResponse executeGet( final String url )
        throws IOException
    {
        InputStream entityStream = null;
        try
        {
            final HttpClient httpClient = new DefaultHttpClient();
            final HttpGet get = new HttpGet( url );
            final HttpResponse httpResponse = httpClient.execute( get );
            return httpResponse;
        }
        catch ( IOException e )
        {
            Closeables.closeQuietly( entityStream );
            throw e;
        }
    }

    /**
     * Fetches file from given URL.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    protected InputStream getPrefixFileFrom( final String url )
        throws IOException
    {
        InputStream entityStream = null;
        try
        {
            final HttpResponse httpResponse = executeGet( url );
            assertThat( httpResponse.getStatusLine().getStatusCode(), equalTo( 200 ) );
            assertThat( httpResponse.getEntity(), is( notNullValue() ) );
            entityStream = httpResponse.getEntity().getContent();
            return entityStream;
        }
        catch ( IOException e )
        {
            Closeables.closeQuietly( entityStream );
            throw e;
        }
    }

    protected boolean exists( final Location location, Directive directive )
        throws IOException
    {
        return content().existsWith( location, directive );
    }

    protected boolean noscrape( final Location location, Directive directive )
        throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        {
            content().downloadWith( location, directive, buf );
            return new String( buf.toByteArray(), "UTF-8" ).startsWith( "@ unsupported" );
        }
        catch ( NexusClientNotFoundException e )
        {
            return false; // requested file was not found, so the repository is not marked as no-scrape
        }
    }

}
