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
package org.sonatype.nexus.plugins.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginMetadata;

import com.google.common.io.Closeables;

/**
 * Abstract {@link NexusPluginRepository} backed by a file-system.
 */
public abstract class AbstractFileNexusPluginRepository
    extends AbstractNexusPluginRepository
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final String PLUGIN_XML = "META-INF/nexus/plugin.xml";

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    @Inject
    private ArtifactPackagingMapper packagingMapper;

    /**
     * To store enabled statuses a la "cache".
     */
    private volatile Properties pluginIndex;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public final Map<GAVCoordinate, PluginMetadata> findAvailablePlugins()
    {
        final File[] plugins = getPluginFolders();
        if ( null == plugins )
        {
            return Collections.emptyMap();
        }

        final Map<GAVCoordinate, PluginMetadata> installedPlugins =
            new HashMap<GAVCoordinate, PluginMetadata>( plugins.length );

        for ( final File f : plugins )
        {
            if ( !f.isDirectory() )
            {
                continue;
            }
            final File pluginJar = getPluginJar( f );
            if ( !pluginJar.isFile() )
            {
                continue;
            }
            final PluginMetadata md = getPluginMetadata( pluginJar );
            if ( null == md )
            {
                continue;
            }
            installedPlugins.put( new GAVCoordinate( md.getGroupId(), md.getArtifactId(), md.getVersion() ), md );
        }

        return installedPlugins;
    }

    public final PluginRepositoryArtifact resolveArtifact( final GAVCoordinate gav )
        throws NoSuchPluginRepositoryArtifactException
    {
        return new PluginRepositoryArtifact( gav, resolvePluginJar( gav ), this );
    }

    public final PluginRepositoryArtifact resolveDependencyArtifact( final PluginRepositoryArtifact plugin,
                                                                     final GAVCoordinate gav )
        throws NoSuchPluginRepositoryArtifactException
    {
        final File dependencyArtifact = resolveSnapshotOrReleaseDependencyArtifact( plugin, gav );

        if ( dependencyArtifact == null || !dependencyArtifact.isFile() )
        {
            throw new NoSuchPluginRepositoryArtifactException( this, gav );
        }

        return new PluginRepositoryArtifact( gav, dependencyArtifact, this );
    }

    public final PluginMetadata getPluginMetadata( final GAVCoordinate gav )
        throws NoSuchPluginRepositoryArtifactException
    {
        return getPluginMetadata( resolvePluginJar( gav ) );
    }

    public synchronized boolean isEnabledPlugin( final GAVCoordinate gav )
    {
        if ( pluginIndex == null )
        {
            pluginIndex = loadPluginIndex();
        }

        return Boolean.valueOf( pluginIndex.getProperty( gav.toString() ) );
    }

    public synchronized void setEnabledPlugin( final GAVCoordinate gav, final boolean value )
    {
        final Properties index = loadPluginIndex();
        index.setProperty( gav.toString(), Boolean.toString( value ) );
        storePluginIndex( index );
        pluginIndex = null;
    }

    // ----------------------------------------------------------------------
    // Customizable methods
    // ----------------------------------------------------------------------

    protected abstract File getNexusPluginsDirectory();

    protected File getPluginDependenciesFolder( final PluginRepositoryArtifact plugin )
    {
        return new File( getPluginFolder( plugin.getCoordinate() ), "dependencies" );
    }

    protected File resolveSnapshotOrReleaseDependencyArtifact( final PluginRepositoryArtifact plugin,
                                                               final GAVCoordinate gav )
    {
        // TODO (cstamas): gav has baseVersion, we need to be a bit smarter about resolving it against timestamped too
        // try with baseVersion (-SNAPSHOT) will work if bundle was assembled from stuff in local repository
        // or is part of this same build
        // Also, this part will work with release ones
        final File dependenciesFolder = getPluginDependenciesFolder( plugin );
        File dependencyArtifact = new File( dependenciesFolder, gav.getFinalName( packagingMapper ) );
        if ( dependencyArtifact.isFile() )
        {
            return dependencyArtifact;
        }

        // for timestamped snapshots, we need another try
        if ( Gav.isSnapshot( gav.getVersion() ) )
        {
            // is a snapshot, but is a a timestamped one, so let's try to find it
            final StringBuilder buf = new StringBuilder();
            if ( StringUtils.isNotEmpty( gav.getClassifier() ) )
            {
                buf.append( '-' ).append( gav.getClassifier() );
            }
            if ( StringUtils.isNotEmpty( gav.getType() ) )
            {
                buf.append( '.' ).append( packagingMapper.getExtensionForPackaging( gav.getType() ) );
            }
            else
            {
                buf.append( ".jar" );
            }

            final String prefix = gav.getArtifactId() + "-";
            final String suffix = buf.toString();

            File[] dependencies = dependenciesFolder.listFiles( new FilenameFilter()
            {
                @Override
                public boolean accept( File dir, String name )
                {
                    return name.startsWith( prefix ) && name.endsWith( suffix );
                }
            } );

            if ( dependencies != null && dependencies.length == 1 && dependencies[0].isFile() )
            {
                return dependencies[0];
            }
        }

        return null;
    }

    protected File[] getPluginFolders()
    {
        return getNexusPluginsDirectory().listFiles();
    }

    protected File getPluginFolder( final GAVCoordinate gav )
    {
        return new File( getNexusPluginsDirectory(), gav.getArtifactId() + '-' + gav.getVersion() );
    }

    protected File getPluginIndexFile()
    {
        return new File( getNexusPluginsDirectory(), "repository-index.properties" );
    }

    protected synchronized Properties loadPluginIndex()
    {
        Properties index = new Properties();

        try
        {
            FileInputStream fis = new FileInputStream( getPluginIndexFile() );
            try
            {
                index.load( fis );
            }
            finally
            {
                Closeables.closeQuietly( fis );
            }
        }
        catch ( IOException e )
        {
            // nothing, index does not exists, we need to create it from scratch
            final Map<GAVCoordinate, PluginMetadata> availablePlugins = findAvailablePlugins();
            for ( GAVCoordinate coord : availablePlugins.keySet() )
            {
                index.put( coord.toString(), Boolean.TRUE.toString() );
            }

            storePluginIndex( index );
        }

        return index;
    }

    protected synchronized void storePluginIndex( final Properties index )
    {
        try
        {
            FileOutputStream fos = new FileOutputStream( getPluginIndexFile() );

            try
            {
                index.store( fos, "Nexus Plugin Manager Index" );
            }
            finally
            {
                Closeables.closeQuietly( fos );
            }
        }
        catch ( IOException e )
        {
            // huh?
        }
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    private static final File getPluginJar( final File pluginFolder )
    {
        return new File( pluginFolder, pluginFolder.getName() + ".jar" );
    }

    private final File resolvePluginJar( final GAVCoordinate gav )
        throws NoSuchPluginRepositoryArtifactException
    {
        final File pluginFolder = getPluginFolder( gav );
        final File pluginJar = getPluginJar( pluginFolder );
        if ( pluginJar.isFile() )
        {
            return pluginJar;
        }
        throw new NoSuchPluginRepositoryArtifactException( this, gav );
    }

    private final PluginMetadata getPluginMetadata( final File file )
    {
        try
        {
            return getPluginMetadata( new URL( "jar:" + file.toURI() + "!/" + PLUGIN_XML ) );
        }
        catch ( final IOException e )
        {
            return null;
        }
    }
}
