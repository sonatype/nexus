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
package org.sonatype.nexus.proxy.item;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractNexusTestEnvironment;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.repository.Repository;

public class RepositoryItemUidFactoryTest
    extends AbstractNexusTestEnvironment
{
    protected RepositoryItemUidFactory factory;

    protected Repository repository;

    public void setUp()
        throws Exception
    {
        super.setUp();
        repository = lookup( Repository.class, M2Repository.ID );
        final CRepository config = new DefaultCRepository();
        config.setId( "repo1" );
        repository.configure( config );
        factory = lookup( RepositoryItemUidFactory.class );
    }

    @Test
    public void testSameLockResourceInstance()
        throws Exception
    {
        RepositoryItemUid uid1 = factory.createUid( repository, "/some/blammo/poth" );

        RepositoryItemUid uid2 = factory.createUid( repository, "/some/blammo/poth" );

        DefaultRepositoryItemUidLock uidLock1 = (DefaultRepositoryItemUidLock) uid1.getLock();

        DefaultRepositoryItemUidLock uidLock2 = (DefaultRepositoryItemUidLock) uid2.getLock();

        uidLock1.lock( Action.read );

        // They share SAME lock
        Assert.assertSame( "UIDLock instances should be different", uidLock1, uidLock2 );
        Assert.assertSame( "UIDLock lock instances should be same", uidLock1.getContentLock(),
            uidLock2.getContentLock() );
        Assert.assertEquals( "Since invoked from same UT thread, both should say we have lock held",
            uidLock1.getContentLock().hasLocksHeld(), uidLock2.getContentLock().hasLocksHeld() );
    }

    @Test
    public void testPathPeculiarites()
        throws Exception
    {
        RepositoryItemUid uid1 = factory.createUid( repository, "/some/blammo/poth" );

        RepositoryItemUid uid2 = factory.createUid( repository, "/some/blammo/poth.storeItem()" );

        DefaultRepositoryItemUidLock uidLock1 = (DefaultRepositoryItemUidLock) uid1.getLock();

        DefaultRepositoryItemUidLock uidLock2 = (DefaultRepositoryItemUidLock) uid2.getLock();

        uidLock1.lock( Action.read );

        // They dont share SAME lock
        Assert.assertNotSame( "UIDLock instances should be different", uidLock1, uidLock2 );
        Assert.assertNotSame( "UIDLock lock instances should be different", uidLock1.getContentLock(),
            uidLock2.getContentLock() );
    }

    @Test
    public void testPathPeculiaritesInM2Repo()
        throws Exception
    {
        // In Mave repositories, the artifact and their sha1/md5 counterpart locks are
        // "squashed" all onto the artifact keyed lock (subordinate locking)
        // see org.sonatype.nexus.proxy.maven.AbstractMavenRepository.fixPathForLockKey(String)
        final RepositoryItemUid uid1 = repository.createUid( "/some/blammo/poth.jar" );
        final RepositoryItemUid uid2 = repository.createUid( "/some/blammo/poth.jar.sha1" );
        final RepositoryItemUid uid3 = repository.createUid( "/some/blammo/poth.jar.md5" );
        final RepositoryItemUid uid4 = repository.createUid( "/some/blammo/poth.pom" );
        final RepositoryItemUid uid5 = repository.createUid( "/some/blammo/poth.pom.sha1" );
        final RepositoryItemUid uid6 = repository.createUid( "/some/blammo/poth.pom.md5" );

        final DefaultRepositoryItemUidLock uidLock1 = (DefaultRepositoryItemUidLock) uid1.getLock();
        final DefaultRepositoryItemUidLock uidLock2 = (DefaultRepositoryItemUidLock) uid2.getLock();
        final DefaultRepositoryItemUidLock uidLock3 = (DefaultRepositoryItemUidLock) uid3.getLock();
        final DefaultRepositoryItemUidLock uidLock4 = (DefaultRepositoryItemUidLock) uid4.getLock();
        final DefaultRepositoryItemUidLock uidLock5 = (DefaultRepositoryItemUidLock) uid5.getLock();
        final DefaultRepositoryItemUidLock uidLock6 = (DefaultRepositoryItemUidLock) uid6.getLock();

        // They share SAME lock, but wrapping instances of DefaultRepositoryItemUidLock are not same!
        Assert.assertNotSame( "UIDLock instances should be same", uidLock1.hashCode(), uidLock2.hashCode() );
        Assert.assertTrue( "UIDLock instances should be same", uidLock1.equals( uidLock2 ) );
        Assert.assertSame( "UIDLock lock instances should be same", uidLock1.getContentLock(),
            uidLock2.getContentLock() );
        // They share SAME lock, but wrapping instances of DefaultRepositoryItemUidLock are not same!
        Assert.assertNotSame( "UIDLock instances should be same", uidLock1.hashCode(), uidLock3.hashCode() );
        Assert.assertTrue( "UIDLock instances should be same", uidLock1.equals( uidLock3 ) );
        Assert.assertSame( "UIDLock lock instances should be same", uidLock1.getContentLock(),
            uidLock3.getContentLock() );
        // They share SAME lock, but wrapping instances of DefaultRepositoryItemUidLock are not same!
        Assert.assertNotSame( "UIDLock instances should be same", uidLock4.hashCode(), uidLock5.hashCode() );
        Assert.assertTrue( "UIDLock instances should be same", uidLock4.equals( uidLock5 ) );
        Assert.assertSame( "UIDLock lock instances should be same", uidLock4.getContentLock(),
            uidLock5.getContentLock() );
        // They share SAME lock, but wrapping instances of DefaultRepositoryItemUidLock are not same!
        Assert.assertNotSame( "UIDLock instances should be same", uidLock4.hashCode(), uidLock6.hashCode() );
        Assert.assertTrue( "UIDLock instances should be same", uidLock4.equals( uidLock6 ) );
        Assert.assertSame( "UIDLock lock instances should be same", uidLock4.getContentLock(),
            uidLock6.getContentLock() );

        // but, uid1 (artifact) and uid4 (pom) are DIFFERENT
        Assert.assertNotSame( "UIDLock instances should be different", uidLock1.hashCode(), uidLock4.hashCode() );
        Assert.assertTrue( "UIDLock instances should be different", !uidLock1.equals( uidLock4 ) );
        Assert.assertNotSame( "UIDLock lock instances should be different", uidLock1.getContentLock(),
            uidLock4.getContentLock() );
    }

    @Test
    public void testRelease()
        throws Exception
    {
        // Explanation: before, there was a "release" on locks, when not used anymore, but it prevent patterns exactly
        // as in here:
        // obtain an instance of lock, and once released, it's _unusable_ (was throwing IllegalStatusEx).
        // Later, a cosmetic improvement was suggested by Alin, to make last unlock() invocation actually release too,
        // but
        // it did not change the fact, released lock instance is unusable.
        // Finally, code was modified to not require release.
        RepositoryItemUid uid = factory.createUid( repository, "/some/blammo/poth" );

        DefaultRepositoryItemUidLock uidLock1 = (DefaultRepositoryItemUidLock) uid.getLock();

        uidLock1.lock( Action.read );

        Assert.assertTrue( "Since locked it should say we have lock held", uidLock1.getContentLock().hasLocksHeld() );

        uidLock1.unlock();

        Assert.assertFalse( "Since unlocked it should say we have no lock held",
            uidLock1.getContentLock().hasLocksHeld() );

        // now again

        uidLock1.lock( Action.read );

        Assert.assertTrue( "Since locked it should say we have lock held", uidLock1.getContentLock().hasLocksHeld() );

        uidLock1.unlock();

        Assert.assertFalse( "Since unlocked it should say we have no lock held",
            uidLock1.getContentLock().hasLocksHeld() );

        // unlock above released it too
        // uidLock1.release();

        // No more release, hence no more IllegalStateEx either.
        // try
        // {
        // uidLock1.unlock();
        //
        // Assert.fail( "Reusing a released instance of UIDLock is a no-go" );
        // }
        // catch ( IllegalStateException e )
        // {
        // // good
        // }
    }

    @Test
    public void testWeakHashMapIsWorkingAsExpected()
    {
        // we create many _different_ keyed uids and corresponding locks
        int size = 0;

        size = ( (DefaultRepositoryItemUidFactory) factory ).locksInMap();

        Assert.assertTrue( "We should have nothing in weak map: " + size, size == 0 );

        for ( int i = 0; i < 10000; i++ )
        {
            factory.createUid( repository, "/some/blammo/poth/" + String.valueOf( i ) ).getLock();
        }

        size = ( (DefaultRepositoryItemUidFactory) factory ).locksInMap();

        Assert.assertTrue( "We should have less than 10k in weak map: " + size, size <= 10000 && size > 0 );

        System.gc();

        for ( int i = 10000; i < 20000; i++ )
        {
            factory.createUid( repository, "/some/blammo/poth/" + String.valueOf( i ) ).getLock();
        }

        size = ( (DefaultRepositoryItemUidFactory) factory ).locksInMap();

        Assert.assertTrue( "We should have less than 10k in weak map: " + size, size <= 20000 && size > 0 );

        System.gc();

        for ( int i = 20000; i < 30000; i++ )
        {
            factory.createUid( repository, "/some/blammo/poth/" + String.valueOf( i ) ).getLock();
        }

        size = ( (DefaultRepositoryItemUidFactory) factory ).locksInMap();

        Assert.assertTrue( "We should have less than 10k in weak map: " + size, size <= 30000 && size > 0 );
    }
}
