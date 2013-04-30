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
package org.sonatype.nexus.mime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Properties;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

/**
 * Tests for {@link NexusMimeTypes}
 */
public class NexusMimeTypesTest
    extends TestSupport
{

    NexusMimeTypes underTest = new NexusMimeTypes();

    private Properties addMimeType( final Properties properties, final String extension, final String... types )
    {
        properties.setProperty( extension, Joiner.on( "," ).join( types ) );
        return properties;
    }

    @Test
    public void unconfigured()
    {
        assertThat( underTest.getMimeTypes( "test" ), is( nullValue() ) );
    }

    @Test
    public void addMimeType()
    {
        underTest.initMimeTypes( addMimeType( new Properties(), "test", "application/octet-stream" ) );
        assertThat( underTest.getMimeTypes( "test" ), is( notNullValue() ) );
        assertThat( underTest.getMimeTypes( "test" ), hasProperty( "extension", is( "test" ) ) );
        assertThat( underTest.getMimeTypes( "test" ), hasProperty( "override", is( false ) ) );
        assertThat( underTest.getMimeTypes( "test" ).getMimetypes(), contains( "application/octet-stream" ));
    }

    @Test
    public void overrideMimeType()
    {
        Properties properties = new Properties();
        underTest.initMimeTypes( addMimeType( properties, "override.test", "application/octet-stream" ) );
        assertThat( underTest.getMimeTypes( "test" ), is( notNullValue() ) );
        assertThat( underTest.getMimeTypes( "test" ), hasProperty( "extension", is( "test" ) ) );
        assertThat( underTest.getMimeTypes( "test" ), hasProperty( "override", is( true ) ) );
        assertThat( underTest.getMimeTypes( "test" ).getMimetypes(), contains( "application/octet-stream" ));
    }

    @Test
    public void mergeOverrideAndAdditional()
    {
        Properties types = new Properties();

        addMimeType( types, "override.test", "application/octet-stream" );
        addMimeType( types, "test", "text/plain" );

        underTest.initMimeTypes( types );
        assertThat( underTest.getMimeTypes( "test" ), is( notNullValue() ) );
        assertThat( underTest.getMimeTypes( "test" ), hasProperty( "extension", is( "test" ) ) );
        assertThat( underTest.getMimeTypes( "test" ), hasProperty( "override", is( true ) ) );
        assertThat( underTest.getMimeTypes( "test" ).getMimetypes(), hasItems( "application/octet-stream",
                                                                               "text/plain" ));
    }

}
