/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.maven.tasks.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepoOrGroupComboFormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;

@Component( role = ScheduledTaskDescriptor.class, hint = "SnapshotRemoval", description = "Remove Snapshots From Repository" )
public class SnapshotRemovalTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
    public static final String ID = "SnapshotRemoverTask";

    public static final String REPO_OR_GROUP_FIELD_ID = "repositoryId";

    public static final String MIN_TO_KEEP_FIELD_ID = "minSnapshotsToKeep";

    public static final String KEEP_DAYS_FIELD_ID = "removeOlderThanDays";

    public static final String REMOVE_WHEN_RELEASED_FIELD_ID = "removeIfReleaseExists";

    public static final String DELETE_IMMEDIATELY = "deleteImmediately";

    private final RepoOrGroupComboFormField repoField = new RepoOrGroupComboFormField( REPO_OR_GROUP_FIELD_ID,
                                                                                       FormField.MANDATORY );

    private final NumberTextFormField minToKeepField =
        new NumberTextFormField( MIN_TO_KEEP_FIELD_ID, "Minimum snapshot count",
                                 "Minimum number of snapshots to keep for one GAV.", FormField.OPTIONAL );

    private final NumberTextFormField keepDaysField =
        new NumberTextFormField(
                                 KEEP_DAYS_FIELD_ID,
                                 "Snapshot retention (days)",
                                 "The job will purge all snapshots older than the entered number of days, but will obey to Min. count of snapshots to keep.",
                                 FormField.OPTIONAL );

    private final CheckboxFormField removeWhenReleasedField =
        new CheckboxFormField(
                               REMOVE_WHEN_RELEASED_FIELD_ID,
                               "Remove if released",
                               "The job will purge all snapshots that have a corresponding released artifact (same version not including the -SNAPSHOT).",
                               FormField.OPTIONAL );

    private final CheckboxFormField deleteImmediatelyField =
        new CheckboxFormField( DELETE_IMMEDIATELY, "Delete immediately", "The job will not move deleted items into the repository trash but delete immediately.", FormField.OPTIONAL );


    public String getId()
    {
        return ID;
    }

    public String getName()
    {
        return "Remove Snapshots From Repository";
    }

    public List<FormField> formFields()
    {
        List<FormField> fields = new ArrayList<FormField>();

        fields.add( repoField );
        fields.add( minToKeepField );
        fields.add( keepDaysField );
        fields.add( removeWhenReleasedField );
        fields.add( deleteImmediatelyField );

        return fields;
    }
}
