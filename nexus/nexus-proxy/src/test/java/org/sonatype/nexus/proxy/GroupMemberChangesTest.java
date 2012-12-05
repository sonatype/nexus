/*
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
package org.sonatype.nexus.proxy;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.httpclient.CustomMultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.events.RepositoryItemBatchEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemBatchEventAdded;
import org.sonatype.nexus.proxy.events.RepositoryItemBatchEventAddedToGroup;
import org.sonatype.nexus.proxy.events.RepositoryItemBatchEventRemoved;
import org.sonatype.nexus.proxy.events.RepositoryItemBatchEventRemovedFromGroup;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.TransientRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.commonshttpclient.CommonsHttpClientRemoteStorage;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import sun.reflect.generics.tree.ArrayTypeSignature;

/**
 * Group member changes tests.
 */
public class GroupMemberChangesTest
    extends AbstractProxyTestEnvironment
{

    protected final String ITEM1_PATH = "/org/foo/1.0/foo-1.0.pom";

    protected final String ITEM2_PATH = "/org/foo/1.0/foo-1.0.pom.sha1";

    protected final String ITEM3_PATH = "/org/foo/1.0/foo-1.0.jar";

    protected final String ITEM4_PATH = "/org/foo/1.0/foo-1.0.jar.sha1";

    protected final String[] ITEMS = { ITEM1_PATH, ITEM2_PATH, ITEM3_PATH, ITEM4_PATH };

    @Override
    protected EnvironmentBuilder getEnvironmentBuilder()
        throws Exception
    {
        return new M2TestsuiteEnvironmentBuilder( lookup( ServletServer.class ) );
    }

    @Test
    public void testBatchEvents()
        throws Exception
    {
        final EventCollector collector = new EventCollector();
        final EventBus eventBus = lookup( EventBus.class );
        eventBus.register( collector );
        assertNothingFiredYet( collector );

        final Repository repository = getRepositoryRegistry().getRepository( "inhouse" );
        for ( String path : ITEMS )
        {
            repository.storeItem( new ResourceStoreRequest( path ), new ByteArrayInputStream( path.getBytes() ), null );
        }
        assertNothingFiredYet( collector );

        final GroupRepository group = getRepositoryRegistry().getRepositoryWithFacet( "test", GroupRepository.class );

        // remove it
        group.removeMemberRepositoryId( repository.getId() );
        getApplicationConfiguration().saveConfiguration();
        assertNothingFiredYet( collector );

        // make this repo a transient one
        repository.getRepositoryKind().addFacet( TransientRepository.class );

        // add it, but now is transient!
        group.addMemberRepositoryId( repository.getId() );
        getApplicationConfiguration().saveConfiguration();
        assertIsFired( collector, true, 1 );

        // remove it, still transient
        group.removeMemberRepositoryId( repository.getId() );
        getApplicationConfiguration().saveConfiguration();
        assertIsFired( collector, false, 2 );
    }

    protected void assertNothingFiredYet( final EventCollector collector )
    {
        Assert.assertThat( collector.getCollectedEvents().size(), equalTo( 0 ) );
    }

    protected void assertIsFired( final EventCollector collector, final boolean added, final int count )
    {
        Assert.assertThat( collector.getCollectedEvents().size(), equalTo( count ) );
        Assert.assertThat( collector.getCollectedEvents().get( count - 1 ),
                           instanceOf( RepositoryItemBatchEvent.class ) );

        if ( added )
        {
            Assert.assertThat( collector.getCollectedEvents().get( count - 1 ),
                               instanceOf( RepositoryItemBatchEventAddedToGroup.class ) );
        }
        else
        {
            Assert.assertThat( collector.getCollectedEvents().get( count - 1 ),
                               instanceOf( RepositoryItemBatchEventRemovedFromGroup.class ) );
        }

        // items
        final RepositoryItemBatchEvent evt = collector.getCollectedEvents().get( count - 1 );
        Assert.assertThat( evt.getRepository().getId(), equalTo( "test" ) );
        Assert.assertThat( evt.getItemPaths().size(), equalTo( ITEMS.length ) );
        Assert.assertThat( evt.getItemPaths(), containsInAnyOrder( ITEMS ) );
    }

    public static class EventCollector
    {

        private final ArrayList<RepositoryItemBatchEvent> collectedEvents = new ArrayList<RepositoryItemBatchEvent>();

        @Subscribe
        public void collect( final RepositoryItemBatchEvent evt )
        {
            this.collectedEvents.add( evt );
        }

        public ArrayList<RepositoryItemBatchEvent> getCollectedEvents()
        {
            return new ArrayList<RepositoryItemBatchEvent>( collectedEvents );
        }
    }
}
