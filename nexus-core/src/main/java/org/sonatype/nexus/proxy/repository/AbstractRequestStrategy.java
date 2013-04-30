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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * A helper base class that makes it easier to create processors.
 * 
 * @author cstamas
 * @since 2.5
 */
public abstract class AbstractRequestStrategy
    implements RequestStrategy
{
    @Override
    public void onHandle( Repository repository, ResourceStoreRequest request, Action action )
        throws ItemNotFoundException, IllegalOperationException
    {
        // nop
    }

    @Override
    public void onServing( final Repository repository, final ResourceStoreRequest request, final StorageItem item )
        throws ItemNotFoundException, IllegalOperationException
    {
        // nop
    }

    @Override
    public void onRemoteAccess( ProxyRepository proxy, ResourceStoreRequest request, StorageItem item )
        throws ItemNotFoundException, IllegalOperationException
    {
        // nop
    }
}
