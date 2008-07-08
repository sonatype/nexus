package org.sonatype.nexus.proxy.item;

import java.util.ArrayList;
import java.util.Iterator;

import org.sonatype.nexus.proxy.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.repository.DummyRepository;

public class RepositoryItemUidTest
    extends AbstractNexusTestCase
{
    protected DummyRepository repository = new DummyRepository( "dummy" );
    
    public void testLocks()
        throws Exception
    {
        RepositoryItemUid uidA = new RepositoryItemUid( repository, "/a.txt" );
        RepositoryItemUid uidB = new RepositoryItemUid( repository, "/b.txt" );
        RepositoryItemUid uidC = new RepositoryItemUid( repository, "/c.txt" );
        RepositoryItemUid uidD = new RepositoryItemUid( repository, "/d.txt" );
        RepositoryItemUid uidE = new RepositoryItemUid( repository, "/e.txt" );

        try
        {
            uidA.lock();
            assert RepositoryItemUid.getLockCount() == 1;

            uidB.lock();
            assert RepositoryItemUid.getLockCount() == 2;

            uidC.lock();
            assert RepositoryItemUid.getLockCount() == 3;

            uidD.lock();
            assert RepositoryItemUid.getLockCount() == 4;

            uidE.lock();
            assert RepositoryItemUid.getLockCount() == 5;

            uidA.unlock();
            assert RepositoryItemUid.getLockCount() == 4;

            uidB.unlock();
            assert RepositoryItemUid.getLockCount() == 3;

            uidC.unlock();
            assert RepositoryItemUid.getLockCount() == 2;

            uidD.unlock();
            assert RepositoryItemUid.getLockCount() == 1;

            uidE.unlock();
            assert RepositoryItemUid.getLockCount() == 0;
        }
        finally
        {
            // just in case
            uidA.unlock();
            uidB.unlock();
            uidC.unlock();
            uidD.unlock();
            uidE.unlock();
        }
    }

    public void testLocksOfSameUid()
        throws Exception
    {
        RepositoryItemUid uidA = new RepositoryItemUid( repository, "/a.txt" );
        
        ArrayList<Thread> threads = new ArrayList<Thread>();
        
        for ( int i = 15 ; i < 100 ; i++ )
        {
            threads.add( new Thread( new RepositoryItemUidLockProcess( uidA, i ) ) );
        }
        uidA.lock();
        assert RepositoryItemUid.getLockCount() == 1;
        
        for ( Iterator<Thread> iter = threads.iterator() ; iter.hasNext() ; )
        {
            iter.next().start();
        }
        
        Thread.sleep( 10 );
        
        assert RepositoryItemUid.getLockCount() == 1;
        
        uidA.unlock();
        
        for ( Iterator<Thread> iter = threads.iterator() ; iter.hasNext() ; )
        {
            iter.next().join();
        }
        
        assert RepositoryItemUid.getLockCount() == 0;
    }

    private static final class RepositoryItemUidLockProcess
        implements
            Runnable
    {
        private RepositoryItemUid uid;

        private long timeout;

        public RepositoryItemUidLockProcess( RepositoryItemUid uid, long timeout )
        {
            this.uid = uid;
            this.timeout = timeout;
        }

        public void run()
        {
            try
            {
                this.uid.lock();
                Thread.sleep( timeout );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            finally
            {
                this.uid.unlock();
            }
        }
    }
}
