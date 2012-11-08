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
package org.sonatype.nexus.proxy.maven;

import org.junit.Test;
import org.sonatype.nexus.proxy.repository.validator.AbstractFileTypeValidationUtilTest;

/**
 * Tests for MavenFileTypeValidator specific file types.
 */
public class MavenFileTypeValidatorTest
    extends AbstractFileTypeValidationUtilTest
{
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
    }

    @Test
    public void testXml()
        throws Exception
    {
        doTest( "something/else/myapp.xml", "pom.xml", true );
        doTest( "something/else/myapp.xml", "pom.xml", true, true );
        doTest( "something/else/myapp.xml", "simple.xml", true );
        doTest( "something/else/myapp.xml", "simple.xml", true, true );
        // will be INVALID with XML Lax validation OFF
        doTest( "something/else/myapp.xml", "error.html", false, false );
        // will be VALID with XML Lax validation ON
        doTest( "something/else/myapp.xml", "error.html", true, true );
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

}
