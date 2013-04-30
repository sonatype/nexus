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
package org.sonatype.nexus.plugins.mac;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.context.IndexingContext;
import org.sonatype.nexus.index.DefaultIndexerManager;
import org.sonatype.nexus.index.IndexArtifactFilter;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.item.ContentGenerator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.RepositoryURLBuilder;

/**
 * Archetype catalog content generator.
 * 
 * @author cstamas
 */
@Named( ArchetypeContentGenerator.ID )
@Singleton
public class ArchetypeContentGenerator
    implements ContentGenerator
{
    public static final String ID = "ArchetypeContentGenerator";

    @Inject
    private MacPlugin macPlugin;

    @Inject
    private DefaultIndexerManager indexerManager;

    @Inject
    private IndexArtifactFilter indexArtifactFilter;

    @Inject
    private RepositoryURLBuilder repositoryURLBuilder;

    @Override
    public String getGeneratorId()
    {
        return ID;
    }

    @Override
    public ContentLocator generateContent( Repository repository, String path, StorageFileItem item )
        throws IllegalOperationException, ItemNotFoundException, LocalStorageException
    {
        // make length unknown (since it will be known only in the moment of actual content pull)
        item.setLength( -1 );

        ArtifactInfoFilter artifactInfoFilter = new ArtifactInfoFilter()
        {
            public boolean accepts( IndexingContext ctx, ArtifactInfo ai )
            {
                return indexArtifactFilter.filterArtifactInfo( ai );
            }
        };
        final String exposedRepositoryContentUrl = repositoryURLBuilder.getExposedRepositoryContentUrl( repository );

        return new ArchetypeContentLocator( repository, exposedRepositoryContentUrl, indexerManager, macPlugin,
                                            artifactInfoFilter );
    }
}
