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
package org.sonatype.nexus.configuration.application.upgrade;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.StringWriter;
import java.util.TimeZone;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;
import org.sonatype.configuration.upgrade.SingleVersionUpgrader;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Writer;
import org.sonatype.nexus.test.NexusTestSupport;
import org.sonatype.security.configuration.model.SecurityConfiguration;
import org.sonatype.security.configuration.model.io.xpp3.SecurityConfigurationXpp3Writer;
import org.sonatype.security.configuration.source.FileSecurityConfigurationSource;
import org.sonatype.security.configuration.source.SecurityConfigurationSource;
import org.sonatype.sisu.litmus.testsupport.hamcrest.DiffMatchers;

public class DefaultApplicationConfigurationUpgraderTest
    extends NexusTestSupport
{
    protected ApplicationConfigurationUpgrader configurationUpgrader;

    private FileSecurityConfigurationSource securitySource;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        this.configurationUpgrader = lookup( ApplicationConfigurationUpgrader.class );
        this.securitySource = (FileSecurityConfigurationSource) this.lookup( SecurityConfigurationSource.class, "file" );
    }

    protected void resultIsFine( String path, Configuration configuration )
        throws Exception
    {
        NexusConfigurationXpp3Writer w = new NexusConfigurationXpp3Writer();

        StringWriter sw = new StringWriter();

        w.write( sw, configuration );

        String shouldBe = IOUtil.toString( getClass().getResourceAsStream( path + ".result" ) );

        assertThat( sw.toString(), DiffMatchers.equalToOnlyDiffs( shouldBe ) );

//        shouldBe = shouldBe.replace( "\r", "" );
//
//        if ( !StringUtils.equals( shouldBe, sw.toString() ) )
//        {
//            // write the file out so we can have something to compare
//
//            File expected = FileUtils.toFile( getClass().getResource( path + ".result" ) );
//            File actual = new File( "target", expected.getName().replaceFirst( "result", "actual" ) );
//            FileOutputStream out = new FileOutputStream( actual );
//            try
//            {
//                IOUtil.copy( sw.toString(), out );
//            }
//            finally
//            {
//                IOUtil.close( out );
//            }
//            String diffMessage = "diff " + expected.getAbsolutePath() + " " + actual.getAbsolutePath();
//            String message = "Files differ, you can manually diff them:\n" + diffMessage;
//
//            // the method makes the error pretty, so we can keep it.
//            assertEquals( message, shouldBe, sw.toString().replace( "\r", "" ) );
//        }
    }

    protected void securityResultIsFine( String path )
        throws Exception
    {

        String shouldBe = IOUtil.toString( getClass().getResourceAsStream( path ) ).replace( "\r", "" );

        // we can only compare the string with no encrypted passwords, because the encryption is different every time.
        SecurityConfiguration securityConfig = this.securitySource.loadConfiguration();
        SecurityConfigurationXpp3Writer writer = new SecurityConfigurationXpp3Writer();
        StringWriter stringWriter = new StringWriter();
        writer.write( stringWriter, securityConfig );
        String actual = stringWriter.toString().replace( "\r", "" );

        if ( !StringUtils.equals( shouldBe, actual ) )
        {
            // the method makes the error pretty, so we can keep it.
            assertEquals( shouldBe, actual );
        }
    }

    @Test
    public void testFromDEC()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-001-1.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        assertEquals( 7 + 2, configuration.getRepositories().size() );

        assertEquals( 2, configuration.getRepositoryGrouping().getPathMappings().size() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-001-1.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-001-1.xml" );
    }

    @Test
    public void testFromDECDmz()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-001-2.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        // 11 repos, 2 groups
        assertEquals( 11 + 2, configuration.getRepositories().size() );

        assertEquals( 3, configuration.getRepositoryGrouping().getPathMappings().size() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-001-2.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-001-2.xml" );
    }

    @Test
    public void testFromDECInt()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-001-3.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        assertEquals( 7 + 2, configuration.getRepositories().size() );

        assertEquals( 2, configuration.getRepositoryGrouping().getPathMappings().size() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-001-3.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-001-3.xml" );
    }

    @Test
    public void testFrom100()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-100.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        // 7 repos and 2 groups
        assertEquals( 7 + 2, configuration.getRepositories().size() );

        assertEquals( 2, configuration.getRepositoryGrouping().getPathMappings().size() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-100.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-100.xml" );
    }

    @Test
    public void testFrom101()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-101.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        // 15 repos, 5 groups, 2 shadows
        assertEquals( 15 + 5 + 2, configuration.getRepositories().size() );

        assertEquals( 4, configuration.getRepositoryGrouping().getPathMappings().size() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-101.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-101.xml" );
    }

    @Test
    public void testFrom103_1()
        throws Exception
    {
        TimeZone defaultTZ = TimeZone.getDefault();

        // use UTC for this test
        TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );

        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/103-1/nexus-103.xml",
            getNexusConfiguration() );

        // trick: copying by nexus.xml the tasks.xml too
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/103-1/tasks.xml", new File( new File(
            getNexusConfiguration() ).getParentFile(), "tasks.xml" ) );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        // set back to the default timezone
        TimeZone.setDefault( defaultTZ );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        // 6 repos 3 groups
        assertEquals( 6 + 3, configuration.getRepositories().size() );

        assertEquals( 2, configuration.getRepositoryGrouping().getPathMappings().size() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/103-1/nexus-103.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/103-1/security-configuration-103.xml" );
    }

    @Test
    public void testFrom103_2()
        throws Exception
    {
        // same as above, but we have no tasks.xml
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/103-2/nexus-103.xml",
            getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        // 6 repos, 1 shadow, 2 groups
        assertEquals( 6 + 1 + 2, configuration.getRepositories().size() );

        assertEquals( 2, configuration.getRepositoryGrouping().getPathMappings().size() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/103-2/nexus-103.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/103-2/security-configuration-103.xml" );
    }

    @Test
    public void testFrom104()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-104.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-104.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-104.xml" );
    }

    @Test
    public void testFrom105()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-105.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-105.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-105.xml" );
    }

    @Test
    public void testNEXUS1710()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus1710/nexus.xml",
            getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus1710/nexus.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus1710/security-configuration-1710.xml" );
    }

    @Test
    public void testLookup()
        throws Exception
    {
        // this has slf4f deps and plexus might skip it
        this.lookup( SingleVersionUpgrader.class, "1.0.8" );
    }

    // public void testUpgradeStaticConfig()
    // throws Exception
    // {
    // copyFromClasspathToFile( "/META-INF/nexus/nexus.xml", getNexusConfiguration() );
    //
    // Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );
    //
    // Assert.assertNotNull( configuration );
    //
    // NexusConfigurationXpp3Writer w = new NexusConfigurationXpp3Writer();
    //
    // StringWriter sw = new StringWriter();
    //
    // w.write( sw, configuration );
    //
    // File actual = new File( "target", "upgraded-nexus.xml" );
    // FileOutputStream out = new FileOutputStream( actual );
    // try
    // {
    // IOUtil.copy( sw.toString(), out );
    // }
    // finally
    // {
    // IOUtil.close( out );
    // }
    //
    // }

    @Test
    public void testMirrorsFrom108()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-108-with-mirrors.xml",
            getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-108-with-mirrors.xml", configuration );
    }

    @Test
    public void testFrom108()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-108.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-108.xml", configuration );
        securityResultIsFine( "/org/sonatype/nexus/configuration/upgrade/security-configuration-108.xml" );
    }

    @Test
    public void testFrom142()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-142.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-142.xml", configuration );
    }

    @Test
    public void testFrom143()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-143.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertEquals( Configuration.MODEL_VERSION, configuration.getVersion() );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-143.xml", configuration );
    }

    @Test
    public void testFrom220()
        throws Exception
    {
        copyFromClasspathToFile( "/org/sonatype/nexus/configuration/upgrade/nexus-220.xml", getNexusConfiguration() );

        Configuration configuration = configurationUpgrader.loadOldConfiguration( new File( getNexusConfiguration() ) );

        assertThat( configuration.getVersion(), is( Configuration.MODEL_VERSION ) );

        resultIsFine( "/org/sonatype/nexus/configuration/upgrade/nexus-220.xml", configuration );
    }

}
