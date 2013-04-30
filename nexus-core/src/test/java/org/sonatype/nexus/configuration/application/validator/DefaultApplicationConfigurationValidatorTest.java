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
package org.sonatype.nexus.configuration.application.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Reader;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Writer;
import org.sonatype.nexus.configuration.validator.ApplicationConfigurationValidator;
import org.sonatype.nexus.configuration.validator.ApplicationValidationContext;
import org.sonatype.nexus.configuration.validator.DefaultApplicationConfigurationValidator;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.test.NexusTestSupport;
import org.sonatype.nexus.util.ExternalConfigUtil;

public class DefaultApplicationConfigurationValidatorTest
    extends NexusTestSupport
{

    protected DefaultApplicationConfigurationValidator underTest;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        this.underTest = new DefaultApplicationConfigurationValidator();
    }

    protected Configuration getConfigurationFromStream( InputStream is )
        throws Exception
    {
        NexusConfigurationXpp3Reader reader = new NexusConfigurationXpp3Reader();

        Reader fr = new InputStreamReader( is );

        return reader.read( fr );
    }

    protected Configuration loadNexusConfig( File configFile )
        throws Exception
    {
        NexusConfigurationXpp3Reader reader = new NexusConfigurationXpp3Reader();

        Reader fr = new FileReader( configFile );
        try
        {
            return reader.read( fr );
        }
        finally
        {
            IOUtil.close( fr );
        }

    }

    protected void saveConfiguration( Configuration config, String pathToConfig )
        throws IOException
    {
        NexusConfigurationXpp3Writer writer = new NexusConfigurationXpp3Writer();

        Writer fw = null;
        try
        {
            fw = new FileWriter( pathToConfig );

            writer.write( fw, config );
        }
        finally
        {
            IOUtil.close( fw );
        }
    }

    @Test
    public void testBad1()
        throws Exception
    {
        // get start with the default config
        this.copyDefaultConfigToPlace();
        Configuration config = this.loadNexusConfig( new File( this.getNexusConfiguration() ) );

        // make it bad

        // remove the name from a repository
        CRepository missingNameRepo = (CRepository) config.getRepositories().get( 0 );
        missingNameRepo.setName( null );

        // TDOD add 2 more warnings

        // wrong shadow type
        CRepository badShadow = new DefaultCRepository();
        badShadow.setId( "badShadow" );
        badShadow.setName( "Does not follow" );
        badShadow.setProviderRole( ShadowRepository.class.getName() );
        badShadow.setProviderHint( "m2-m1-shadow" );
        // Manipulate the dom
        Xpp3Dom externalConfig = new Xpp3Dom( "externalConfiguration" );
        badShadow.setExternalConfiguration( externalConfig );
        ExternalConfigUtil.setNodeValue( externalConfig, "masterRepositoryId", "non-existent" );
        config.addRepository( badShadow );

        // now validate it
        ValidationResponse response = underTest.validateModel( new ValidationRequest( config ) );

        assertThat( response.getValidationWarnings(), hasSize( 1 ) );
        assertThat( response.getValidationErrors(), hasSize( 0 ) );

        // codehaus-snapshots has no name, it will be defaulted
        assertThat( response.isModified(), is( true ) );

        assertThat( response.isValid(), is( true ) );
    }

    @Test
    public void testBad2()
        throws Exception
    {
        // get start with the default config
        this.copyDefaultConfigToPlace();
        this.copyResource( "/META-INF/nexus/default-oss-nexus.xml", getNexusConfiguration() );
        Configuration config = this.loadNexusConfig( new File( this.getNexusConfiguration() ) );

        // make it bad

        // invalid policy
        CRepository invalidPolicyRepo = config.getRepositories().get( 0 );
        Xpp3Dom externalConfig = (Xpp3Dom) invalidPolicyRepo.getExternalConfiguration();
        ExternalConfigUtil.setNodeValue( externalConfig, "repositoryPolicy", "badPolicy" );

        // duplicate the repository id
        for ( CRepository repo : config.getRepositories() )
        {
            if ( !repo.getId().equals( "central" ) )
            {
                // duplicate
                repo.setId( "central" );
                break;
            }
        }

        // TODO: add more errors here

        // now validate it
        ValidationResponse response = underTest.validateModel( new ValidationRequest( config ) );

        assertThat( response.isValid(), is( false ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( greaterThan( 0 ) ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
    }

    @Test
    public void testNexus1710Bad()
        throws Exception
    {

        // this one is easy because you can compare:
        // /org/sonatype/nexus/configuration/upgrade/nexus1710/nexus.xml.result-bad
        // with
        // /org/sonatype/nexus/configuration/upgrade/nexus1710/nexus.xml.result
        // and you have the diff, and you already have to manually update the good one.

        // this was before fix: groupId/repoId name clash
        ValidationResponse response = underTest.validateModel( new ValidationRequest(
            getConfigurationFromStream( getClass().getResourceAsStream(
                "/org/sonatype/nexus/configuration/upgrade/nexus1710/nexus.xml.result-bad" ) ) ) );

        assertThat( response.isValid(), is( false ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( 1 ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
    }

    @Test
    public void testNexus1710Good()
        throws Exception
    {
        // this is after fix: groupId is appended by "-group" to resolve clash
        ValidationResponse response = underTest.validateModel( new ValidationRequest(
            getConfigurationFromStream( getClass().getResourceAsStream(
                "/org/sonatype/nexus/configuration/upgrade/nexus1710/nexus.xml.result" ) ) ) );

        assertThat( response.isValid(), is( true ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( 0 ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
    }

    @Test
    public void repoEmptyId()
    {
        ApplicationValidationContext ctx = new ApplicationValidationContext();

        CRepository repo = new CRepository();
        repo.setLocalStatus( LocalStatus.IN_SERVICE.toString() );
        repo.setName( "name" );
        ValidationResponse response = underTest.validateRepository( ctx, repo );

        assertThat( response.isValid(), is( false ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( 1 ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
        assertThat( response.getValidationErrors().get( 0 ), hasKey("id") );
    }

    private Matcher<? super ValidationMessage> hasKey( final String id )
    {
        return hasProperty( "key", equalTo( "id") );
    }

    @Test
    public void repoIllegalId()
    {
        ApplicationValidationContext ctx = new ApplicationValidationContext();
        CRepository repo = new CRepository();
        repo.setId( " " );
        repo.setLocalStatus( LocalStatus.IN_SERVICE.toString() );
        repo.setName( "name" );

        ValidationResponse response = underTest.validateRepository( ctx, repo );

        assertThat( response.isValid(), is( false ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( 1 ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
        assertThat( response.getValidationErrors().get( 0 ), hasKey("id") );
    }

    @Test
    public void repoClashRepoId()
    {
        ApplicationValidationContext ctx = new ApplicationValidationContext();
        ctx.addExistingRepositoryIds();
        ctx.getExistingRepositoryIds().add( "id" );
        CRepository repo = new CRepository();
        repo.setId( "id" );
        repo.setLocalStatus( LocalStatus.IN_SERVICE.toString() );
        repo.setName( "name" );

        ValidationResponse response = underTest.validateRepository( ctx, repo );

        assertThat( response.isValid(), is( false ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( 1 ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
        assertThat( response.getValidationErrors().get( 0 ), hasKey("id") );
    }
    @Test
    public void repoClashGroupId()
    {
        ApplicationValidationContext ctx = new ApplicationValidationContext();
        ctx.addExistingRepositoryGroupIds();
        ctx.getExistingRepositoryGroupIds().add( "id" );
        CRepository repo = new CRepository();
        repo.setId( "id" );
        repo.setLocalStatus( LocalStatus.IN_SERVICE.toString() );
        repo.setName( "name" );

        ValidationResponse response = underTest.validateRepository( ctx, repo );

        assertThat( response.isValid(), is( false ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( 1 ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
        assertThat( response.getValidationErrors().get( 0 ), hasKey("id") );
    }

    @Test
    public void repoClashShadowId()
    {
        ApplicationValidationContext ctx = new ApplicationValidationContext();
        ctx.addExistingRepositoryShadowIds();
        ctx.getExistingRepositoryShadowIds().add( "id" );
        CRepository repo = new CRepository();
        repo.setId( "id" );
        repo.setLocalStatus( LocalStatus.IN_SERVICE.toString() );
        repo.setName( "name" );

        ValidationResponse response = underTest.validateRepository( ctx, repo );

        assertThat( response.isValid(), is( false ) );
        assertThat( response.isModified(), is( false ) );

        assertThat( response.getValidationErrors(), hasSize( 1 ) );
        assertThat( response.getValidationWarnings(), hasSize( 0 ) );
        assertThat( response.getValidationErrors().get( 0 ), hasKey("id") );
    }

    @Test
    public void repoEmptyName()
    {
        ApplicationValidationContext ctx = new ApplicationValidationContext();
        CRepository repo = new CRepository();
        repo.setId( "id" );
        repo.setLocalStatus( LocalStatus.IN_SERVICE.toString() );

        ValidationResponse response = underTest.validateRepository( ctx, repo );

        assertThat( response.isValid(), is( true ) );
        assertThat( response.isModified(), is( true ) );

        assertThat( response.getValidationErrors(), hasSize( 0 ) );
        assertThat( response.getValidationWarnings(), hasSize( 1 ) );
        ValidationMessage actual = response.getValidationWarnings().get( 0 );
        assertThat( actual, hasKey("id") );
    }
}
