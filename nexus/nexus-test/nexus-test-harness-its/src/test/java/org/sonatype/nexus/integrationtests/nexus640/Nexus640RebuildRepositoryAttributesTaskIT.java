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
package org.sonatype.nexus.integrationtests.nexus640;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.RebuildAttributesTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the rebuild repository attributes task.
 */
public class Nexus640RebuildRepositoryAttributesTaskIT
    extends AbstractNexusIntegrationTest
{

    @Test
    public void rebuildAttributes()
        throws Exception
    {
        String attributePath = "storage/"+REPO_TEST_HARNESS_REPO+"/.nexus/attributes/nexus640/artifact/1.0.0/";

        ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
        repo.setKey( "repositoryId" );
        repo.setValue( REPO_TEST_HARNESS_REPO );
        TaskScheduleUtil.runTask( RebuildAttributesTaskDescriptor.ID, repo );

        File jar = new File( nexusWorkDir, attributePath + "artifact-1.0.0.jar" );
        Assert.assertTrue( jar.exists(), "Attribute files should be generated after rebuild" );
        File pom = new File( nexusWorkDir, attributePath + "artifact-1.0.0.pom" );
        Assert.assertTrue( pom.exists(), "Attribute files should be generated after rebuild" );

    }

}
