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
package org.sonatype.nexus.bootstrap.jetty;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Custom re-implementation of {@link com.yammer.metrics.jetty.InstrumentedSelectChannelConnector}
 * which sets up metrics collection on {@link #doStart()} instead of in CTOR.
 *
 * @since 2.5
 */
public final class InstrumentedSelectChannelConnector
    extends SelectChannelConnector
{
    private static final Logger log = LoggerFactory.getLogger(InstrumentedSelectChannelConnector.class);

    private final MetricsRegistry registry;

    private Timer duration;

    private Meter accepts, connects, disconnects;

    private Counter connections;

    public InstrumentedSelectChannelConnector() {
        registry = Metrics.defaultRegistry();
    }

    @Override
    protected void doStart() throws Exception {
        String port = String.valueOf(getPort());

        this.duration = registry.newTimer(SelectChannelConnector.class,
            "connection-duration",
            port,
            TimeUnit.MILLISECONDS,
            TimeUnit.SECONDS);

        this.accepts = registry.newMeter(SelectChannelConnector.class,
            "accepts",
            port,
            "connections",
            TimeUnit.SECONDS);

        this.connects = registry.newMeter(SelectChannelConnector.class,
            "connects",
            port,
            "connections",
            TimeUnit.SECONDS);

        this.disconnects = registry.newMeter(SelectChannelConnector.class,
            "disconnects",
            port,
            "connections",
            TimeUnit.SECONDS);

        this.connections = registry.newCounter(SelectChannelConnector.class,
            "active-connections",
            port);

        log.info("Metrics enabled");

        super.doStart();
    }

    // TODO: remove metrics on doStop()

    @Override
    public void accept(final int acceptorID) throws IOException {
        super.accept(acceptorID);
        accepts.mark();
    }

    @Override
    protected void connectionOpened(final Connection connection) {
        connections.inc();
        super.connectionOpened(connection);
        connects.mark();
    }

    @Override
    protected void connectionClosed(final Connection connection) {
        super.connectionClosed(connection);
        disconnects.mark();
        final long duration = System.currentTimeMillis() - connection.getTimeStamp();
        this.duration.update(duration, TimeUnit.MILLISECONDS);
        connections.dec();
    }
}
