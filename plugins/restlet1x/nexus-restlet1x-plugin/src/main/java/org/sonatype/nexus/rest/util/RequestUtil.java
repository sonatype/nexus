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
package org.sonatype.nexus.rest.util;

import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.util.Series;

import com.google.common.collect.Maps;

/**
 * Helper class for some handy method operating on {@link Request}.
 * 
 * @author cstamas
 * @since 2.5
 */
public class RequestUtil
{
    private RequestUtil()
    {
        // no instance please
    }

    /**
     * Simple handy method that creates a map out of all Restlet Request's query parameters.
     * 
     * @param request the Restlet request to source for properties.
     * @param prefix the prefix to match. How it is exactly matched depends on stripOffPrefix parameter. Might be
     *            {@code null}, and then it gathers all query parameters into the resulting map.
     * @param stripOffPrefix set to {@code true} if you want to have prefixes stripped off from keys, or {@code false}
     *            if you want to use parameter names as-is. Note, that if this parameter is {@code true}, matching will
     *            be done for "real prefix" ({@code paramName.length() > prefix.length()}), to have some remaining part
     *            after stripping the prefix off, to not end up with empty string as key.
     * @param allowNullValues set to {@code true} will allow {@code null} values to be mapped, otherwise the key with
     *            {@code null} value will not be put into the resulting map.
     * @return The map with gathered key-values.
     */
    public static Map<String, String> gatherQueryParametersToMap( final Request request, final String prefix,
                                                                  final boolean stripOffPrefix,
                                                                  final boolean allowNullValues )
    {
        final Form form = request.getResourceRef().getQueryAsForm();
        return seriesToMap( form, prefix, stripOffPrefix, allowNullValues );
    }

    /**
     * Simple handy method that creates a map out of all Restlet Request's HTTP headers.
     * 
     * @param request the Restlet request to source for properties.
     * @param prefix the prefix to match. How it is exactly matched depends on stripOffPrefix parameter. Might be
     *            {@code null}, and then it gathers all headers into the resulting map.
     * @param stripOffPrefix set to {@code true} if you want to have prefixes stripped off from keys, or {@code false}
     *            if you want to use parameter names as-is. Note, that if this parameter is {@code true}, matching will
     *            be done for "real prefix" ({@code paramName.length() > prefix.length()}), to have some remaining part
     *            after stripping the prefix off, to not end up with empty string as key.
     * @param allowNullValues set to {@code true} will allow {@code null} values to be mapped, otherwise the key with
     *            {@code null} value will not be put into the resulting map.
     * @return The map with gathered key-values.
     */
    public static Map<String, String> gatherRequestHeadersToMap( final Request request, final String prefix,
                                                                 final boolean stripOffPrefix,
                                                                 final boolean allowNullValues )
    {
        final Series<Parameter> headers = (Series<Parameter>) request.getAttributes().get( "org.restlet.http.headers" );
        return seriesToMap( headers, prefix, stripOffPrefix, allowNullValues );
    }

    // ==

    private static Map<String, String> seriesToMap( final Series<Parameter> parameters, final String prefix,
                                                    final boolean stripOffPrefix, final boolean allowNullValues )
    {
        final Map<String, String> result = Maps.newHashMapWithExpectedSize( parameters.size() );
        for ( Parameter parameter : parameters )
        {
            String key = null;
            String value = null;
            // we strictly check for prefixes (real prefix, not equals)
            if ( prefix != null && parameter.getName().startsWith( prefix )
                && ( !stripOffPrefix || parameter.getName().length() > prefix.length() ) )
            {
                if ( stripOffPrefix )
                {
                    key = parameter.getName().substring( prefix.length() );
                }
                else
                {
                    key = parameter.getName();
                }
                value = parameter.getValue();
            }
            else if ( prefix == null )
            {
                key = parameter.getName();
                value = parameter.getValue();
            }

            if ( key != null )
            {
                if ( !StringUtils.isBlank( value ) || allowNullValues )
                {
                    result.put( StringEscapeUtils.escapeHtml( key ), StringEscapeUtils.escapeHtml( value ) );
                }
            }
        }
        return result;
    }
}
