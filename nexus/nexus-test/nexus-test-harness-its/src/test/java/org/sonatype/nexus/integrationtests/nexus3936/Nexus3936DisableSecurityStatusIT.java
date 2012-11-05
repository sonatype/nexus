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
package org.sonatype.nexus.integrationtests.nexus3936;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.StatusResource;
import org.sonatype.nexus.test.utils.NexusStatusUtil;
import org.sonatype.security.rest.model.ClientPermission;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Security is already disabled for this Test, we just need to make sure the Status resource returns ALL/15, for all the
 * permission strings.
 */
@Test( groups = { "security", "anonymous", "status" } )
public class Nexus3936DisableSecurityStatusIT
    extends AbstractNexusIntegrationTest
{

    @Test
    public void testSecurityDisabledStatus()
        throws Exception
    {

        NexusStatusUtil statusUtil = getNexusStatusUtil();
        StatusResource statusResource = statusUtil.getNexusStatus().getData();

        List<ClientPermission> permisisons = statusResource.getClientPermissions().getPermissions();

        Assert.assertTrue( permisisons.size() > 0, "Permissions are empty, expected a whole bunch, not zero." );
        for ( ClientPermission clientPermission : permisisons )
        {
            Assert.assertEquals( clientPermission.getValue(), 15, "Permission '"+ clientPermission.getId() +"' should have had a value of '15', the value was" + clientPermission.getValue() );
        }
        // that is it, just checking the values, when security is disabled, access is WIDE open.
    }
}
