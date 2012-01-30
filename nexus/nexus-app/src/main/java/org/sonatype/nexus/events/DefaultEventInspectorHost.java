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
package org.sonatype.nexus.events;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.slf4j.Logger;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.events.AsynchronousEventInspector;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.threads.NexusThreadFactory;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.plexus.appevents.Event;

import com.google.common.annotations.VisibleForTesting;

/**
 * A default implementation of EventInspectorHost, a component simply collecting all EventInspectors and re-emitting
 * events towards them in they wants to receive it. For ones implementing {@link AsynchronousEventInspector} a cached
 * thread pool is used to execute them in a separate thread. In case of pool saturation, the caller thread will execute
 * the async inspector (as if it would be non-async one). Host cannot assume and does not know which inspector is
 * "less important" (could be dropped without having data loss in case of excessive load for example), hence it applies
 * same rules to all inspectors.
 * 
 * @author cstamas
 */
@Component( role = EventInspectorHost.class )
public class DefaultEventInspectorHost
    extends AbstractLoggingComponent
    implements EventInspectorHost, Disposable
{
    private final int HOST_THREAD_POOL_SIZE = SystemPropertiesHelper.getInteger(
        "org.sonatype.nexus.events.DefaultEventInspectorHost.poolSize", 500 );

    private final ThreadPoolExecutor hostThreadPool;

    @Requirement( role = EventInspector.class )
    private Map<String, EventInspector> eventInspectors;

    public DefaultEventInspectorHost()
    {
        // direct hand-off used! Host pool will use caller thread to execute async inspectors when pool full!
        this.hostThreadPool =
            new ThreadPoolExecutor( 0, HOST_THREAD_POOL_SIZE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new NexusThreadFactory( "nxevthost", "Event Inspector Host" ), new CallerRunsPolicy() );
    }

    @VisibleForTesting 
    public DefaultEventInspectorHost( final Map<String, EventInspector> eventInspectors )
    {
        this();
        this.eventInspectors = eventInspectors;
    }

    // == Disposable iface, to manage ExecutorService lifecycle

    public void dispose()
    {
        shutdown();
    }

    // == EventInspectorHost iface

    public void shutdown()
    {
        // we need clean shutdown, wait all background event inspectors to finish to have consistent state
        hostThreadPool.shutdown();
    }

    public boolean isCalmPeriod()
    {
        // "calm period" is when we have no queued nor active threads
        return hostThreadPool.getQueue().isEmpty() && hostThreadPool.getActiveCount() == 0;
    }

    // == EventListener iface

    public void onEvent( final Event<?> evt )
    {
        try
        {
            processEvent( evt, getEventInspectors() );
        }
        catch ( IllegalStateException e )
        {
            // NEXUS-4775 guice exception trying to resolve circular dependencies too early
            getLogger().trace( "Event inspectors are not fully initialized, skipping handling of {}", evt, e );
        }
    }

    // ==

    protected Set<EventInspector> getEventInspectors()
    {
        return new HashSet<EventInspector>( eventInspectors.values() );
    }

    protected void processEvent( final Event<?> evt, final Set<EventInspector> inspectors )
    {
        // 1st pass: sync ones (without handler)
        for ( EventInspector ei : inspectors )
        {
            if ( !( ei instanceof AsynchronousEventInspector ) )
            {
                try
                {
                    if ( ei.accepts( evt ) )
                    {
                        ei.inspect( evt );
                    }
                }
                catch ( Exception e )
                {
                    getLogger().warn( "EventInspector implementation={} had problem accepting an event={}",
                        new Object[] { ei.getClass().getName(), evt.getClass(), e } );
                }
            }
        }

        // 2nd pass: async ones
        for ( EventInspector ei : inspectors )
        {
            if ( ei instanceof AsynchronousEventInspector )
            {
                try
                {
                    if ( ei.accepts( evt ) )
                    {
                        final EventInspectorHandler handler = new EventInspectorHandler( getLogger(), ei, evt );

                        hostThreadPool.execute( handler );
                    }
                }
                catch ( Exception e )
                {
                    getLogger().warn( "Async EventInspector implementation={} had problem accepting an event={}",
                        new Object[] { ei.getClass().getName(), evt.getClass(), e } );
                }
            }
        }
    }

    // ==

    public static class EventInspectorHandler
        implements Runnable
    {
        private final Logger logger;

        private final EventInspector ei;

        private final Event<?> evt;

        public EventInspectorHandler( final Logger logger, final EventInspector ei, final Event<?> evt )
        {
            this.logger = logger;
            this.ei = ei;
            this.evt = evt;
        }

        public void run()
        {
            try
            {
                ei.inspect( evt );
            }
            catch ( Exception e )
            {
                logger.warn( "EventInspector implementation={} had problem accepting an event={}", new Object[] {
                    ei.getClass().getName(), evt.getClass(), e } );
            }
        }
    }
}
