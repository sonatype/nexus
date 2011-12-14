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
package org.sonatype.nexus.plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.sonatype.guice.bean.reflect.ClassSpace;
import org.sonatype.guice.bean.reflect.URLClassSpace;
import org.sonatype.guice.nexus.binders.NexusAnnotatedBeanModule;
import org.sonatype.guice.plexus.binders.PlexusXmlBeanModule;
import org.sonatype.guice.plexus.config.PlexusBeanModule;
import org.sonatype.inject.Parameters;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.plugins.events.PluginActivatedEvent;
import org.sonatype.nexus.plugins.events.PluginRejectedEvent;
import org.sonatype.nexus.plugins.repository.NoSuchPluginRepositoryArtifactException;
import org.sonatype.nexus.plugins.repository.PluginRepositoryArtifact;
import org.sonatype.nexus.plugins.repository.PluginRepositoryManager;
import org.sonatype.nexus.plugins.rest.NexusResourceBundle;
import org.sonatype.nexus.plugins.rest.StaticResource;
import org.sonatype.nexus.proxy.maven.version.GenericVersionParser;
import org.sonatype.nexus.proxy.maven.version.InvalidVersionSpecificationException;
import org.sonatype.nexus.proxy.maven.version.Version;
import org.sonatype.nexus.proxy.maven.version.VersionParser;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.util.AlphanumComparator;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.ClasspathDependency;
import org.sonatype.plugins.model.PluginDependency;
import org.sonatype.plugins.model.PluginMetadata;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

/**
 * Default {@link NexusPluginManager} implementation backed by a {@link PluginRepositoryManager}.
 */
