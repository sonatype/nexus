/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.events;

import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.events.AbstractEvent;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.tasks.ClearCacheTask;

/**
 * @author Juven Xu
 */
@Component( role = EventInspector.class, hint = "ConfigurationChangeEvent" )
public class ConfigurationChangeEventInspector
    extends AbstractFeedRecorderEventInspector
{
    @Requirement
    private IndexerManager indexerManager;

    @Requirement
    private NexusScheduler nexusScheduler;

    protected IndexerManager getIndexerManager()
    {
        return indexerManager;
    }

    public boolean accepts( AbstractEvent evt )
    {
        if ( evt instanceof ConfigurationChangeEvent )
        {
            return true;
        }
        return false;
    }

    public void inspect( AbstractEvent evt )
    {
        inspectForNexus( evt );

        inspectForIndexerManager( evt );

        inspectForRepository( evt );
    }

    private void inspectForNexus( AbstractEvent evt )
    {
        // TODO: This causes cycle!
        // getNexus().getSystemStatus().setLastConfigChange( new Date() );

        getFeedRecorder().addSystemEvent( FeedRecorder.SYSTEM_CONFIG_ACTION, "Nexus configuration changed/updated." );

    }

    private void inspectForIndexerManager( AbstractEvent evt )
    {
        getIndexerManager().resetConfiguration();
    }

    private void inspectForRepository( AbstractEvent evt )
    {
        ConfigurationChangeEvent event = (ConfigurationChangeEvent) evt;

        List<Object> changes = event.getChanges();

        if ( changes != null && changes.size() == 2 )
        {
            CRepository oldConfig = (CRepository) changes.get( 0 );

            CRepository newConfig = (CRepository) changes.get( 1 );

            if ( repositoryStorageLocationChanged( oldConfig, newConfig ) )
            {
                ClearCacheTask expireCacheTask = nexusScheduler.createTaskInstance( ClearCacheTask.class );

                expireCacheTask.setRepositoryId( newConfig.getId() );

                expireCacheTask.setResourceStorePath( "/" );

                nexusScheduler.submit( "Expire repository cache", expireCacheTask );
            }
        }
    }

    private boolean repositoryStorageLocationChanged( CRepository oldConfig, CRepository newConfig )
    {
        CLocalStorage oldLocal = oldConfig.getLocalStorage();
        CLocalStorage newLocal = newConfig.getLocalStorage();

        CRemoteStorage oldRemote = oldConfig.getRemoteStorage();
        CRemoteStorage newRemote = newConfig.getRemoteStorage();

        if ( oldLocal != null && newLocal != null && !oldLocal.getUrl().equals( newLocal.getUrl() ) )
        {
            return true;
        }
        if ( oldRemote != null && newRemote != null && !oldRemote.getUrl().equals( newRemote.getUrl() ) )
        {
            return true;
        }

        return false;
    }

}
