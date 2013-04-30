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
package org.sonatype.nexus.proxy.walker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.M2TestsuiteEnvironmentBuilder;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;

public class WalkerTest
    extends AbstractProxyTestEnvironment
{
    private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

    private Walker walker;

    private RepositoryRegistry repositoryRegistry;

    public void setUp()
        throws Exception
    {
        super.setUp();

        walker = lookup( Walker.class );
        repositoryRegistry = this.lookup( RepositoryRegistry.class );
    }

    @Override
    protected EnvironmentBuilder getEnvironmentBuilder()
        throws Exception
    {
        ServletServer ss = (ServletServer) lookup( ServletServer.ROLE );
        this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder( ss );
        return jettyTestsuiteEnvironmentBuilder;
    }

    @Test
    public void testWalker()
        throws Exception
    {

        // fetch some content to have on walk on something
        getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/activemq/activemq-core/1.2/activemq-core-1.2.jar", false ) );
        getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/xstream/xstream/1.2.2/xstream-1.2.2.pom", false ) );
        getRootRouter().retrieveItem( new ResourceStoreRequest( "/groups/test/rome/rome/0.9/rome-0.9.pom", false ) );
        getRootRouter().retrieveItem( new ResourceStoreRequest( "/groups/test/repo3.txt", false ) );

        TestWalkerProcessor wp = null;
        WalkerContext wc = null;

        wp = new TestWalkerProcessor();

        // this is a group
        wc =
            new DefaultWalkerContext( getRepositoryRegistry().getRepository( "test" ), new ResourceStoreRequest(
                RepositoryItemUid.PATH_ROOT, true ) );

        wc.getProcessors().add( wp );

        walker.walk( wc );

        assertThat( "Should not be stopped!", wc.isStopped(), is( false ) );

        if ( wc.getStopCause() != null )
        {
            wc.getStopCause().printStackTrace();

            fail( "Should be no exception!" );
        }

        Assert.assertEquals( 10, wp.collEnters );
        Assert.assertEquals( 10, wp.collExits );
        Assert.assertEquals( 0, wp.colls );
        Assert.assertEquals( 4, wp.files );
        Assert.assertEquals( 0, wp.links );
    }

    /**
     * See NXCM-4516. We are invoking "walker" using a path that points to a non-collection item (a file).
     * 
     * @throws Exception
     */
    @Test
    public void testWalkerRunningAgainstFileItem()
        throws Exception
    {
        // fetch some content to have on walk on something
        getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/activemq/activemq-core/1.2/activemq-core-1.2.jar", false ) );
        TestWalkerProcessor wp = null;
        WalkerContext wc = null;
        wp = new TestWalkerProcessor();
        // this is a group
        wc =
            new DefaultWalkerContext( getRepositoryRegistry().getRepository( "test" ), new ResourceStoreRequest(
                "/activemq/activemq-core/1.2/activemq-core-1.2.jar", true ) );
        wc.getProcessors().add( wp );
        walker.walk( wc );
        assertThat( "Should not be stopped!", wc.isStopped(), is( false ) );
        if ( wc.getStopCause() != null )
        {
            wc.getStopCause().printStackTrace();
            fail( "Should be no exception!" );
        }

        Assert.assertEquals( 0, wp.collEnters );
        Assert.assertEquals( 0, wp.collExits );
        Assert.assertEquals( 0, wp.colls );
        Assert.assertEquals( 1, wp.files );
        Assert.assertEquals( 0, wp.links );
    }

    /**
     * Tests walking an out of service repo. The walker should NOT not fail, but also NOT find any items.</BR> Verifies
     * fix for: NEXUS-4554 (which is more general then just fixing the Trash task)
     * 
     * @throws Exception
     */
    @Test
    public void testWalkOutOfServiceRepo()
        throws Exception
    {
        // put repo2 out of service
        String repoId = "repo2";
        Repository repo = repositoryRegistry.getRepository( repoId );
        repo.setLocalStatus( LocalStatus.OUT_OF_SERVICE );
        repo.commitChanges();

        TestWalkerProcessor wp = null;
        WalkerContext wc = null;

        wp = new TestWalkerProcessor();

        // this is a group
        wc =
            new DefaultWalkerContext( getRepositoryRegistry().getRepository( repoId ), new ResourceStoreRequest(
                RepositoryItemUid.PATH_ROOT, true ) );

        wc.getProcessors().add( wp );

        walker.walk( wc );

        Assert.assertEquals( 0, wp.collEnters );
        Assert.assertEquals( 0, wp.collExits );
        Assert.assertEquals( 0, wp.colls );
        Assert.assertEquals( 0, wp.files );
        Assert.assertEquals( 0, wp.links );
    }

    /**
     * Tests whether the walker makes use of the item comparator set in the context.
     */
    @Test
    public void testSortedWalk()
        throws Exception
    {
        // fetch some content to have on walk on something
        getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar", false ) );
        getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.pom", false ) );

        Comparator<StorageItem> itemComparator = new Comparator<StorageItem>()
        {
            @Override
            public int compare( final StorageItem o1, final StorageItem o2 )
            {
                return o1.getName().compareTo( o2.getName() );
            }
        };

        Matcher<Iterable<? extends String>> matcher =
            contains( "slf4j-api-1.4.3.jar", "slf4j-api-1.4.3.jar.sha1", "slf4j-api-1.4.3.pom",
                "slf4j-api-1.4.3.pom.sha1" );

        assertSortedWalk( itemComparator, matcher );

        itemComparator = new Comparator<StorageItem>()
        {
            @Override
            public int compare( final StorageItem o1, final StorageItem o2 )
            {
                return o2.getName().compareTo( o1.getName() );
            }
        };

        matcher =
            contains( "slf4j-api-1.4.3.pom.sha1", "slf4j-api-1.4.3.pom", "slf4j-api-1.4.3.jar.sha1",
                "slf4j-api-1.4.3.jar" );

        assertSortedWalk( itemComparator, matcher );
    }

    private void assertSortedWalk( final Comparator<StorageItem> itemComparator,
                                   final Matcher<Iterable<? extends String>> matcher )
        throws NoSuchRepositoryException
    {

        DefaultWalkerContext wc =
            new DefaultWalkerContext( getRepositoryRegistry().getRepository( "test" ), new ResourceStoreRequest(
                "/org/slf4j/slf4j-api/1.4.3", true ) );

        final List<String> seen = Lists.newLinkedList();
        wc.getProcessors().add( new AbstractWalkerProcessor()
        {
            @Override
            public void processItem( final WalkerContext context, final StorageItem item )
                throws Exception
            {
                seen.add( item.getName() );
            }
        } );

        wc.setItemComparator( itemComparator );

        walker.walk( wc );

        assertThat( seen, matcher );
    }

    private class TestWalkerProcessor
        extends AbstractWalkerProcessor
    {
        public int collEnters;

        public int collExits;

        public int colls;

        public int files;

        public int links;

        public TestWalkerProcessor()
        {
            collEnters = 0;
            collExits = 0;
            colls = 0;
            files = 0;
            links = 0;
        }

        public void onCollectionEnter( WalkerContext context, StorageCollectionItem coll )
        {
            collEnters++;
        }

        @Override
        public void processItem( WalkerContext context, StorageItem item )
        {
            if ( StorageCollectionItem.class.isAssignableFrom( item.getClass() ) )
            {
                colls++;
            }
            else if ( StorageFileItem.class.isAssignableFrom( item.getClass() ) )
            {
                files++;
            }
            else if ( StorageLinkItem.class.isAssignableFrom( item.getClass() ) )
            {
                links++;
            }
        }

        public void onCollectionExit( WalkerContext context, StorageCollectionItem coll )
        {
            collExits++;
        }
    }

}
