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
package org.sonatype.nexus.integrationtests.nexus533;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceWeeklyResource;
import org.sonatype.nexus.tasks.descriptors.UpdateIndexTaskDescriptor;

public class Nexus533TaskWeeklyIT
    extends AbstractNexusTasksIntegrationIT<ScheduledServiceWeeklyResource>
{

    private static ScheduledServiceWeeklyResource scheduledTask;

    @Override
    public ScheduledServiceWeeklyResource getTaskScheduled()
    {
        if ( scheduledTask == null )
        {
            scheduledTask = new ScheduledServiceWeeklyResource();
            scheduledTask.setEnabled( true );
            scheduledTask.setId( null );
            scheduledTask.setName( "taskWeekly" );
            scheduledTask.setSchedule( "weekly" );
            // A future date
            Date startDate = DateUtils.addDays( new Date(), 10 );
            startDate = DateUtils.round( startDate, Calendar.DAY_OF_MONTH );
            scheduledTask.setStartDate( String.valueOf( startDate.getTime() ) );
            scheduledTask.setRecurringTime( "03:30" );
            scheduledTask.setRecurringDay( Arrays.asList( new String[] { "monday", "wednesday", "friday" } ) );

            scheduledTask.setTypeId( UpdateIndexTaskDescriptor.ID );

            ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
            prop.setKey( "repositoryId" );
            prop.setValue( "all_repo" );
            scheduledTask.addProperty( prop );
        }
        return scheduledTask;
    }

    @Override
    public void updateTask( ScheduledServiceWeeklyResource scheduledTask )
    {
        scheduledTask.setRecurringTime( "00:00" );
    }

}
