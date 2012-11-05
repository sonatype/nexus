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
package org.sonatype.nexus.integrationtests.nexus643;

import java.io.File;
import java.io.IOException;

import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EmptyTrashTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests empty trash task.
 */
public class Nexus643EmptyTrashTaskIT
    extends AbstractNexusIntegrationTest
{
    @Test
    public void emptyTrashTask()
        throws Exception
    {

        delete( "nexus643" );

        File trashContent = new File( nexusWorkDir, "storage/nexus-test-harness-repo/.nexus/trash/nexus643" );
        Assert.assertTrue( trashContent.exists(), "Something should be at trash!" );

        // Empty trash content older than 1 days
        File oldTrashFile =
            new File( nexusWorkDir,
                "storage/nexus-test-harness-repo/.nexus/trash/nexus643/artifact-1/1.0.0/artifact-1-1.0.0.pom" );
        File newTrashFile =
            new File( nexusWorkDir,
                "storage/nexus-test-harness-repo/.nexus/trash/nexus643/artifact-1/1.0.0/artifact-1-1.0.0.jar" );
        oldTrashFile.setLastModified( System.currentTimeMillis() - 24L * 60L * 60L * 1000L * 2 );

        Assert.assertTrue( newTrashFile.exists(), "New trash content should be kept! " );
        Assert.assertTrue( oldTrashFile.exists(), "Old trash content should be kept!" );

        // this is unsupported, disabled for now (UI is not using it either)
        ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
        age.setKey( EmptyTrashTaskDescriptor.OLDER_THAN_FIELD_ID );
        age.setValue( "1" );

        TaskScheduleUtil.runTask( "Empty Trash Older Than", EmptyTrashTaskDescriptor.ID, age );

        Assert.assertTrue( newTrashFile.exists(), "New trash content should be kept! " );
        Assert.assertFalse( oldTrashFile.exists(), "Old trash content should be removed!" );

        // Empty the whole trash
        TaskScheduleUtil.runTask( "Empty Whole Trash", EmptyTrashTaskDescriptor.ID );

        Assert.assertFalse( trashContent.exists(), "Trash should be empty!" );
    }

    private void delete( String groupId )
        throws IOException
    {
        String serviceURI = "service/local/repositories/nexus-test-harness-repo/content/" + groupId + "/";
        Response response = RequestFacade.sendMessage( serviceURI, Method.DELETE );
        Assert.assertTrue( response.getStatus().isSuccess(), "Unable to delete nexus643 artifacts" );
    }

}
