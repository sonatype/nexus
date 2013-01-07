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
package org.sonatype.nexus.plugins.siesta;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;
import org.sonatype.nexus.web.NexusGuiceFilter;
import org.sonatype.security.web.guice.SecurityWebFilter;
import org.sonatype.sisu.siesta.jackson.SiestaJacksonModule;
import org.sonatype.sisu.siesta.server.internal.ComponentDiscoveryApplication;
import org.sonatype.sisu.siesta.server.internal.SiestaServlet;
import org.sonatype.sisu.siesta.server.internal.jersey.SiestaJerseyModule;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Siesta plugin module.
 *
 * @since 2.3
 */
@Named
public class SiestaModule
    extends AbstractModule
{
    @Override
    protected void configure() {
        /*
         * We need to import some components from nexus-restlet1x-plugin for SecurityWebFilter, but its use is
         * hidden behind guice-servlet muck. We therefore bind it explicitly here so it will get seen by Sisu.
         *
         * Note: it would have been preferable to use "requireBinding(SecurityWebFilter.class)" to import the
         * SecurityWebFilter instance from nexus-restlet1x-plugin, but guice-servlet only wants to see filters
         * bound directly as singletons in this Injector (odd limitation). An alternative would have been to
         * requireBinding's for SecuritySystem and FilterChainResolver, which are the filter's dependencies.
         */
        bind( SecurityWebFilter.class );

        install(new org.sonatype.sisu.siesta.server.internal.SiestaModule());
        install(new SiestaJerseyModule());
        install(new SiestaJacksonModule());

        // Dynamically discover JAX-RS components
        bind(javax.ws.rs.core.Application.class).to(ComponentDiscoveryApplication.class).in(Singleton.class);

        install(new ServletModule()
        {
            @Override
            protected void configureServlets() {
                // FIXME: Resolve how we want to expose this, might want to add some structure here if we every want/plan/need-to support changing this again
                // FIXME: Maybe /service/<api>/ where <api> is siesta or local (for legacy)?  Since that part will never likely be used for what it was originally intended (a remoting/hostname mechanism IIUC).
                serve("/rest/*").with(SiestaServlet.class);

                filter("/rest/*").through( SecurityWebFilter.class );
            }
        });
    }
}
