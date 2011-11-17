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
package org.sonatype.nexus.rest.schedules;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceStatus;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceStatusResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.scheduling.NoSuchTaskException;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.TaskState;

/**
 * @author tstevens
 */
@Component( role = PlexusResource.class, hint = "ScheduledServiceRunPlexusResource" )
@Path( ScheduledServiceRunPlexusResource.RESOURCE_URI )
@Produces( { "application/xml", "application/json" } )
public class ScheduledServiceRunPlexusResource
    extends AbstractScheduledServicePlexusResource
{
    public static final String RESOURCE_URI = "/schedule_run/{" + SCHEDULED_SERVICE_ID_KEY + "}";

    public ScheduledServiceRunPlexusResource()
    {
        setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/schedule_run/*", "authcBasic,perms[nexus:tasksrun]" );
    }

    /**
     * Run the specified scheduled task right now. Will then be rescheduled upon completion for normal run.
     * 
     * @param scheduledServiceId The scheduled task to access.
     */
    @Override
    @GET
    @ResourceMethodSignature( pathParams = { @PathParam( AbstractScheduledServicePlexusResource.SCHEDULED_SERVICE_ID_KEY ) }, output = ScheduledServiceResourceStatusResponse.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        ScheduledServiceResourceStatusResponse result = null;

        final String scheduledServiceId = getScheduledServiceId( request );

        try
        {
            ScheduledTask<?> task = getNexusScheduler().getTaskById( scheduledServiceId );

            task.runNow();

            ScheduledServiceBaseResource resource = getServiceRestModel( task );

            if ( resource != null )
            {
                ScheduledServiceResourceStatus resourceStatus = new ScheduledServiceResourceStatus();
                resourceStatus.setResource( resource );
                resourceStatus.setResourceURI( createChildReference( request, this, task.getId() ).toString() );
                resourceStatus.setStatus( task.getTaskState().toString() );
                resourceStatus.setReadableStatus( getReadableState( task.getTaskState() ) );
                resourceStatus.setCreated( task.getScheduledAt() == null ? "n/a" : task.getScheduledAt().toString() );
                resourceStatus.setLastRunResult( TaskState.BROKEN.equals( task.getTaskState() ) ? "Error" : "Ok" );
                resourceStatus.setLastRunTime( task.getLastRun() == null ? "n/a" : task.getLastRun().toString() );
                resourceStatus.setNextRunTime( task.getNextRun() == null ? "n/a" : task.getNextRun().toString() );
                if ( task.getScheduledAt() != null )
                {
                    resourceStatus.setCreatedInMillis( task.getScheduledAt().getTime() );
                }
                if ( task.getLastRun() != null )
                {
                    resourceStatus.setLastRunTimeInMillis( task.getLastRun().getTime() );
                }
                if ( task.getNextRun() != null )
                {
                    resourceStatus.setNextRunTimeInMillis( task.getNextRun().getTime() );
                }

                result = new ScheduledServiceResourceStatusResponse();
                result.setData( resourceStatus );
            }
            else
            {
                throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Invalid schedule id ("
                    + scheduledServiceId + "), can't load task." );
            }
            return result;
        }
        catch ( NoSuchTaskException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "There is no task with ID="
                + scheduledServiceId );
        }
    }

    /**
     * Cancel the execution of an existing scheduled task.
     * 
     * @param scheduledServiceId The scheduled task to cancel.
     */
    @Override
    @DELETE
    @ResourceMethodSignature( pathParams = { @PathParam( ScheduledServicePlexusResource.SCHEDULED_SERVICE_ID_KEY ) } )
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        try
        {
            getNexusScheduler().getTaskById( getScheduledServiceId( request ) ).cancelOnly();

            response.setStatus( Status.SUCCESS_NO_CONTENT );
        }
        catch ( NoSuchTaskException e )
        {
            response.setStatus( Status.CLIENT_ERROR_NOT_FOUND, "Scheduled service not found!" );
        }
    }

    // ==

    protected String getScheduledServiceId( Request request )
    {
        return request.getAttributes().get( SCHEDULED_SERVICE_ID_KEY ).toString();
    }
}
