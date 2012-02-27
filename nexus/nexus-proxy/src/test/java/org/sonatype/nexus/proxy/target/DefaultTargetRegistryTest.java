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
package org.sonatype.nexus.proxy.target;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.junit.Test;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.maven.maven1.Maven1ContentClass;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.Repository;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class DefaultTargetRegistryTest
    extends AbstractNexusTestCase
{
    protected ApplicationConfiguration applicationConfiguration;

    protected TargetRegistry targetRegistry;

    protected ContentClass maven1;

    protected ContentClass maven2;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        applicationConfiguration = lookup( ApplicationConfiguration.class );

        maven1 = new Maven1ContentClass();

        maven2 = new Maven2ContentClass();

        targetRegistry = lookup( TargetRegistry.class );

        // adding two targets
        Target t1 = new Target( "maven2-public", "Maven2 (public)", maven2, Arrays
            .asList( new String[] { "/org/apache/maven/((?!sources\\.).)*" } ) );

        targetRegistry.addRepositoryTarget( t1 );

        Target t2 = new Target( "maven2-with-sources", "Maven2 sources", maven2, Arrays
            .asList( new String[] { "/org/apache/maven/.*" } ) );

        targetRegistry.addRepositoryTarget( t2 );

        Target t3 = new Target( "maven1", "Maven1", maven1, Arrays.asList( new String[] { "/org\\.apache\\.maven.*" } ) );

        targetRegistry.addRepositoryTarget( t3 );

        applicationConfiguration.saveConfiguration();
    }

    @Test
    public void testSimpleM2()
    {
        // create a dummy
        Repository repository = createMock( Repository.class );
        expect( repository.getRepositoryContentClass() ).andReturn( maven2 ).anyTimes();
        expect( repository.getId() ).andReturn( "dummy" ).anyTimes();

        replay( repository );

        TargetSet ts = targetRegistry.getTargetsForRepositoryPath(
            repository,
            "/org/apache/maven/maven-core/2.0.9/maven-core-2.0.9.pom" );

        assertNotNull( ts );

        assertEquals( 2, ts.getMatches().size() );

        assertEquals( 1, ts.getMatchedRepositoryIds().size() );

        assertEquals( "dummy", ts.getMatchedRepositoryIds().iterator().next() );

        TargetSet ts1 = targetRegistry.getTargetsForRepositoryPath(
            repository,
            "/org/apache/maven/maven-core/2.0.9/maven-core-2.0.9-sources.jar" );

        assertNotNull( ts1 );

        assertEquals( 1, ts1.getMatches().size() );

        assertEquals( "maven2-with-sources", ts1.getMatches().iterator().next().getTarget().getId() );

        // adding them
        ts.addTargetSet( ts1 );

        assertEquals( 2, ts.getMatches().size() );

        assertEquals( 1, ts.getMatchedRepositoryIds().size() );
    }

    @Test
    public void testSimpleM1()
    {
        // create a dummy
        Repository repository = createMock( Repository.class );
        expect( repository.getRepositoryContentClass() ).andReturn( maven1 ).anyTimes();
        expect( repository.getId() ).andReturn( "dummy" ).anyTimes();

        replay( repository );

        TargetSet ts = targetRegistry.getTargetsForRepositoryPath(
            repository,
            "/org.apache.maven/jars/maven-model-v3-2.0.jar" );

        assertNotNull( ts );

        assertEquals( 1, ts.getMatches().size() );

        ts = targetRegistry.getTargetsForRepositoryPath(
            repository,
            "/org/apache/maven/maven-core/2.0.9/maven-core-2.0.9-sources.jar" );

        assertNotNull( ts );

        assertEquals( 0, ts.getMatches().size() );
    }

    private Function<Target, String> toIds = new Function<Target, String>()
    {
        @Override
        public String apply( Target input )
        {
            checkNotNull( input );
            return input.getId();
        }
    };

    private static final int OPERATIONS = 2000;

    private static final int THREADS = OPERATIONS / 10;

    public void single( final int ref )
        throws Exception
    {
        List<String> ids = Lists.newArrayList();
        for ( int j = 0; j < 5; j++ )
        {

            String id = Long.toHexString( System.nanoTime() + ref + j + ref * j );
            Target target = new Target( id, "name" + id, j % 3 == 0 ? maven1 : maven2, Arrays.asList( ".*/" + id ) );
            targetRegistry.addRepositoryTarget( target );
            ids.add( id );
        }

        applicationConfiguration.saveConfiguration();

        for ( String id : ids )
        {
            Target t = targetRegistry.getRepositoryTarget( id );
            assertThat( t, notNullValue() );
            assertThat( t.getId(), equalTo( id ) );
            assertThat( t.getName(), equalTo( "name" + id ) );
        }

        Collection<String> targets = transform( targetRegistry.getRepositoryTargets(), toIds );
        targets.containsAll( ids );

        for ( String id : ids )
        {
            targetRegistry.removeRepositoryTarget( id );
            // I really wanna try to break this
            applicationConfiguration.saveConfiguration();
        }

        targets = transform( targetRegistry.getRepositoryTargets(), toIds );
        for ( String id : ids )
        {
            Target t = targetRegistry.getRepositoryTarget( id );
            assertThat( "ref: " + ref + " ids: " + ids, t, nullValue() );
            assertThat( "ref: " + ref + " ids: " + ids, targets, not( containsInAnyOrder( id ) ) );
        }
    }

    @Test
    public void testConcurrency()
        throws Exception
    {
        List<FutureTask<String>> calls = Lists.newArrayList();
        for ( int i = 0; i < OPERATIONS; i++ )
        {
            final int j = i;
            Callable<String> c = new Callable<String>()
            {
                @Override
                public String call()
                    throws Exception
                {
                    single( j );

                    return "ok";
                }
            };
            calls.add( new FutureTask<String>( c ) );
        }

        Executor executor = Executors.newFixedThreadPool (THREADS);
        for ( FutureTask<String> futureTask : calls )
        {
            executor.execute( futureTask );
        }

        for ( FutureTask<String> futureTask : calls )
        {
            assertThat( futureTask.get(), equalTo( "ok" ) );
        }
    }

}
