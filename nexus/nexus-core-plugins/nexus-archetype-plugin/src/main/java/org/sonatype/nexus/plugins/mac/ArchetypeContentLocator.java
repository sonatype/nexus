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
package org.sonatype.nexus.plugins.mac;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.apache.maven.index.ArtifactInfoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.DefaultIndexerManager;
import org.sonatype.nexus.index.NexusIndexingContext;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

import com.google.common.base.Preconditions;

/**
 * A content locator to generate archetype catalog. This way, the actual work (search, archetype catalog model fillup
 * from results, converting it to string and flushing it as byte array backed stream) is postponed to very last moment,
 * when the content itself is asked for.
 * 
 * @author cstamas
 */
public class ArchetypeContentLocator
    implements ContentLocator
{
    private final Logger logger;

    private final Repository repository;

    private final String repositoryContentUrl;

    private final DefaultIndexerManager indexerManager;

    private final MacPlugin macPlugin;

    private final ArtifactInfoFilter artifactInfoFilter;

    private volatile String payload;

    /**
     * Constructor.
     * 
     * @param repository
     * @param repositoryContentUrl
     * @param indexerManager
     * @param macPlugin
     * @param artifactInfoFilter
     */
    public ArchetypeContentLocator( final Repository repository, final String repositoryContentUrl,
                                    final DefaultIndexerManager indexerManager, final MacPlugin macPlugin,
                                    final ArtifactInfoFilter artifactInfoFilter )
    {
        this.logger = LoggerFactory.getLogger( getClass() );
        this.repository = Preconditions.checkNotNull( repository );
        this.repositoryContentUrl = repositoryContentUrl;
        this.indexerManager = Preconditions.checkNotNull( indexerManager );
        this.macPlugin = Preconditions.checkNotNull( macPlugin );
        this.artifactInfoFilter = Preconditions.checkNotNull( artifactInfoFilter );
    }

    protected synchronized String generateCatalogPayload()
        throws IOException
    {
        if ( payload == null )
        {
            final ArchetypeCatalog catalog = getArchetypeCatalog( repository );
            final StringWriter sw = new StringWriter();
            final ArchetypeCatalogXpp3Writer writer = new ArchetypeCatalogXpp3Writer();
            writer.write( sw, catalog );
            payload = sw.toString();
        }

        return payload;
    }

    protected ArchetypeCatalog getArchetypeCatalog( final Repository repository )
        throws IOException
    {
        final NexusIndexingContext indexingContext = indexerManager.getRepositoryIndexContext( repository );
        if ( indexingContext == null )
        {
            // NEXUS-5216: Warn if indexing context is null (indexable=false) for given repository but continue
            // to return the correct empty catalog
            logger.info(
                "Archetype Catalog for repository {} is not buildable as it lacks IndexingContext (indexable=false?).",
                RepositoryStringUtils.getHumanizedNameString( repository ) );
            return new ArchetypeCatalog();
        }
        else
        {
            final boolean locked = indexingContext.getLock().readLock().tryLock();
            if ( !locked )
            {
                // we are unable to get the catalog, reindex holds exclusive lock
                // to avoid requests piling up, just return empty catalog
                logger.info(
                    "Archetype Catalog for repository {} is not buildable as its IndexingContext is exclusively held by some other party (being reindexed?).",
                    RepositoryStringUtils.getHumanizedNameString( repository ) );
                return new ArchetypeCatalog();
            }
            else
            {
                // we have all we need, so le'ts do it
                // there is no chance, that someone grabbed exclusive lock here
                // as we already have read lock
                try
                {
                    // get the catalog
                    final MacRequest req =
                        new MacRequest( repository.getId(), repositoryContentUrl, artifactInfoFilter );
                    return macPlugin.listArcherypesAsCatalog( req, indexingContext );
                }
                finally
                {
                    indexingContext.getLock().readLock().unlock();
                }
            }
        }
    }

    @Override
    public InputStream getContent()
        throws IOException
    {
        return new ByteArrayInputStream( generateCatalogPayload().getBytes( "UTF-8" ) );
    }

    @Override
    public String getMimeType()
    {
        return "text/xml";
    }

    @Override
    public boolean isReusable()
    {
        return true;
    }
}
