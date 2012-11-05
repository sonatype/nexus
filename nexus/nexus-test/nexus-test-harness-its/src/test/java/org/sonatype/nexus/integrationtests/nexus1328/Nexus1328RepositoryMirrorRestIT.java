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
package org.sonatype.nexus.integrationtests.nexus1328;

import java.io.IOException;

import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListRequest;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.nexus.rest.model.MirrorStatusResource;
import org.sonatype.nexus.rest.model.MirrorStatusResourceListResponse;
import org.sonatype.nexus.test.utils.MirrorMessageUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Nexus1328RepositoryMirrorRestIT
    extends AbstractNexusIntegrationTest
{
    protected MirrorMessageUtils messageUtil;

    private String repositoryId = "release-proxy-repo-1";

    public Nexus1328RepositoryMirrorRestIT()
    {
        this.messageUtil = new MirrorMessageUtils( this.getJsonXStream(), MediaType.APPLICATION_JSON );
    }

    @Test
    public void setMirrorTest()
        throws IOException
    {
        MirrorResourceListRequest request = new MirrorResourceListRequest();

        MirrorResource resource = new MirrorResource();
        resource.setUrl( "http://setMirrorTest1" );
        request.addData( resource );

        resource = new MirrorResource();
        resource.setUrl( "http://setMirrorTest2" );
        request.addData( resource );

        this.messageUtil.setMirrors( repositoryId, request );
    }

    @Test
    public void updateMirrorTest()
        throws IOException
    {
        MirrorResourceListRequest request = new MirrorResourceListRequest();

        MirrorResource resource = new MirrorResource();
        resource.setUrl( "http://updateMirrorTest1" );
        request.addData( resource );

        resource = new MirrorResource();
        resource.setUrl( "http://updateMirrorTest2" );
        request.addData( resource );

        MirrorResourceListResponse response = this.messageUtil.setMirrors( repositoryId, request );

        request.setData( response.getData() );

        resource = new MirrorResource();
        resource.setUrl( "http://updateMirrorTest3" );
        request.addData( resource );

        ( request.getData().iterator().next() ).setUrl( "http://updateMirrorTest4" );

        response = this.messageUtil.setMirrors( repositoryId, request );

        MirrorResource one = response.getData().get( 0 );
        MirrorResource two = response.getData().get( 1 );
        MirrorResource three = response.getData().get( 2 );

        Assert.assertEquals( "http://updateMirrorTest4", one.getUrl() );
        Assert.assertEquals( "http://updateMirrorTest2", two.getUrl() );
        Assert.assertEquals( "http://updateMirrorTest3", three.getUrl() );
    }

    @Test
    public void getMirrorTest()
        throws IOException
    {
        MirrorResourceListRequest request = new MirrorResourceListRequest();

        MirrorResource resource = new MirrorResource();
        resource.setUrl( "http://getMirrorTest1" );
        request.addData( resource );

        resource = new MirrorResource();
        resource.setUrl( "http://getMirrorTest2" );
        request.addData( resource );

        this.messageUtil.setMirrors( repositoryId, request );

        MirrorResourceListResponse response = this.messageUtil.getMirrors( repositoryId );

        MirrorResource one = response.getData().get( 0 );
        MirrorResource two = response.getData().get( 1 );

        Assert.assertEquals( "http://getMirrorTest1", one.getUrl() );
        Assert.assertEquals( "http://getMirrorTest2", two.getUrl() );
    }

    @Test
    public void mirrorStatusTest()
        throws IOException
    {
        MirrorResourceListRequest request = new MirrorResourceListRequest();

        MirrorResource resource = new MirrorResource();
        resource.setUrl( "http://mirrorStatusTest1" );
        request.addData( resource );

        resource = new MirrorResource();
        resource.setUrl( "http://mirrorStatusTest2" );
        request.addData( resource );

        this.messageUtil.setMirrors( repositoryId, request );

        MirrorStatusResourceListResponse response = this.messageUtil.getMirrorsStatus( repositoryId );

        MirrorStatusResource one = response.getData().get( 0 );
        MirrorStatusResource two = response.getData().get( 1 );

        Assert.assertEquals( "http://mirrorStatusTest1", one.getUrl() );
        Assert.assertEquals( "http://mirrorStatusTest2", two.getUrl() );
        Assert.assertEquals( "Available", one.getStatus() );
        Assert.assertEquals( "Available", two.getStatus() );
    }

    @Test
    public void predefinedMirrorTest()
        throws IOException
    {
        this.messageUtil.getPredefinedMirrors( repositoryId );
    }
}
