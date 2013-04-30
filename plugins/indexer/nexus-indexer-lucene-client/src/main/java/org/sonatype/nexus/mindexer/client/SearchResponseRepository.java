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
package org.sonatype.nexus.mindexer.client;

import org.sonatype.nexus.client.internal.util.Check;

public class SearchResponseRepository
{

    private final String id;

    private final String name;

    private final String contentClass;

    private final String baseUrl;

    public SearchResponseRepository( final String id, final String name, final String contentClass,
                                     final String baseUrl )
    {
        this.id = Check.notBlank( id, "id" );
        this.name = name;
        this.contentClass = Check.notBlank( contentClass, "contentClass" );
        this.baseUrl = baseUrl;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getContentClass()
    {
        return contentClass;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }
}
