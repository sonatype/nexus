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
package org.sonatype.nexus.proxy.events;

import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The event fired on item delete carrying item from where deletion is executed. For one deletion (ie. invocation of
 * {@link Repository#deleteItem(org.sonatype.nexus.proxy.ResourceStoreRequest)}) there will be only one event of this
 * type fired. If item is a collection, other delete related events will be fired, see
 * {@link RepositoryItemEventDeleteItem} event. All delete events are fired before actual deletion is made,
 * hence, the items carried by these events are still present and even it's content is reachable (if any).
 * 
 * @author cstamas
 * @since 2.1
 */
public class RepositoryItemEventDeleteRoot
    extends RepositoryItemEventDelete
{
    public RepositoryItemEventDeleteRoot( final Repository repository, final StorageItem item )
    {
        super( repository, item );
    }
}
