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
package org.sonatype.nexus.maven.tasks;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.maven.tasks.descriptors.SnapshotRemovalTaskDescriptor;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;
import org.sonatype.scheduling.SchedulerTask;

/**
 * SnapshotRemoverTask
 * 
 * @author cstamas
 */
@Component( role = SchedulerTask.class, hint = SnapshotRemovalTaskDescriptor.ID, instantiationStrategy = "per-lookup" )
public class SnapshotRemoverTask
    extends AbstractNexusRepositoriesTask<SnapshotRemovalResult>
{
    public static final String SYSTEM_REMOVE_SNAPSHOTS_ACTION = "REMOVESNAPSHOTS";

    public static final int DEFAULT_MIN_SNAPSHOTS_TO_KEEP = 0;

    public static final int DEFAULT_OLDER_THAN_DAYS = -1;

    @Requirement
    private SnapshotRemover snapshotRemover;

    @Override
    protected String getRepositoryFieldId()
    {
        return SnapshotRemovalTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
    }

    public int getMinSnapshotsToKeep()
    {
        String param = getParameters().get( SnapshotRemovalTaskDescriptor.MIN_TO_KEEP_FIELD_ID );

        if ( StringUtils.isEmpty( param ) )
        {
            return DEFAULT_MIN_SNAPSHOTS_TO_KEEP;
        }

        return Integer.parseInt( param );
    }

    public void setMinSnapshotsToKeep( int minSnapshotsToKeep )
    {
        getParameters().put( SnapshotRemovalTaskDescriptor.MIN_TO_KEEP_FIELD_ID, Integer.toString( minSnapshotsToKeep ) );
    }

    public int getRemoveOlderThanDays()
    {
        String param = getParameters().get( SnapshotRemovalTaskDescriptor.KEEP_DAYS_FIELD_ID );

        if ( StringUtils.isEmpty( param ) )
        {
            return DEFAULT_OLDER_THAN_DAYS;
        }

        return Integer.parseInt( param );
    }

    public void setRemoveOlderThanDays( int removeOlderThanDays )
    {
        getParameters().put( SnapshotRemovalTaskDescriptor.KEEP_DAYS_FIELD_ID, Integer.toString( removeOlderThanDays ) );
    }

    public boolean isRemoveIfReleaseExists()
    {
        return Boolean.parseBoolean( getParameters().get( SnapshotRemovalTaskDescriptor.REMOVE_WHEN_RELEASED_FIELD_ID ) );
    }

    public void setRemoveIfReleaseExists( boolean removeIfReleaseExists )
    {
        getParameters().put( SnapshotRemovalTaskDescriptor.REMOVE_WHEN_RELEASED_FIELD_ID,
                             Boolean.toString( removeIfReleaseExists ) );
    }

    public boolean isDeleteImmediately()
    {
        return Boolean.parseBoolean( getParameters().get( SnapshotRemovalTaskDescriptor.DELETE_IMMEDIATELY ) );
    }

    public void setDeleteImmediately( boolean deleteImmediately )
    {
        getParameters().put( SnapshotRemovalTaskDescriptor.DELETE_IMMEDIATELY, Boolean.toString( deleteImmediately ) );
    }

    @Override
    public SnapshotRemovalResult doRun()
        throws Exception
    {
        SnapshotRemovalRequest req =
            new SnapshotRemovalRequest( getRepositoryId(), getMinSnapshotsToKeep(), getRemoveOlderThanDays(),
                isRemoveIfReleaseExists(), isDeleteImmediately());

        return snapshotRemover.removeSnapshots( req );
    }

    @Override
    protected String getAction()
    {
        return SYSTEM_REMOVE_SNAPSHOTS_ACTION;
    }

    @Override
    protected String getMessage()
    {
        if ( getRepositoryId() != null )
        {
            return "Removing snapshots from repository " + getRepositoryName();
        }
        else
        {
            return "Removing snapshots from all registered repositories";
        }
    }

}
