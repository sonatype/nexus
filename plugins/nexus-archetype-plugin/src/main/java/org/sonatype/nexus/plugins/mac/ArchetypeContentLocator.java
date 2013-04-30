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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.context.IndexingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.DefaultIndexerManager;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

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
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Repository repository;

    private final String repositoryContentUrl;

    private final DefaultIndexerManager nexusIndexer;

    private final MacPlugin macPlugin;

    private final ArtifactInfoFilter artifactInfoFilter;

    private volatile String payload;

    public ArchetypeContentLocator( final Repository repository, final String repositoryContentUrl,
                                    final DefaultIndexerManager nexusIndexer, final MacPlugin macPlugin,
                                    final ArtifactInfoFilter artifactInfoFilter )
    {
        this.repository = repository;
        this.repositoryContentUrl = repositoryContentUrl;
        this.nexusIndexer = nexusIndexer;
        this.macPlugin = macPlugin;
        this.artifactInfoFilter = artifactInfoFilter;
    }

    protected synchronized String generateCatalogPayload()
        throws IOException
    {
        if ( payload == null )
        {
            nexusIndexer.shared( repository, new DefaultIndexerManager.Runnable()
            {
                @Override
                public void run( IndexingContext context )
                    throws IOException
                {
                    // XXX igorf, this is not called when context == null, but we need to generate an empty catalog

                    payload = generateCatalogPayload( context );
                }
            } );
        }

        return payload;
    }

    private String generateCatalogPayload( IndexingContext context )
        throws IOException
    {
        final MacRequest req = new MacRequest( repository.getId(), repositoryContentUrl, artifactInfoFilter );

        // NEXUS-5216: Warn if indexing context is null (indexable=false) for given repository but continue
        // to return the correct empty catalog
        if ( context == null )
        {
            logger.info( "Archetype Catalog for repository {} is not buildable as it lacks IndexingContext (indexable=false?).",
                         RepositoryStringUtils.getHumanizedNameString( repository ) );
        }

        // get the catalog
        final ArchetypeCatalog catalog = macPlugin.listArcherypesAsCatalog( req, context );
        // serialize it to XML
        final StringWriter sw = new StringWriter();
        final ArchetypeCatalogXpp3Writer writer = new ArchetypeCatalogXpp3Writer();
        writer.write( sw, catalog );
        return sw.toString();
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
