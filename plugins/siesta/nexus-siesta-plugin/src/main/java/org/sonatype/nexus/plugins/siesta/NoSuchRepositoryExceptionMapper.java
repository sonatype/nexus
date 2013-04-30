/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.siesta;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.sisu.siesta.common.error.ErrorXO;
import org.sonatype.sisu.siesta.server.ErrorExceptionMapperSupport;

/**
 * Maps {@link NoSuchRepositoryException} to 404 with a {@link ErrorXO} body.
 *
 * @since 2.4
 */
@Named
@Singleton
public class NoSuchRepositoryExceptionMapper
    extends ErrorExceptionMapperSupport<NoSuchRepositoryException>
{

    @Override
    protected int getStatusCode( final NoSuchRepositoryException exception )
    {
        return Response.Status.NOT_FOUND.getStatusCode();
    }

}
