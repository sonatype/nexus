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
package org.sonatype.nexus.test.utils;

import java.io.IOException;

import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;

public class UserCreationUtil
{

    public static Status login()
        throws IOException
    {
        String serviceURI = "service/local/authentication/login";

        return RequestFacade.doGetForStatus( serviceURI, null );
    }

    public static Status login( String username, String password )
        throws IOException
    {
        TestContainer.getInstance().getTestContext().setUsername( username );
        TestContainer.getInstance().getTestContext().setPassword( password );

        return login();
    }

    /**
     * Log out the current user and assert a successful request.
     * 
     * @throws IOException
     * @throws AssertionError
     */
    public static void logout()
        throws IOException, AssertionError
    {
        String serviceURI = "service/local/authentication/logout";
        RequestFacade.doGet( serviceURI );
    }

}
