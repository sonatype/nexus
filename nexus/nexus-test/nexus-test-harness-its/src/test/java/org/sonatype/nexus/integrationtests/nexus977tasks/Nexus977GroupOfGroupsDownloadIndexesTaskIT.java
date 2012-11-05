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
package org.sonatype.nexus.integrationtests.nexus977tasks;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.DownloadIndexesTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Nexus977GroupOfGroupsDownloadIndexesTaskIT
    extends AbstractNexusProxyIntegrationTest
{

    @Test
    public void downloadIndexes()
        throws Exception
    {
        Assert.assertTrue( getSearchMessageUtil().searchForGav( getTestId(), "project", "0.8" ).isEmpty() );
        Assert.assertTrue( getSearchMessageUtil().searchForGav( getTestId(), "project", "2.1" ).isEmpty() );

        ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
        repo.setKey( "repositoryId" );
        repo.setValue( "g4" );
        TaskScheduleUtil.runTask( "DownloadIndexesTaskDescriptor-snapshot", DownloadIndexesTaskDescriptor.ID, repo );
        
        Assert.assertFalse( getSearchMessageUtil().searchForGav( getTestId(), "project", "0.8" ).isEmpty() );
        Assert.assertFalse( getSearchMessageUtil().searchForGav( getTestId(), "project", "2.1" ).isEmpty() );
    }

}