@Named
@Singleton
public final class DefaultNexusPluginManager
    extends AbstractLoggingComponent
    implements NexusPluginManager
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    @Inject
    private PluginRepositoryManager repositoryManager;

    @Inject
    private ApplicationEventMulticaster eventMulticaster;

    @Inject
    private RepositoryTypeRegistry repositoryTypeRegistry;

    @Inject
    private MimeSupport mimeSupport;

    @Inject
    private DefaultPlexusContainer container;

    @Inject
    @Parameters
    private Map<String, String> variables;

    private final Map<GAVCoordinate, PluginDescriptor> activePlugins = new HashMap<GAVCoordinate, PluginDescriptor>();

    private final Map<GAVCoordinate, PluginResponse> pluginResponses = new HashMap<GAVCoordinate, PluginResponse>();

    private final VersionParser versionParser = new GenericVersionParser();

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public Map<GAVCoordinate, PluginDescriptor> getActivatedPlugins()
    {
        return new HashMap<GAVCoordinate, PluginDescriptor>( activePlugins );
    }

    public Map<GAVCoordinate, PluginMetadata> getInstalledPlugins()
    {
        return repositoryManager.findAvailablePlugins();
    }

    public Map<GAVCoordinate, PluginResponse> getPluginResponses()
    {
        return new HashMap<GAVCoordinate, PluginResponse>( pluginResponses );
    }

    public Collection<PluginManagerResponse> activateInstalledPlugins()
    {
        final List<PluginManagerResponse> result = new ArrayList<PluginManagerResponse>();

        // if multiple V's for GAs are found, choose the one with biggest version (and pray that plugins has sane
        // versioning)
        Map<GAVCoordinate, PluginMetadata> filteredPlugins =
            filterInstalledPlugins( repositoryManager.findAvailablePlugins() );

        for ( final GAVCoordinate gav : filteredPlugins.keySet() )
        {
            // activate what we found in reposes
            result.add( activatePlugin( gav, true ) );
        }
        return result;
    }

    public boolean isActivatedPlugin( final GAVCoordinate gav )
    {
        return isActivatedPlugin( gav, true );
    }

    public boolean isEnabledPlugin( final GAVCoordinate gav )
    {
        return repositoryManager.isEnabledPlugin( gav );
    }

    public void setEnabledPlugin( final GAVCoordinate gav, final boolean value )
    {
        repositoryManager.setEnabledPlugin( gav, value );
    }

    public PluginManagerResponse activatePlugin( final GAVCoordinate gav )
    {
        return activatePlugin( gav, true );
    }

    public PluginManagerResponse deactivatePlugin( final GAVCoordinate gav )
    {
        throw new UnsupportedOperationException(); // TODO
    }

    public boolean installPluginBundle( final URL bundle )
        throws IOException
    {
        throw new UnsupportedOperationException(); // TODO
    }

    public boolean uninstallPluginBundle( final GAVCoordinate gav )
        throws IOException
    {
        throw new UnsupportedOperationException(); // TODO
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    /**
     * Filters a map of GAVCoordinates by "max" version and by it's "enabled" status. Hence, in the result Map, it is
     * guaranteed that only one GA combination will exists, and if input contained multiple V's for same GA, the one GAV
     * contained in result with have max V.
     * 
     * @param installedPlugins
     * @return
     */
    protected Map<GAVCoordinate, PluginMetadata> filterInstalledPlugins( final Map<GAVCoordinate, PluginMetadata> installedPlugins )
    {
        final HashMap<GAVCoordinate, PluginMetadata> result =
            new HashMap<GAVCoordinate, PluginMetadata>( installedPlugins.size() );

        nextInstalledEntry: for ( Map.Entry<GAVCoordinate, PluginMetadata> installedEntry : installedPlugins.entrySet() )
        {
            // if not enabled, filter it out
            if ( !isEnabledPlugin( installedEntry.getKey() ) )
            {
                getLogger().info( "Plugin [{}] is disabled.", installedEntry.getKey().toString() );
                result.remove( installedEntry.getKey() );
                continue nextInstalledEntry;
            }

            // if enabled, we want the max(V) of same GAs
            for ( Map.Entry<GAVCoordinate, PluginMetadata> resultEntry : result.entrySet() )
            {
                if ( resultEntry.getKey().matchesByGA( installedEntry.getKey() ) )
                {
                    if ( compareVersionStrings( resultEntry.getKey().getVersion(), installedEntry.getKey().getVersion() ) < 0 )
                    {
                        // result contains smaller version than installedOne, remove it
                        result.remove( resultEntry.getKey() );
                    }
                    else
                    {
                        continue nextInstalledEntry;
                    }
                }
            }
            result.put( installedEntry.getKey(), installedEntry.getValue() );
        }

        return result;
    }

    protected int compareVersionStrings( final String v1str, final String v2str )
    {
        try
        {
            final Version v1 = versionParser.parseVersion( v1str );
            final Version v2 = versionParser.parseVersion( v2str );

            return v1.compareTo( v2 );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // fall back to "sane" human alike sorting of strings
            return new AlphanumComparator().compare( v1str, v2str );
        }
    }

    protected GAVCoordinate getActivatedPluginGav( final GAVCoordinate gav, final boolean strict )
    {
        // try exact match 1st
        if ( activePlugins.containsKey( gav ) )
        {
            return gav;
        }

        // if we are lax, try by GA
        if ( !strict )
        {
            for ( GAVCoordinate coord : activePlugins.keySet() )
            {
                if ( coord.matchesByGA( gav ) )
                {
                    return coord;
                }
            }
        }

        // sad face here
        return null;
    }

    protected boolean isActivatedPlugin( final GAVCoordinate gav, final boolean strict )
    {
        return getActivatedPluginGav( gav, strict ) != null;
    }

    protected PluginManagerResponse activatePlugin( final GAVCoordinate gav, final boolean strict )
    {
        final GAVCoordinate activatedGav = getActivatedPluginGav( gav, strict );
        if ( activatedGav == null )
        {
            final PluginManagerResponse response = new PluginManagerResponse( gav, PluginActivationRequest.ACTIVATE );
            try
            {
                activatePlugin( repositoryManager.resolveArtifact( gav ), response );
            }
            catch ( final NoSuchPluginRepositoryArtifactException e )
            {
                reportMissingPlugin( response, e );
            }
            return response;
        }
        else
        {
            return new PluginManagerResponse( activatedGav, PluginActivationRequest.ACTIVATE );
        }
    }

    private void activatePlugin( final PluginRepositoryArtifact plugin, final PluginManagerResponse response )
        throws NoSuchPluginRepositoryArtifactException
    {
        final GAVCoordinate pluginGAV = plugin.getCoordinate();
        final PluginMetadata metadata = plugin.getPluginMetadata();

        final PluginDescriptor descriptor = new PluginDescriptor( pluginGAV );
        descriptor.setPluginMetadata( metadata );

        final PluginResponse result = new PluginResponse( pluginGAV, PluginActivationRequest.ACTIVATE );
        result.setPluginDescriptor( descriptor );

        activePlugins.put( pluginGAV, descriptor );

        final List<GAVCoordinate> importList = new ArrayList<GAVCoordinate>();
        final List<GAVCoordinate> resolvedList = new ArrayList<GAVCoordinate>();
        for ( final PluginDependency pd : metadata.getPluginDependencies() )
        {
            // here, a plugin might express a need for GAV1, but GAV2 might be already activated
            // since today we just "play" dependency resolution, we support GA resolution only
            // so, we say "relax version matching" and rely on luck for now it will work
            final GAVCoordinate gav = new GAVCoordinate( pd.getGroupId(), pd.getArtifactId(), pd.getVersion() );
            final PluginManagerResponse dependencyActivationResponse = activatePlugin( gav, false );
            response.addPluginManagerResponse( dependencyActivationResponse );
            importList.add( gav );
            resolvedList.add( dependencyActivationResponse.getOriginator() );
        }
        descriptor.setImportedPlugins( importList );
        descriptor.setResolvedPlugins( resolvedList );

        if ( !response.isSuccessful() )
        {
            result.setAchievedGoal( PluginActivationResult.BROKEN );
        }
        else
        {
            try
            {
                createPluginInjector( plugin, descriptor );
                result.setAchievedGoal( PluginActivationResult.ACTIVATED );
            }
            catch ( final Throwable e )
            {
                result.setThrowable( e );
            }
        }

        reportActivationResult( response, result );
    }

    private void createPluginInjector( final PluginRepositoryArtifact plugin, final PluginDescriptor descriptor )
        throws NoSuchPluginRepositoryArtifactException
    {
        final String realmId = descriptor.getPluginCoordinates().toString();
        final ClassRealm containerRealm = container.getContainerRealm();
        ClassRealm pluginRealm;
        try
        {
            pluginRealm = containerRealm.createChildRealm( realmId );
        }
        catch ( final DuplicateRealmException e1 )
        {
            try
            {
                pluginRealm = containerRealm.getWorld().getRealm( realmId );
            }
            catch ( final NoSuchRealmException e2 )
            {
                throw new IllegalStateException();
            }
        }

        final List<URL> scanList = new ArrayList<URL>();

        final URL pluginURL = toURL( plugin );
        if ( null != pluginURL )
        {
            pluginRealm.addURL( pluginURL );
            scanList.add( pluginURL );
        }

        boolean needDeprecationWarning = false;
        for ( final ClasspathDependency d : descriptor.getPluginMetadata().getClasspathDependencies() )
        {
            final GAVCoordinate gav =
                new GAVCoordinate( d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getClassifier(), d.getType() );

            final URL url = toURL( repositoryManager.resolveDependencyArtifact( plugin, gav ) );
            if ( null != url )
            {
                pluginRealm.addURL( url );
                if ( d.isShared() || d.isHasComponents() )
                {
                    scanList.add( url );
                }

                // warning about deprecation goes into log
                if ( d.isHasComponents() )
                {
                    needDeprecationWarning = true;
                }
            }
        }

        if ( needDeprecationWarning )
        {
            getLogger().warn(
                "Plugin [{}] was built using deprecated plugin configuration \"componentDependencies\" (or with old plugin). Please update it's build accordingly and rebuild the plugin with latest app-lifecycle plugin!",
                descriptor.getPluginCoordinates().toString() );
        }

        for ( final GAVCoordinate gav : descriptor.getResolvedPlugins() )
        {
            final String importId = gav.toString();
            for ( final String classname : activePlugins.get( gav ).getExportedClassnames() )
            {
                try
                {
                    pluginRealm.importFrom( importId, classname );
                }
                catch ( final NoSuchRealmException e )
                {
                    // should never happen
                }
            }
        }

        final List<String> exportedClassNames = new ArrayList<String>();
        final List<RepositoryTypeDescriptor> repositoryTypes = new ArrayList<RepositoryTypeDescriptor>();
        final List<StaticResource> staticResources = new ArrayList<StaticResource>();

        final NexusResourceBundle resourceBundle = new NexusResourceBundle()
        {
            public List<StaticResource> getContributedResouces()
            {
                return staticResources;
            }
        };

        final Module resourceModule = new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind( NexusResourceBundle.class ).annotatedWith( Names.named( realmId ) ).toInstance( resourceBundle );
            }
        };

        final List<PlexusBeanModule> beanModules = new ArrayList<PlexusBeanModule>();

        final ClassSpace pluginSpace = new URLClassSpace( pluginRealm );
        beanModules.add( new PlexusXmlBeanModule( pluginSpace, variables ) );

        final ClassSpace annSpace = new URLClassSpace( pluginRealm, scanList.toArray( new URL[scanList.size()] ) );
        beanModules.add( new NexusAnnotatedBeanModule( annSpace, variables, exportedClassNames, repositoryTypes ) );

        container.addPlexusInjector( beanModules, resourceModule );

        for ( final RepositoryTypeDescriptor r : repositoryTypes )
        {
            repositoryTypeRegistry.registerRepositoryTypeDescriptors( r );
        }

        final Enumeration<URL> e = pluginSpace.findEntries( "static/", null, true );
        while ( e.hasMoreElements() )
        {
            final URL url = e.nextElement();
            final String path = getPublishedPath( url );
            if ( path != null )
            {
                staticResources.add( new PluginStaticResource( url, path,
                    mimeSupport.guessMimeTypeFromPath( url.getPath() ) ) );
            }
        }

        descriptor.setExportedClassnames( exportedClassNames );
        descriptor.setRepositoryTypes( repositoryTypes );
        descriptor.setStaticResources( staticResources );
    }

    private URL toURL( final PluginRepositoryArtifact artifact )
    {
        try
        {
            return artifact.getFile().toURI().toURL();
        }
        catch ( final MalformedURLException e )
        {
            return null; // should never happen
        }
    }

    private String getPublishedPath( final URL resourceURL )
    {
        final String path = resourceURL.toExternalForm();
        final int index = path.indexOf( "jar!/" );
        return index > 0 ? path.substring( index + 4 ) : null;
    }

    private void reportMissingPlugin( final PluginManagerResponse response,
                                      final NoSuchPluginRepositoryArtifactException cause )
    {
        final GAVCoordinate gav = cause.getCoordinate();
        final PluginResponse result = new PluginResponse( gav, response.getRequest() );
        result.setThrowable( cause );
        result.setAchievedGoal( PluginActivationResult.MISSING );

        response.addPluginResponse( result );
        pluginResponses.put( gav, result );
    }

    private void reportActivationResult( final PluginManagerResponse response, final PluginResponse result )
    {
        final Event<NexusPluginManager> pluginEvent;
        final GAVCoordinate gav = result.getPluginCoordinates();
        if ( result.isSuccessful() )
        {
            pluginEvent = new PluginActivatedEvent( this, result.getPluginDescriptor() );
        }
        else
        {
            pluginEvent = new PluginRejectedEvent( this, gav, result.getThrowable() );
            activePlugins.remove( gav );
        }

        response.addPluginResponse( result );
        pluginResponses.put( gav, result );

        eventMulticaster.notifyEventListeners( pluginEvent );
    }
}
