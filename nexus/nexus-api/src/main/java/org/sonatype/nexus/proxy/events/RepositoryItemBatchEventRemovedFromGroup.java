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
package org.sonatype.nexus.proxy.events;

import java.util.Collection;

import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Event fired by a group repository when some member repository is removed from a group, and hence, contributed items
 * are not anymore accessible over the given group.
 *
 * @author cstamas
 * @since 2.3
 */
public class RepositoryItemBatchEventRemovedFromGroup
    extends RepositoryItemBatchEvent
{

    private final Repository contributor;

    /**
     * Constructor.
     *
     * @param group
     * @param contributor
     * @param itemPaths
     */
    public RepositoryItemBatchEventRemovedFromGroup( final GroupRepository group, final Repository contributor,
        final Collection<String> itemPaths )
    {
        super( group, itemPaths );
        this.contributor = contributor;
    }

    /**
     * Returns the member repository that contributed items to group.
     *
     * @return
     */
    public Repository getContributor()
    {
        return contributor;
    }
}
