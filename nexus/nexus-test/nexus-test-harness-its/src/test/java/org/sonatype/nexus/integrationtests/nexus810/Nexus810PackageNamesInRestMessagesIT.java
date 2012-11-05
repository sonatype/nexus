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
package org.sonatype.nexus.integrationtests.nexus810;

import static org.hamcrest.CoreMatchers.*;
import static org.sonatype.nexus.test.utils.ResponseMatchers.*;

import java.io.IOException;

import org.hamcrest.Matcher;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Checks to make sure the tasks don't have packages in the type field.
 */
public class Nexus810PackageNamesInRestMessagesIT
    extends AbstractNexusIntegrationTest
{
    @BeforeClass
    public void setSecureTest()
    {
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @Test
    public void checkForPackageNamesInResponse()
        throws IOException
    {
        Matcher<Response> matcher = allOf( isSuccessful(), not( responseText( containsString( "org.sonatype." ) ) ) );
        RequestFacade.doGet( "service/local/schedule_types", matcher );
    }
}
