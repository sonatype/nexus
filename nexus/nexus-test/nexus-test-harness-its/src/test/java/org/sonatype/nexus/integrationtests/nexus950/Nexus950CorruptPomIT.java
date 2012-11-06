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
package org.sonatype.nexus.integrationtests.nexus950;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Nexus950CorruptPomIT
    extends AbstractNexusIntegrationTest
{

    @Test
    public void uploadCorruptPomTest() throws HttpException, IOException
    {
        
        File jarFile = this.getTestFile( "bad-pom.jar" );
        File badPomFile = this.getTestFile( "pom.xml" );
        
        HttpMethod resultMethod = getDeployUtils().deployUsingPomWithRestReturnResult( this.getTestRepositoryId(), jarFile, badPomFile, "", "jar" );
        
        Assert.assertEquals( 400, resultMethod.getStatusCode(), "Expected a 400 error returned." );        
    }
    
}
