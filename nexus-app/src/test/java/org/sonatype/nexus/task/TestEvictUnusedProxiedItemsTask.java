package org.sonatype.nexus.task;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.item.StorageItem;

public class TestEvictUnusedProxiedItemsTask
    extends AbstractMavenRepoContentTests
{

    public void testDeleteEmptyFolder()
        throws Exception
    {
        fillInRepo();

        long tsDeleting = System.currentTimeMillis() - 10000L;
        long tsToBeKept = tsDeleting + 1000L;
        long tsToBeDeleted = tsDeleting - 1000L;

        String[] itemsToBeKept = { "/org/sonatype/test-evict/1.0/test.txt" };
        String[] itemsToBeDeleted = {
            "/org/sonatype/test-evict/sonatype-test-evict_1.4_mail",
            "/org/sonatype/test-evict/sonatype-test-evict_1.4_mail/1.0-SNAPSHOT/sonatype-test-evict_1.4_mail-1.0-SNAPSHOT.jar" };

        for ( String item : itemsToBeKept )
        {
            StorageItem storageItem = apacheSnapshots.retrieveItem( apacheSnapshots.createUid( item ), null );

            storageItem.setLastRequested( tsToBeKept );

            apacheSnapshots.storeItem( storageItem );
        }

        for ( String item : itemsToBeDeleted )
        {
            StorageItem storageItem = apacheSnapshots.retrieveItem( apacheSnapshots.createUid( item ), null );

            storageItem.setLastRequested( tsToBeDeleted );

            apacheSnapshots.storeItem( storageItem );
        }

        defaultNexus.evictRepositoryUnusedProxiedItems( tsDeleting, apacheSnapshots.getId() );

        for ( String item : itemsToBeKept )
        {
            try
            {
                assertNotNull( apacheSnapshots.retrieveItem( apacheSnapshots.createUid( item ), null ) );
            }
            catch ( ItemNotFoundException e )
            {
                fail( "Item should not have been deleted: " + item );
            }
        }

        for ( String item : itemsToBeDeleted )
        {
            try
            {
                apacheSnapshots.retrieveItem( apacheSnapshots.createUid( item ), null );

                fail( "Item should have been deleted: " + item );
            }
            catch ( ItemNotFoundException e )
            {
                // this is correct
            }
        }
    }
}
