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

import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;

public class RepositoryStatusMessageUtil
{

    private static final RepositoriesNexusRestClient REPOSITORY_NRC = new RepositoriesNexusRestClient(
        RequestFacade.getNexusRestClient(),
        new TasksNexusRestClient( RequestFacade.getNexusRestClient() ),
        new EventInspectorsUtil( RequestFacade.getNexusRestClient() )
    );

    public static Response putOutOfService( String repoId, String repoType )
        throws IOException
    {
        return REPOSITORY_NRC.putOutOfService( repoId, repoType );
    }

    public static Response putInService( String repoId, String repoType )
        throws IOException
    {
        return REPOSITORY_NRC.putInService( repoId, repoType );
    }

    /**
     * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
     */
    public static Response changeStatus( RepositoryStatusResource status )
        throws IOException
    {
        return REPOSITORY_NRC.changeStatus( status );
    }

}
