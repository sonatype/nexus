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
package org.sonatype.nexus.plugin.discovery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.sonatype.nexus.plugin.ExpectPrompter;
import org.sonatype.nexus.plugin.discovery.fixture.ClientManagerFixture;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;
import org.sonatype.plexus.components.sec.dispatcher.model.io.xpp3.SecurityConfigurationXpp3Writer;

public class AbstractNexusDiscoveryTest
{
    protected DefaultNexusDiscovery discovery;

    protected ClientManagerFixture testClientManager;

    protected PlexusContainer container;

    protected SecDispatcher secDispatcher;

    private ArtifactHandlerManager artifactHandlerManager;

    protected ExpectPrompter prompter;

    protected static File secFile;

    protected static String encryptedPassword;

    protected static String clearTextPassword = "password";

    protected static String oldSecLocation;

    private static Random random = new Random();

    @BeforeClass
    public static void beforeAll()
        throws PlexusCipherException, IOException
    {
        DefaultPlexusCipher cipher = new DefaultPlexusCipher();

        String master =
            cipher.encryptAndDecorate( clearTextPassword, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );

        SettingsSecurity sec = new SettingsSecurity();
        sec.setMaster( master );

        secFile = new File( String.format( "target/settings-security.%s.xml", random.nextInt( Integer.MAX_VALUE ) ) );
        FileWriter writer = null;
        try
        {
            writer = new FileWriter( secFile );
            new SecurityConfigurationXpp3Writer().write( writer, sec );
        }
        finally
        {
            IOUtil.close( writer );
        }

        encryptedPassword = cipher.encryptAndDecorate( "password", "password" );

        Properties sysProps = System.getProperties();
        oldSecLocation = sysProps.getProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );
        sysProps.setProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, secFile.getAbsolutePath() );

        System.setProperties( sysProps );
    }

    @AfterClass
    public static void afterAll()
    {
        if ( oldSecLocation != null )
        {
            Properties sysProps = System.getProperties();
            sysProps.setProperty( DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, oldSecLocation );

            System.setProperties( sysProps );
        }

        try
        {
            FileUtils.forceDelete( secFile );
        }
        catch ( IOException e )
        {
        }
    }

    @Before
    public void beforeOne()
        throws ComponentLookupException, PlexusContainerException
    {
        container = new DefaultPlexusContainer();

        testClientManager = new ClientManagerFixture();

        secDispatcher = (SecDispatcher) container.lookup( SecDispatcher.class.getName(), "maven" );
        artifactHandlerManager = container.lookup( ArtifactHandlerManager.class );

        prompter = new ExpectPrompter();

        discovery = new DefaultNexusDiscovery( testClientManager, secDispatcher, prompter );
    }

    @After
    public void afterOne()
        throws ComponentLifecycleException
    {
        container.release( secDispatcher );
        container.release( artifactHandlerManager );
        container.dispose();
    }

    public Artifact createArtifactFromProject( MavenProject project )
    {
        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler( project.getPackaging() );

        return new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
            VersionRange.createFromVersion( project.getVersion() ), null, project.getPackaging(), null, handler, false );
    }
}
