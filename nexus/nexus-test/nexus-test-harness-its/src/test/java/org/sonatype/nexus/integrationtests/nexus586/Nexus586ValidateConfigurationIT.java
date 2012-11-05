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
package org.sonatype.nexus.integrationtests.nexus586;

import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Saving the Nexus config needs to validate the anonymous user information 
 */
public class Nexus586ValidateConfigurationIT
    extends AbstractNexusIntegrationTest
{

    @BeforeClass
    public void setSecureTest(){
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @Test
    public void wrongAnonymousAccount()
        throws Exception
    {
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        GlobalConfigurationResource globalConfig = SettingsMessageUtil.getCurrentSettings();
        globalConfig.setSecurityAnonymousUsername( "zigfrid" );

        Status status = SettingsMessageUtil.save( globalConfig );
        Assert.assertEquals( status.getCode(), 400, "Can't set an invalid user as anonymous" );
    }

}
