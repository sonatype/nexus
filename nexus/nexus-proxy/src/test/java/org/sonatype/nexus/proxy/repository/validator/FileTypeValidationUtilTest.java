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
package org.sonatype.nexus.proxy.repository.validator;

import java.io.File;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractNexusTestCase;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.maven.MavenFileTypeValidator;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.fs.FileContentLocator;

/**
 * This test actually tests MavenFileTypeValidator. Also, it uses non-LAX XML validation! This test would pass in LAX
 * XML validation too, except one assertion, see testPom() for why's.
 */
public class FileTypeValidationUtilTest
    extends AbstractNexusTestCase
{
    private FileTypeValidatorHub getValidationUtil()
        throws Exception
    {
        return lookup( FileTypeValidatorHub.class );
    }

    @Test
    public void testJar()
        throws Exception
    {
        doTest( "something/else/myapp.jar", "test.jar", true );
        doTest( "something/else/myapp.zip", "test.jar", true );
        doTest( "something/else/myapp.war", "test.jar", true );
        doTest( "something/else/myapp.ear", "test.jar", true );
        doTest( "something/else/myapp.jar", "error.html", false );
    }

    @Test
    public void testPom()
        throws Exception
    {
        doTest( "something/else/myapp.pom", "no-doctype-pom.xml", true );
        doTest( "something/else/myapp.pom", "simple.xml", false );
        doTest( "something/else/myapp.pom", "pom.xml", true );
        doTest( "something/else/myapp.xml", "pom.xml", true );
        doTest( "something/else/myapp.xml", "simple.xml", true );
        // Note: this last line makes this test fail for LAX XML validation
        // In LAX mode, we do not expect from XML to have preamble but do expect it to be "plain text"
        // Obviously, the HTML used for testing passes here.
        // The point is, that the path "myapp.xml" is not a POM path despite this test
        // is named as such, hence, this is actually XML validation test.
        // And since we agreed to lax XML validation (many XMLs lacks the preamble)
        // we cannot decide this below, unless LAX XML validation is OFF
        doTest( "something/else/myapp.xml", "error.html", false );
    }

    @Test
    public void testNonHandled()
        throws Exception
    {
        doTest( "something/else/image.jpg", "no-doctype-pom.xml", true );
        doTest( "something/else/image.avi", "no-doctype-pom.xml", true );
    }

    @Test
    public void testFlexArtifacts()
        throws Exception
    {
        doTest( "something/else/library.swc", "no-doctype-pom.xml", false );
        doTest( "something/else/library.swc", "test.swc", true );
        doTest( "something/else/app.swf", "no-doctype-pom.xml", false );
        doTest( "something/else/app.swf", "test.swf", true );
    }

    @Test
    public void testTar()
        throws Exception
    {
        doTest( "something/else/bundle.tar", "no-doctype-pom.xml", false );
        doTest( "something/else/bundle.tar", "test.tar", true );
    }

    @Test
    public void testTarGz()
        throws Exception
    {
        doTest( "something/else/bundle.tar.gz", "no-doctype-pom.xml", false );
        doTest( "something/else/bundle.tar.gz", "test.tar.gz", true );
        doTest( "something/else/bundle.tgz", "no-doctype-pom.xml", false );
        doTest( "something/else/bundle.tgz", "test.tgz", true );
        doTest( "something/else/bundle.gz", "no-doctype-pom.xml", false );
        doTest( "something/else/bundle.gz", "test.gz", true );
    }

    @Test
    public void testTarBz2()
        throws Exception
    {
        doTest( "something/else/bundle.tar.bz2", "no-doctype-pom.xml", false );
        doTest( "something/else/bundle.tar.bz2", "test.tar.bz2", true );
        doTest( "something/else/bundle.tbz", "no-doctype-pom.xml", false );
        doTest( "something/else/bundle.tbz", "test.tbz", true );
        doTest( "something/else/bundle.bz2", "no-doctype-pom.xml", false );
        doTest( "something/else/bundle.bz2", "test.bz2", true );
    }

    @Test
    public void testChecksum()
        throws Exception
    {
        doTest( "something/else/file.jar.sha1", "no-doctype-pom.xml", false );
        doTest( "something/else/file.jar.sha1", "test.md5", false );
        doTest( "something/else/file.jar.sha1", "test.sha1", true );
        doTest( "something/else/file.jar.md5", "no-doctype-pom.xml", false );
        doTest( "something/else/file.jar.md5", "test.sha1", false );
        doTest( "something/else/file.jar.md5", "test.md5", true );
    }

    // ==

    protected Repository getDummyRepository()
        throws Exception
    {
        Repository repository = lookup( Repository.class, "maven2" );

        CRepository cRepo = new DefaultCRepository();
        cRepo.setId( "test-repo" );
        cRepo.setLocalStatus( LocalStatus.IN_SERVICE.toString() );
        cRepo.setNotFoundCacheTTL( 1 );
        cRepo.setLocalStorage( new CLocalStorage() );
        cRepo.getLocalStorage().setProvider( "file" );
        cRepo.setProviderRole( Repository.class.getName() );
        cRepo.setProviderHint( "maven2" );

        Xpp3Dom ex = new Xpp3Dom( "externalConfiguration" );
        cRepo.setExternalConfiguration( ex );
        M2RepositoryConfiguration extConf = new M2RepositoryConfiguration( ex );
        extConf.setRepositoryPolicy( RepositoryPolicy.RELEASE );

        repository.configure( cRepo );

        return repository;
    }

    private void doTest( String expectedFileName, String testFileName, boolean expectedResult )
        throws Exception
    {
        File testFile = new File( "target/test-classes/FileTypeValidationUtilTest", testFileName );

        DefaultStorageFileItem file =
            new DefaultStorageFileItem( getDummyRepository(), new ResourceStoreRequest( expectedFileName ), true, true,
                new FileContentLocator( testFile, "this-is-neglected-in-this-test" ) );
        file.getItemContext().put( MavenFileTypeValidator.XML_DETECTION_LAX_KEY, "false" );

        boolean result = getValidationUtil().isExpectedFileType( file );

        Assert.assertEquals( "File name: " + expectedFileName + " and file: " + testFileName + " match result: "
            + result + " expected: " + expectedResult, expectedResult, result );
    }

    //

}
