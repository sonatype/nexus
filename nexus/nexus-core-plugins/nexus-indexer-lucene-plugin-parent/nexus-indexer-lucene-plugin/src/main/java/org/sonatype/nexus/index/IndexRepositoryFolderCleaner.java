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
package org.sonatype.nexus.index;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.wastebasket.AbstractRepositoryFolderCleaner;
import org.sonatype.nexus.proxy.wastebasket.RepositoryFolderCleaner;

@Component( role = RepositoryFolderCleaner.class, hint = "indexer-lucene" )
public class IndexRepositoryFolderCleaner
    extends AbstractRepositoryFolderCleaner
{
    public void cleanRepositoryFolders( Repository repository, boolean deleteForever )
        throws IOException
    {
        if ( repository.getRepositoryKind().isFacetAvailable( ShadowRepository.class ) )
        {
            return;
        }

        File indexContextFolder =
            new File( getApplicationConfiguration().getWorkingDirectory(
                DefaultIndexerManager.INDEXER_WORKING_DIRECTORY_KEY ), repository.getId()
                + DefaultIndexerManager.CTX_SUFIX );

        if ( indexContextFolder.isDirectory() )
        {
            // indexes are not preserved
            delete( indexContextFolder, true );
        }
    }

}
