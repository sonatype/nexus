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
package org.sonatype.nexus.plugins.restlet1x;

import org.sonatype.nexus.plugins.rest.AbstractDocumentationNexusResourceBundle;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @since 2.3
 */
@Named
@Singleton
public class DocumentationResourceBundleImpl
    extends AbstractDocumentationNexusResourceBundle
{
    @Override
    public String getPluginId()
    {
        return "nexus-restlet1x-plugin";
    }

    @Override
    public String getDescription()
    {
        return "Restlet 1.x API";
    }
}
