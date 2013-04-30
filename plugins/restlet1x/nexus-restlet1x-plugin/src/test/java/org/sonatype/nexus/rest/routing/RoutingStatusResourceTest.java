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
package org.sonatype.nexus.rest.routing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.SystemState;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.internal.ManagerImpl;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.model.RoutingStatusMessageWrapper;
import org.sonatype.plexus.rest.resource.PlexusResource;

public class RoutingStatusResourceTest
    extends NexusAppTestSupport
{
    private final String REPO_ID = "releases";

    private Subject subject;

    @Before
    public void login()
        throws Exception
    {
        lookup( ApplicationStatusSource.class ).setState( SystemState.STARTED );
        ThreadContext.bind( new Subject.Builder().buildSubject() );
    }

    @After
    public void logout()
        throws Exception
    {
        ThreadContext.remove();
    }

    @Override
    protected boolean enableAutomaticRoutingFeature()
    {
        return true;
    }

    protected void waitForRoutingBackgroundUpdates()
        throws Exception
    {
        // TODO: A hack, I don't want to expose this over component contract iface
        final ManagerImpl wm = (ManagerImpl) lookup( Manager.class );
        while ( wm.isUpdatePrefixFileJobRunning() )
        {
            Thread.sleep( 500 );
        }
    }

    /**
     * Testing does {@link RoutingStatusResource} honors {@link Repository#isExposed()} flag, since it has to be handled at
     * REST level as it exactly prevents access to repository over HTTP layer. Internally, exposed repositories are
     * still accessible in programmatic way.
     * 
     * @throws Exception
     */
    @Test
    public void statusUrlHonorsRepoState()
        throws Exception
    {
        final RoutingStatusResource wlStatusResource = (RoutingStatusResource) lookup( PlexusResource.class, "RoutingStatusResource" );
        waitForRoutingBackgroundUpdates();

        final Request request = new Request();
        request.setRootRef( new Reference( "http://localhost:8081/nexus" ) );
        request.getAttributes().put( RoutingResourceSupport.REPOSITORY_ID_KEY, REPO_ID );
        Response.setCurrent( new Response( request ) );
        try
        {
            final Repository repository = lookup( RepositoryRegistry.class ).getRepository( REPO_ID );

            {
                final RoutingStatusMessageWrapper payload = wlStatusResource.get( null, request, null, null );
                assertThat( payload, is( notNullValue() ) );
                assertThat( payload.getData().getPublishedUrl(), is( notNullValue() ) );
                assertThat( payload.getData().getPublishedUrl(), containsString( ".meta/prefixes.txt" ) );
                assertThat( payload.getData().getPublishedUrl(), containsString( REPO_ID ) );
            }

            repository.setExposed( false );
            repository.commitChanges();

            {
                final RoutingStatusMessageWrapper payload = wlStatusResource.get( null, request, null, null );
                assertThat( payload, is( notNullValue() ) );
                assertThat( payload.getData().getPublishedUrl(), is( nullValue() ) );
            }

            repository.setExposed( true );
            repository.commitChanges();

            {
                final RoutingStatusMessageWrapper payload = wlStatusResource.get( null, request, null, null );
                assertThat( payload, is( notNullValue() ) );
                assertThat( payload.getData().getPublishedUrl(), is( notNullValue() ) );
                assertThat( payload.getData().getPublishedUrl(), containsString( ".meta/prefixes.txt" ) );
                assertThat( payload.getData().getPublishedUrl(), containsString( REPO_ID ) );
            }
        }
        finally
        {
            Response.setCurrent( null );
        }
    }
}
