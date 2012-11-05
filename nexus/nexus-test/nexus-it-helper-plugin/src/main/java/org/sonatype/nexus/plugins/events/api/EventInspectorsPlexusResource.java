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
package org.sonatype.nexus.plugins.events.api;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
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
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "EventInspectorsPlexusResource" )
public class EventInspectorsPlexusResource
    extends AbstractPlexusResource
{
    private static final String RESOURCE_URI = "/eventInspectors/isCalmPeriod";

    @Requirement
    private EventInspectorHost eventInspectorHost;

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
        boolean waitForCalm = Boolean.parseBoolean( form.getFirstValue( "waitForCalm" ) );

        if ( waitForCalm )
        {
            for ( int i = 0; i < 100; i++ )
            {
                try
                {
                    Thread.sleep( 500 );
                }
                catch ( InterruptedException e )
                {
                }
                
                if ( eventInspectorHost.isCalmPeriod() )
                {
                    response.setStatus( Status.SUCCESS_OK );
                    return "Ok";
                }
            }
            
            response.setStatus( Status.SUCCESS_ACCEPTED );
            return "Still munching on them...";
        }
        else
        {
            if ( eventInspectorHost.isCalmPeriod() )
            {
                response.setStatus( Status.SUCCESS_OK );
                return "Ok";
            }
            else
            {
                response.setStatus( Status.SUCCESS_ACCEPTED );
                return "Still munching on them...";
            }
        }
    }

}
