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
package org.sonatype.nexus.templates;

public abstract class AbstractTemplate
    implements Template
{
    private final TemplateProvider provider;

    private final String id;

    private final String description;

    public AbstractTemplate( TemplateProvider provider, String id, String description )
    {
        this.provider = provider;

        this.id = id;

        this.description = description;
    }

    public TemplateProvider getTemplateProvider()
    {
        return provider;
    }

    public String getId()
    {
        return id;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean targetFits( Object clazz )
    {
        return targetIsClassAndFitsClass( clazz, getClass() );
    }

    // ==

    protected boolean targetIsClassAndFitsClass( Object filter, Class<?> clazz )
    {
        if ( filter instanceof Class<?> )
        {
            return ( (Class<?>) filter ).isAssignableFrom( clazz );
        }
        else
        {
            return false;
        }
    }
}
