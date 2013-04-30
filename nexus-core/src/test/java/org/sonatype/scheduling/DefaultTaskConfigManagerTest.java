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
package org.sonatype.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.inject.Module;
import org.junit.Test;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CScheduleConfig;
import org.sonatype.nexus.configuration.model.CScheduledTask;
import org.sonatype.nexus.test.NexusTestSupport;
import org.sonatype.scheduling.schedules.CronSchedule;
import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.MonthlySchedule;
import org.sonatype.scheduling.schedules.OnceSchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.scheduling.schedules.WeeklySchedule;
import org.sonatype.security.guice.SecurityModule;

public class DefaultTaskConfigManagerTest
    extends NexusTestSupport
{
    private DefaultScheduler defaultScheduler;

    private DefaultTaskConfigManager defaultManager;

    private NexusConfiguration applicationConfiguration;

    private static final String PROPERTY_KEY_START_DATE = "startDate";

    private static final String PROPERTY_KEY_END_DATE = "endDate";

    private static final String PROPERTY_KEY_CRON_EXPRESSION = "cronExpression";

    private static final String SCHEDULE_TYPE_ONCE = "once";

    private static final String SCHEDULE_TYPE_DAILY = "daily";

    private static final String SCHEDULE_TYPE_WEEKLY = "weekly";

    private static final String SCHEDULE_TYPE_MONTHLY = "monthly";

    private static final String SCHEDULE_TYPE_ADVANCED = "advanced";

    private static final String TASK_NAME = "test";

    private static final String CRON_EXPRESSION = "0 0/5 14,18,3-9,2 ? JAN,MAR,SEP MON-FRI 2002-2010";

    // private static final HashMap<String, Class> typeClassMap;

    // static
    // {
    // typeClassMap = new HashMap<String, Class>();
    // typeClassMap.put( SCHEDULE_TYPE_ONCE, COnceSchedule.class );
    // typeClassMap.put( SCHEDULE_TYPE_DAILY, CDailySchedule.class );
    // typeClassMap.put( SCHEDULE_TYPE_WEEKLY, CWeeklySchedule.class );
    // typeClassMap.put( SCHEDULE_TYPE_MONTHLY, CMonthlySchedule.class );
    // typeClassMap.put( SCHEDULE_TYPE_ADVANCED, CAdvancedSchedule.class );
    // }

    @Override
    protected Module[] getTestCustomModules()
    {
        return new Module[] { new SecurityModule() };
    }

    public void setUp()
        throws Exception
    {
        super.setUp();

        defaultScheduler = (DefaultScheduler) lookup( Scheduler.class.getName() );
        defaultManager = (DefaultTaskConfigManager) lookup( TaskConfigManager.class.getName() );
        applicationConfiguration = lookup( NexusConfiguration.class );
        applicationConfiguration.loadConfiguration();
    }

    @Test
    public void testStoreOnceSchedule()
        throws Exception
    {
        Date date = new Date();
        HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
        scheduleProperties.put( PROPERTY_KEY_START_DATE, date );
        genericTestStore( SCHEDULE_TYPE_ONCE, scheduleProperties );
    }

    @Test
    public void testStoreDailySchedule()
        throws Exception
    {
        Date startDate = new Date();
        Date endDate = new Date();
        HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
        scheduleProperties.put( PROPERTY_KEY_START_DATE, startDate );
        scheduleProperties.put( PROPERTY_KEY_END_DATE, endDate );
        genericTestStore( SCHEDULE_TYPE_DAILY, scheduleProperties );
    }

    @Test
    public void testStoreWeeklySchedule()
        throws Exception
    {
        Date startDate = new Date();
        Date endDate = new Date();
        HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
        scheduleProperties.put( PROPERTY_KEY_START_DATE, startDate );
        scheduleProperties.put( PROPERTY_KEY_END_DATE, endDate );
        genericTestStore( SCHEDULE_TYPE_WEEKLY, scheduleProperties );
    }

    @Test
    public void testStoreMonthlySchedule()
        throws Exception
    {
        Date startDate = new Date();
        Date endDate = new Date();
        HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
        scheduleProperties.put( PROPERTY_KEY_START_DATE, startDate );
        scheduleProperties.put( PROPERTY_KEY_END_DATE, endDate );
        genericTestStore( SCHEDULE_TYPE_MONTHLY, scheduleProperties );
    }

    @Test
    public void testStoreAdvancedSchedule()
        throws Exception
    {
        HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
        scheduleProperties.put( PROPERTY_KEY_CRON_EXPRESSION, CRON_EXPRESSION );
        genericTestStore( SCHEDULE_TYPE_ADVANCED, scheduleProperties );
    }

    @Test
    public void testInitializeCronSchedule()
        throws Exception
    {
        final CScheduledTask cst = new CScheduledTask();
        cst.setId( "foo-" + System.currentTimeMillis() );
        cst.setEnabled( true );
        cst.setName( "foo" );
        cst.setType( TestNexusTask.class.getName() );
        cst.setNextRun( new SimpleDateFormat( "yyyy-MM-DD hh:mm:ss" ).parse( "2099-01-01 20:00:00" ).getTime() );

        // System.out.println( new Date( cst.getNextRun() ) );

        final CScheduleConfig csc = new CScheduleConfig();
        csc.setType( SCHEDULE_TYPE_ADVANCED );
        csc.setCronCommand( "0 0 20 ? * TUE,THU,SAT" );
        cst.setSchedule( csc );

        defaultManager.initializeTasks( defaultScheduler, Arrays.asList( cst ) );

        final ScheduledTask<?> task = defaultScheduler.getTaskById( cst.getId() );
        assertThat( new Date( task.getNextRun().getTime() ), is( new Date( cst.getNextRun() ) ) );
    }

    public void genericTestStore( String scheduleType, HashMap<String, Object> scheduleProperties )
        throws ParseException
    {
        ScheduledTask<Integer> task = null;
        try
        {
            task = createScheduledTask( createSchedule( scheduleType, scheduleProperties ) );

            defaultManager.addTask( task );

            // loadConfig();

            assertThat( getTaskConfiguration().size(), equalTo( 1 ) );
            assertThat( TaskState.valueOf( ( (CScheduledTask) getTaskConfiguration().get( 0 ) ).getStatus() ),
                equalTo( TaskState.SUBMITTED ) );
            assertThat( ( (CScheduledTask) getTaskConfiguration().get( 0 ) ).getName(), equalTo( TASK_NAME ) );

            defaultManager.removeTask( task );

            // loadConfig();
            // assertTrue( getTaskConfiguration().size() == 0 );
        }
        finally
        {
            if ( task != null )
            {
                task.cancel();
                defaultManager.removeTask( task );
            }
        }
    }

    private Schedule createSchedule( String type, HashMap<String, Object> properties )
        throws ParseException
    {
        if ( SCHEDULE_TYPE_ONCE.equals( type ) )
        {
            return new OnceSchedule( (Date) properties.get( PROPERTY_KEY_START_DATE ) );
        }
        else if ( SCHEDULE_TYPE_DAILY.equals( type ) )
        {
            return new DailySchedule( (Date) properties.get( PROPERTY_KEY_START_DATE ),
                (Date) properties.get( PROPERTY_KEY_END_DATE ) );
        }
        else if ( SCHEDULE_TYPE_WEEKLY.equals( type ) )
        {
            Set<Integer> daysToRun = new HashSet<Integer>();
            daysToRun.add( new Integer( 1 ) );
            return new WeeklySchedule( (Date) properties.get( PROPERTY_KEY_START_DATE ),
                (Date) properties.get( PROPERTY_KEY_END_DATE ), daysToRun );
        }
        else if ( SCHEDULE_TYPE_MONTHLY.equals( type ) )
        {
            Set<Integer> daysToRun = new HashSet<Integer>();
            daysToRun.add( new Integer( 1 ) );
            return new MonthlySchedule( (Date) properties.get( PROPERTY_KEY_START_DATE ),
                (Date) properties.get( PROPERTY_KEY_END_DATE ), daysToRun );
        }
        else if ( SCHEDULE_TYPE_ADVANCED.equals( type ) )
        {
            return new CronSchedule( (String) properties.get( PROPERTY_KEY_CRON_EXPRESSION ) );
        }

        return null;
    }

    private ScheduledTask<Integer> createScheduledTask( Schedule schedule )
    {
        TestCallable callable = new TestCallable();
        return new DefaultScheduledTask<Integer>( "1", TASK_NAME, callable.getClass().getSimpleName(),
        // TODO this is only use for testing, but we are expecting that the TaskHint matches the Classname.
            defaultScheduler, callable, schedule );
    }

    private List<CScheduledTask> getTaskConfiguration()
    {
        return applicationConfiguration.getConfigurationModel().getTasks();
    }

    public class TestCallable
        implements Callable<Integer>
    {
        private int runCount = 0;

        public Integer call()
            throws Exception
        {
            return runCount++;
        }

        public int getRunCount()
        {
            return runCount;
        }
    }

}
