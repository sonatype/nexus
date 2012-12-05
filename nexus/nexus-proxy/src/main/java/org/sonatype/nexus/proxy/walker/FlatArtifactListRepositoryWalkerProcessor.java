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
package org.sonatype.nexus.proxy.walker;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.proxy.item.StorageFileItem;

/**
 * Processor that simply builds a "flat" list of item paths that were met during the walk of the repository.
 *
 * @since 2.3
 */
public class FlatArtifactListRepositoryWalkerProcessor
    extends AbstractFileWalkerProcessor
{
    final ArrayList<String> paths = new ArrayList<String>();

    @Override
    protected void processFileItem( final WalkerContext context, final StorageFileItem item )
        throws Exception
    {
        if ( !item.isContentGenerated() )
        {
            paths.add( item.getPath() );
        }
    }

    /**
     * Returns the list of harvested paths.
     * @return
     */
    public List<String> getPaths()
    {
        return paths;
    }
}
