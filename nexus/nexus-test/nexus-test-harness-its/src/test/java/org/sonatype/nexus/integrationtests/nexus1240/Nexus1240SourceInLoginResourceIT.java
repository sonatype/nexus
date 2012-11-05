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
package org.sonatype.nexus.integrationtests.nexus1240;

import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.AuthenticationClientPermissions;
import org.sonatype.security.rest.model.AuthenticationLoginResource;
import org.sonatype.security.rest.model.AuthenticationLoginResourceResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Nexus1240SourceInLoginResourceIT
    extends AbstractNexusIntegrationTest
{
    @BeforeClass
    public void setSecureTest(){
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @Test
    public void sourceInLoginResourceTest() throws IOException
    {     
        AuthenticationClientPermissions clientPermissions = this.getPermissions();
        
        Assert.assertEquals( "default", clientPermissions.getLoggedInUserSource() );
    }

    private AuthenticationClientPermissions getPermissions()
        throws IOException
    {
        Response response = RequestFacade
            .sendMessage( RequestFacade.SERVICE_LOCAL + "authentication/login", Method.GET );

        Assert.assertTrue( response.getStatus().isSuccess(), "Status: "+ response.getStatus()  );
        
        String responseText = response.getEntity().getText();

        XStreamRepresentation representation = new XStreamRepresentation(
            XStreamFactory.getXmlXStream(),
            responseText,
            MediaType.APPLICATION_XML );

        AuthenticationLoginResourceResponse resourceResponse = (AuthenticationLoginResourceResponse) representation
            .getPayload( new AuthenticationLoginResourceResponse() );

        AuthenticationLoginResource resource = resourceResponse.getData();

        return resource.getClientPermissions();
    }

}
