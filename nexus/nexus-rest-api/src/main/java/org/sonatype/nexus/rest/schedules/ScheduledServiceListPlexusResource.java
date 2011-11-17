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

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceStatus;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceStatusResponse;
import org.sonatype.nexus.scheduling.NexusTask;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.SchedulerTask;
import org.sonatype.scheduling.TaskState;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.scheduling.schedules.Schedule;

/**
 * @author tstevens
 */
@Component( role = PlexusResource.class, hint = "ScheduledServiceListPlexusResource" )
@Path( ScheduledServiceListPlexusResource.RESOURCE_URI )
@Produces( { "application/xml", "application/json" } )
@Consumes( { "application/xml", "application/json" } )
public class ScheduledServiceListPlexusResource
    extends AbstractScheduledServicePlexusResource
{
    public static final String RESOURCE_URI = "/schedules";

    private static final Long UNKNOWN = null;

    public ScheduledServiceListPlexusResource()
    {
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return new ScheduledServiceResourceResponse();
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:tasks]" );
    }

    private boolean isAllTasks( Request request )
    {
        Form form = request.getResourceRef().getQueryAsForm();

        if ( form != null )
        {
            String result = form.getFirstValue( "allTasks" );

            if ( result != null )
            {
                return result.equalsIgnoreCase( "true" );
            }
        }

        return false;
    }

    /**
     * Retrieve a list of scheduled tasks currently configured in nexus.
     * 
     * @param allTasks If true, will return all tasks, even non-exposed tasks.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = ScheduledServiceListResourceResponse.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        boolean allTasks = isAllTasks( request );

        Map<String, List<ScheduledTask<?>>> tasksMap = getNexusScheduler().getAllTasks();

        ScheduledServiceListResourceResponse result = new ScheduledServiceListResourceResponse();

        for ( String key : tasksMap.keySet() )
        {
            List<ScheduledTask<?>> tasks = tasksMap.get( key );

            for ( ScheduledTask<?> task : tasks )
            {
                boolean isExposed = true;

                SchedulerTask<?> st = task.getSchedulerTask();

                if ( st != null && st instanceof NexusTask<?> )
                {
                    isExposed = ( (NexusTask<?>) st ).isExposed();
                }

                if ( allTasks || isExposed )
                {
                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Building task '" + task.getName() + "' of type '" + task.getType() + "'." );
                    }

                    ScheduledServiceListResource item = new ScheduledServiceListResource();
                    item.setResourceURI( createChildReference( request, this, task.getId() ).toString() );
                    item.setLastRunResult( getLastRunResult( task ) );
                    item.setId( task.getId() );
                    item.setName( task.getName() );
                    item.setStatus( task.getTaskState().toString() );
                    item.setReadableStatus( getReadableState( task.getTaskState() ) );
                    item.setTypeId( task.getType() );
                    ScheduledTaskDescriptor descriptor =
                        getNexusConfiguration().getScheduledTaskDescriptor( task.getType() );
                    if ( descriptor != null )
                    {
                        item.setTypeName( descriptor.getName() );
                    }
                    item.setCreated( task.getScheduledAt() == null ? "n/a" : task.getScheduledAt().toString() );
                    item.setLastRunTime( task.getLastRun() == null ? "n/a" : task.getLastRun().toString() );
                    final Date nextRunTime = getNextRunTime( task );
                    item.setNextRunTime( nextRunTime == null ? "n/a" : nextRunTime.toString() );
                    if ( task.getScheduledAt() != null )
                    {
                        item.setCreatedInMillis( task.getScheduledAt().getTime() );
                    }
                    if ( task.getLastRun() != null )
                    {
                        item.setLastRunTimeInMillis( task.getLastRun().getTime() );
                    }
                    if ( nextRunTime != null )
                    {
                        item.setNextRunTimeInMillis( nextRunTime.getTime() );
                    }
                    item.setSchedule( getScheduleShortName( task.getSchedule() ) );
                    item.setEnabled( task.isEnabled() );

                    result.addData( item );
                }
            }

        }

        return result;
    }

    /**
     * Add a new scheduled service to nexus.
     */
    @Override
    @POST
    @ResourceMethodSignature( input = ScheduledServiceResourceResponse.class, output = ScheduledServiceResourceStatusResponse.class )
    public Object post( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        ScheduledServiceResourceResponse serviceRequest = (ScheduledServiceResourceResponse) payload;
        ScheduledServiceResourceStatusResponse result = null;

        if ( serviceRequest != null )
        {
            ScheduledServiceBaseResource serviceResource = serviceRequest.getData();
            try
            {
                Schedule schedule = getModelSchedule( serviceRequest.getData() );
                ScheduledTask<?> task = null;

                if ( schedule != null )
                {
                    task =
                        getNexusScheduler().schedule( getModelName( serviceResource ),
                            getModelNexusTask( serviceResource, request ), schedule );
                }
                else
                {
                    task =
                        getNexusScheduler().schedule( getModelName( serviceResource ),
                            getModelNexusTask( serviceResource, request ), new ManualRunSchedule() );
                }

                task.setEnabled( serviceResource.isEnabled() );

                // Need to store the enabled flag update
                getNexusScheduler().updateSchedule( task );

                ScheduledServiceResourceStatus resourceStatus = new ScheduledServiceResourceStatus();
                resourceStatus.setResource( serviceResource );
                // Just need to update the id, as the incoming data is a POST w/ no id
                resourceStatus.getResource().setId( task.getId() );
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
            catch ( RejectedExecutionException e )
            {
                getLogger().warn( "Execution of task " + getModelName( serviceResource ) + " rejected." );

                throw new ResourceException( Status.CLIENT_ERROR_CONFLICT, e.getMessage() );
            }
            catch ( ParseException e )
            {
                getLogger().warn( "Unable to parse data for task " + getModelName( serviceResource ) );

                throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(),
                    getNexusErrorResponse( "cronCommand", e.getMessage() ) );
            }
            catch ( InvalidConfigurationException e )
            {
                handleConfigurationException( e );
            }
        }
        return result;
    }

}
