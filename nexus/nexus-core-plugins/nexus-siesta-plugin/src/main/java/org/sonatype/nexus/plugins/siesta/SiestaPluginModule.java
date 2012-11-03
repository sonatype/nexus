/*
 * Copyright (c) 2007-2012 Sonatype, Inc.  All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/central-secure/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.nexus.plugins.siesta;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;
import org.sonatype.sisu.siesta.jackson.SiestaJacksonModule;
import org.sonatype.sisu.siesta.server.internal.ComponentDiscoveryApplication;
import org.sonatype.sisu.siesta.server.internal.SiestaModule;
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
public class SiestaPluginModule
    extends AbstractModule
{
    @Override
    protected void configure() {
        install(new SiestaModule());
        install(new SiestaJerseyModule());
        install(new SiestaJacksonModule());

        // NOTE: not including XStream support... it can suck a dick, use JAXB2

        // Dynamically discover JAX-RS components
        bind(javax.ws.rs.core.Application.class).to(ComponentDiscoveryApplication.class).in(Singleton.class);

        install(new ServletModule()
        {
            @Override
            protected void configureServlets() {
                // FIXME: Resolve how we want to expose this, might want to add some structure here if we every want/plan/need-to support changing this again
                serve("/rest/*").with(SiestaServlet.class);
            }
        });
    }
}
