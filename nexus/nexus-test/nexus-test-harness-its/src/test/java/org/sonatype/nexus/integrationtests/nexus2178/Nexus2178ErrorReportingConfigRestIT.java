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
package org.sonatype.nexus.integrationtests.nexus2178;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.ErrorReportingSettings;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Nexus2178ErrorReportingConfigRestIT
    extends AbstractNexusIntegrationTest
{
    @Test
    public void validationConfiguration()
        throws Exception
    {
        // Default config
        GlobalConfigurationResource resource = SettingsMessageUtil.getCurrentSettings();

        Assert.assertFalse( resource.getErrorReportingSettings().isReportErrorsAutomatically(),
                            "Error reporting should be null by default" );

        // Set some values
        ErrorReportingSettings settings = resource.getErrorReportingSettings();
        settings.setJiraUsername( "someusername" );
        settings.setJiraPassword( "somepassword" );
        settings.setReportErrorsAutomatically( true );

        SettingsMessageUtil.save( resource );

        resource = SettingsMessageUtil.getCurrentSettings();

        Assert.assertNotNull( resource.getErrorReportingSettings(), "Error reporting should not be null" );
        Assert.assertEquals( "someusername", resource.getErrorReportingSettings().getJiraUsername() );
        Assert.assertEquals( AbstractNexusPlexusResource.PASSWORD_PLACE_HOLDER,
                             resource.getErrorReportingSettings().getJiraPassword() );

        // Clear them again
        resource.setErrorReportingSettings( null );

        Assert.assertTrue( SettingsMessageUtil.save( resource ).isSuccess() );

        resource = SettingsMessageUtil.getCurrentSettings();

        Assert.assertFalse( resource.getErrorReportingSettings().isReportErrorsAutomatically(),
                            "Error reporting should be null" );
    }
}
