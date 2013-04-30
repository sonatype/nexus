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
package org.sonatype.nexus.integrationtests;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.sonatype.nexus.test.utils.TestProperties;

import com.google.inject.Module;

public class TestContainer
{
    private static TestContainer SELF = null;

    public static TestContainer getInstance()
    {
        synchronized ( TestContainer.class )
        {
            if ( SELF == null )
            {
                SELF = new TestContainer();
            }
        }
        return SELF;
    }

    public static String getBasedir()
    {
        String basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "" ).getAbsolutePath();
        }

        return basedir;
    }

    // ==

    private final TestContext testContext;

    private PlexusContainer plexusContainer;

    private TestContainer()
    {
        testContext = new TestContext();
    }

    public TestContext getTestContext()
    {
        return testContext;
    }

    public PlexusContainer getPlexusContainer()
    {
        return plexusContainer;
    }

    public synchronized void startPlexusContainer( final Class<?> clazz, final Module... modules )
    {
        if ( plexusContainer == null )
        {
            plexusContainer = setupContainer( clazz, modules );
        }
    }

    public synchronized void stopPlexusContainer()
    {
        if ( plexusContainer != null )
        {
            plexusContainer.dispose();
            plexusContainer = null;
        }
    }

    public void reset()
    {
        getTestContext().reset();
    }

    public <T> T invokeAsAdministrator( final Callable<T> callable )
        throws Exception
    {
        final TestContext ctx = TestContainer.getInstance().getTestContext();
        final String username = ctx.getUsername();
        final String password = ctx.getPassword();
        final boolean secure = ctx.isSecureTest();
        ctx.useAdminForRequests();
        ctx.setSecureTest( true );

        try
        {
            return callable.call();
        }
        finally
        {
            ctx.setUsername( username );
            ctx.setPassword( password );
            ctx.setSecureTest( secure );
        }
    }

    // ==

    protected PlexusContainer setupContainer( Class<?> baseClass, Module... modules )
    {
        // ----------------------------------------------------------------------------
        // Context Setup
        // ----------------------------------------------------------------------------

        Map<Object, Object> context = new HashMap<Object, Object>();

        context.put( "basedir", getBasedir() );
        context.putAll( getTestProperties() );

        boolean hasPlexusHome = context.containsKey( "plexus.home" );

        if ( !hasPlexusHome )
        {
            File f = new File( getBasedir(), "target/plexus-home" );

            if ( !f.isDirectory() )
            {
                f.mkdir();
            }

            context.put( "plexus.home", f.getAbsolutePath() );
        }

        // ----------------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------------

        ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration();
        containerConfiguration.setName( "test" );
        containerConfiguration.setContext( context );
        containerConfiguration.setContainerConfiguration( baseClass.getName().replace( '.', '/' ) + ".xml" );
        containerConfiguration.setAutoWiring( true );
        containerConfiguration.setClassPathScanning( PlexusConstants.SCANNING_INDEX );

        try
        {
            return new DefaultPlexusContainer( containerConfiguration, modules );
        }
        catch ( PlexusContainerException e )
        {
            e.printStackTrace();
            fail( "Failed to create plexus container." );
            return null;
        }
    }

    protected Map<String, String> getTestProperties()
    {
        HashMap<String, String> variables = new HashMap<String, String>();
        variables.putAll( TestProperties.getAll() );
        return variables;
    }

}
