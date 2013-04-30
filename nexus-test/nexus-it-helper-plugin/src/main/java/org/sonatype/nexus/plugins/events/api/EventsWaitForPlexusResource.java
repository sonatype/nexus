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
package org.sonatype.nexus.plugins.events.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.events.EventInspectorHost;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.sisu.goodies.eventbus.EventBus;

@Named
@Singleton
public class EventsWaitForPlexusResource
    extends AbstractPlexusResource
{

    private static final String RESOURCE_URI = "/events/waitFor";

    private final EventInspectorHost eventInspectorHost;

    private final EventBus eventBus;

    @Inject
    public EventsWaitForPlexusResource( final EventInspectorHost eventInspectorHost,
                                        final EventBus eventBus )
    {
        this.eventInspectorHost = checkNotNull( eventInspectorHost );
        this.eventBus = checkNotNull( eventBus );
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( getResourceUri(), "anon" );
    }

    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        Form form = request.getResourceRef().getQueryAsForm();
        final long window = Long.parseLong( form.getFirstValue( "window", "10000" ) );
        final long timeout = Long.parseLong( form.getFirstValue( "timeout", "60000" ) );

        final AtomicLong lastEventTime = new AtomicLong( System.currentTimeMillis() );

        final Object recorder = new Object()
        {

            public void on( final Object event )
            {
                lastEventTime.set( System.currentTimeMillis() );
            }

        };

        eventBus.register( recorder );

        try
        {
            final long startTime = System.currentTimeMillis();
            while ( System.currentTimeMillis() - startTime <= timeout )
            {
                if ( eventInspectorHost.isCalmPeriod()
                    && System.currentTimeMillis() - lastEventTime.get() >= window )
                {
                    response.setStatus( Status.SUCCESS_OK );
                    return "Ok";
                }
                Thread.sleep( 500 );
            }
        }
        catch ( final InterruptedException ignore )
        {
            // ignore
        }
        finally
        {
            eventBus.unregister( recorder );
        }

        response.setStatus( Status.SUCCESS_ACCEPTED );
        return "Still munching on them...";
    }

}
