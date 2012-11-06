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
package org.sonatype.nexus.integrationtests.nexus1961;

import java.util.List;

import org.apache.maven.index.treeview.TreeNode;
import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeNode;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeViewResponseDTO;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.thoughtworks.xstream.XStream;

public class Nexus1961IndexContentIT
    extends AbstractNexusIntegrationTest
{

    @Override
    protected void runOnce()
        throws Exception
    {
        super.runOnce();

        RepositoryMessageUtil.updateIndexes( REPO_TEST_HARNESS_REPO );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void getIndexContent()
        throws Exception
    {
        String serviceURI = "service/local/repositories/" + REPO_TEST_HARNESS_REPO + "/index_content/";

        String responseText = RequestFacade.doGetForText( serviceURI );

        XStream xstream = XStreamFactory.getXmlXStream();

        xstream.processAnnotations( IndexBrowserTreeNode.class );
        xstream.processAnnotations( IndexBrowserTreeViewResponseDTO.class );

        XStreamRepresentation re = new XStreamRepresentation( xstream, responseText, MediaType.APPLICATION_XML );
        IndexBrowserTreeViewResponseDTO resourceResponse =
            (IndexBrowserTreeViewResponseDTO) re.getPayload( new IndexBrowserTreeViewResponseDTO() );

        IndexBrowserTreeNode content = resourceResponse.getData();

        List<TreeNode> children = content.getChildren();
        for ( TreeNode child : children )
        {
            Assert.assertEquals( "nexus1961", child.getNodeName() );
        }

    }
}
