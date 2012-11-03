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
package org.sonatype.nexus.plugins.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.inject.Parameters;
import org.sonatype.plugins.model.PluginMetadata;
import org.sonatype.plugins.model.io.xpp3.PluginModelXpp3Reader;

/**
 * Abstract {@link NexusPluginRepository} that can parse plugin metadata.
 */
abstract class AbstractNexusPluginRepository
    implements NexusPluginRepository
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final PluginModelXpp3Reader PLUGIN_METADATA_READER = new PluginModelXpp3Reader();

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    @Inject
    @Parameters
    private Map<String, String> variables;

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    /**
     * Parses a {@code plugin.xml} resource into plugin metadata.
     * 
     * @param pluginXml The plugin.xml URL
     * @return Nexus plugin metadata
     */
    final PluginMetadata getPluginMetadata( final URL pluginXml )
        throws IOException
    {
        final InputStream in = pluginXml.openStream();
        try
        {
            final Reader reader = new InterpolationFilterReader( ReaderFactory.newXmlReader( in ), variables );
            return PLUGIN_METADATA_READER.read( reader, false );
        }
        catch ( final XmlPullParserException e )
        {
            throw new IOException( "Problem parsing: " + pluginXml + " reason: " + e );
        }
        finally
        {
            IOUtil.close( in );
        }
    }
}
