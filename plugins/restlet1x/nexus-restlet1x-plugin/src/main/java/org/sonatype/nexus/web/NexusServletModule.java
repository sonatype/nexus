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
package org.sonatype.nexus.web;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.servlet.ServletModule;
import org.sonatype.guice.bean.locators.DefaultRankingFunction;
import org.sonatype.guice.bean.locators.RankingFunction;
import org.sonatype.security.web.guice.SecurityWebFilter;

/**
 * Guice module for binding nexus servlets.
 * 
 * @author adreghiciu
 */
@Named
@Singleton
public class NexusServletModule
    extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        requestStaticInjection( NexusGuiceFilter.class );

        serve( "/*" ).with( NexusRestletServlet.class, nexusRestletServletInitParams() );

        filter("/service/local/*").through( SecurityWebFilter.class );
        filter("/content/*").through( SecurityWebFilter.class );
        filter("/*").through( MdcUserContextFilter.class );

        /*
         * Give components contributed by this plugin a low-level ranking (same level as Nexus core) so they are ordered
         * after components from other plugins. This makes sure the restlet1x "catch-all" filter won't kick in early and
         * block other filters in the pipeline.
         */
        bind( RankingFunction.class ).toInstance( new DefaultRankingFunction( 0 ) );
    }

    private Map<String, String> nexusRestletServletInitParams()
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put( "nexus.role", "org.restlet.Application" );
        params.put( "nexus.roleHint", "nexus" );
        params.put( "nexus.org.restlet.clients", "FILE CLAP" );
        params.put( "plexus.discoverResources", "true" );
        return params;
    }
}
