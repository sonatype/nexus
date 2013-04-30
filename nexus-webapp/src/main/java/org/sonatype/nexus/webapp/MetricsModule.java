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
package org.sonatype.nexus.webapp;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;
import com.yammer.metrics.HealthChecks;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Clock;
import com.yammer.metrics.core.HealthCheckRegistry;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.VirtualMachineMetrics;
import com.yammer.metrics.reporting.HealthCheckServlet;
import com.yammer.metrics.reporting.MetricsServlet;
import com.yammer.metrics.reporting.PingServlet;
import com.yammer.metrics.reporting.ThreadDumpServlet;
import com.yammer.metrics.util.DeadlockHealthCheck;
import com.yammer.metrics.web.DefaultWebappMetricsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.guice.FilterChainModule;
import org.sonatype.security.web.guice.SecurityWebFilter;

/**
 * <a href="http://metrics.codahale.com">Yammer Metrics</a> guice configuration.
 *
 * Installs servlet endpoints:
 *
 * <ul>
 *     <li>/internal/ping</li>
 *     <li>/internal/threads</li>
 *     <li>/internal/metrics</li>
 *     <li>/internal/healthcheck</li>
 * </ul>
 *
 * Protected by {@code nexus:metrics-endpoints} permission.
 *
 * @since 2.5
 */
public class MetricsModule
    extends AbstractModule
{
    private static final Logger log = LoggerFactory.getLogger(MetricsModule.class);

    private static final String MOUNT_POINT = "/internal";

    @Override
    protected void configure() {
        // NOTE: AdminServletModule (metrics-guice intgegration) generates invalid links, so wire up servlets ourselves

        final Clock clock = Clock.defaultClock();
        bind(Clock.class).toInstance(clock);

        final VirtualMachineMetrics virtualMachineMetrics = VirtualMachineMetrics.getInstance();
        bind(VirtualMachineMetrics.class).toInstance(virtualMachineMetrics);

        final HealthCheckRegistry healthCheckRegistry = HealthChecks.defaultRegistry();
        bind(HealthCheckRegistry.class).toInstance(healthCheckRegistry);

        // TODO: Consider making a component which can mediate (via sisu) adding/removing healthcheck components to/from the registry
        // TODO: For now new instances must be explicitly registered, like this one:
        healthCheckRegistry.register(new DeadlockHealthCheck(virtualMachineMetrics));

        final MetricsRegistry metricsRegistry = Metrics.defaultRegistry();
        bind(MetricsRegistry.class).toInstance(metricsRegistry);

        final JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
        bind(JsonFactory.class).toInstance(jsonFactory);

        // FIXME: Sort out if we need to do this.  Its noted in SiestaModule, may be needed here too :-(
        //bind(SecurityWebFilter.class);

        install(new ServletModule()
        {
            @Override
            protected void configureServlets() {
                serve(MOUNT_POINT + "/ping").with(new PingServlet());

                serve(MOUNT_POINT + "/threads").with(new ThreadDumpServlet(virtualMachineMetrics));

                serve(MOUNT_POINT + "/metrics").with(new MetricsServlet(
                    clock,
                    virtualMachineMetrics,
                    metricsRegistry,
                    jsonFactory,
                    true
                ));

                serve(MOUNT_POINT + "/healthcheck").with(new HealthCheckServlet(healthCheckRegistry));

                // record metrics for all webapp access
                filter("/*").through(new DefaultWebappMetricsFilter());

                // configure security
                filter(MOUNT_POINT + "/*").through(SecurityWebFilter.class);
            }
        });

        // require permission to use endpoints
        install(new FilterChainModule()
        {
            @Override
            protected void configure() {
                addFilterChain(MOUNT_POINT + "/**", "noSessionCreation,authcBasic,perms[nexus:metrics-endpoints]");
            }
        });

        log.info("Metrics support configured");
    }
}
