/*
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
package org.sonatype.nexus.plugins.siesta;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.shiro.authz.AuthorizationException;
import org.sonatype.sisu.siesta.common.ErrorResponse;
import org.sonatype.sisu.siesta.common.ErrorResponseGenerator;
import org.sonatype.sisu.siesta.common.ExceptionMapperSupport;

/**
 * Converts {@link AuthorizationException} to a JAX-RS {@link Response} with {@link Response.Status#FORBIDDEN}.
 *
 * @since 2.4
 */
@Named
@Singleton
public class AuthorizationExceptionMapper
    extends ExceptionMapperSupport<AuthorizationException>
    implements ExceptionMapper<AuthorizationException>
{

    private final ErrorResponseGenerator errorResponseGenerator;

    @Inject
    public AuthorizationExceptionMapper(final ErrorResponseGenerator errorResponseGenerator)
    {
        this.errorResponseGenerator = checkNotNull( errorResponseGenerator );
    }

    @Override
    protected Response convert( final AuthorizationException exception )
    {
        final ErrorResponse response = errorResponseGenerator.mapException(
            exception, Response.Status.FORBIDDEN.getStatusCode()
        );

        return Response.status( response.getStatusCode() )
            .type( ErrorResponse.CONTENT_TYPE )
            .entity( response.getMessageBody() )
            .build();
    }

}
