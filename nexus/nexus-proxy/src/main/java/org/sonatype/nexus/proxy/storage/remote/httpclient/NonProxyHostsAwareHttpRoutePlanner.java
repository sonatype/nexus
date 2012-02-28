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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.ClientParamsStack;
import org.apache.http.impl.conn.DefaultHttpRoutePlanner;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import com.google.common.base.Preconditions;

/**
 * An {@link HttpRoutePlanner} that bypasses proxy for specific hosts.
 *
 * @since 2.0
 */
class NonProxyHostsAwareHttpRoutePlanner
    extends DefaultHttpRoutePlanner
{

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    /**
     * Set of patterns for matching hosts names against. Never null.
     */
    private final Set<Pattern> nonProxyHostPatterns;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    NonProxyHostsAwareHttpRoutePlanner( final SchemeRegistry schemeRegistry,
                                        final Set<Pattern> nonProxyHostPatterns )
    {
        super( schemeRegistry );
        this.nonProxyHostPatterns = Preconditions.checkNotNull( nonProxyHostPatterns );
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public HttpRoute determineRoute( final HttpHost target,
                                     final HttpRequest request,
                                     final HttpContext context )
        throws HttpException
    {
        if ( request.getParams() instanceof ClientParamsStack )
        {
            request.setParams( new NonProxyHostParamsStack( (ClientParamsStack) request.getParams(), noProxyFor( target.getHostName() ) ) );
        }
        else
        {
            request.setParams( new NonProxyHostParamsStack( request.getParams(), noProxyFor( target.getHostName() ) ) );
        }

        return super.determineRoute( target, request, context );
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    private boolean noProxyFor( final String hostName )
    {
        for ( final Pattern nonProxyHostPattern : nonProxyHostPatterns )
        {
            if ( nonProxyHostPattern.matcher( hostName ).matches() )
            {
                return true;
            }
        }
        return false;
    }

    private static class NonProxyHostParamsStack
        extends ClientParamsStack
    {

        private final boolean filterProxy;

        /**
         * @param stack the parameter stack to delegate to
         * @param filterProxy if {@code true}, always return null on {@code #getParameter( ConnRouteParams.DEFAULT_PROXY )}
         */
        public NonProxyHostParamsStack( final ClientParamsStack stack, final boolean filterProxy )
        {
            super( stack.getApplicationParams(), stack.getClientParams(), stack.getRequestParams(),
                   stack.getOverrideParams() );
            this.filterProxy = filterProxy;
        }

        /**
         * @param params the parameters instance to delegate to
         * @param filterProxy if {@code true}, always return null on {@code #getParameter( ConnRouteParams.DEFAULT_PROXY )}
         */
        public NonProxyHostParamsStack( final HttpParams params, final boolean filterProxy )
        {
            super( null, null, params, null );
            this.filterProxy = filterProxy;
        }

        @Override
        public Object getParameter( final String name )
        {
            return filterProxy && ConnRouteParams.DEFAULT_PROXY.equals( name ) ? null : super.getParameter( name );
        }
    }
}
