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
package org.sonatype.nexus.integrationtests.nexus4635same;

import static org.testng.Assert.assertFalse;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.StatusResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * subsequent start of same instance<br>
 * https://issues.sonatype.org/browse/NEXUS-4635
 * 
 * <pre>
 * <firstStart>false</firstStart>
 * <instanceUpgraded>false</instanceUpgraded>
 * <configurationUpgraded>false</configurationUpgraded>
 * </pre>
 */
public class Nexus4635SubsequentStartIT
    extends AbstractNexusIntegrationTest
{
    @BeforeClass
    protected void disableSecurity()
    {
        TestContainer.getInstance().getTestContext().setSecureTest( false );
    }

    @Test
    public void checkState()
        throws Exception
    {
        // initial nexus start upgraded config, so we just bounce it
        stopNexus();
        startNexus();

        StatusResource status = getNexusStatusUtil().getNexusStatus().getData();
        assertFalse( status.isFirstStart() );
        assertFalse( status.isInstanceUpgraded() );
        assertFalse( status.isConfigurationUpgraded() );
    }
}
