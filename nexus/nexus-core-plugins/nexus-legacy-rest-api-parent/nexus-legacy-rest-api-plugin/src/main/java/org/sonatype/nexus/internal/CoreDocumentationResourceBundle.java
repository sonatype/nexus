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
package org.sonatype.nexus.internal;

import org.sonatype.nexus.plugins.rest.AbstractDocumentationNexusResourceBundle;
import org.sonatype.nexus.rest.NexusApplication;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * @since 2.3
 */
@Named
@Singleton
public class CoreDocumentationResourceBundle
    extends AbstractDocumentationNexusResourceBundle
{

    @Override
    public String getPluginId()
    {
        return "nexus-legacy-rest-api-plugin";
    }

    @Override
    public String getDescription()
    {
        return "Core (Reslet 1.x) API";
    }

    @Override
    public String getPathPrefix() {
        return "core";
    }

    @Override
    protected ZipFile getZipFile()
        throws IOException
    {
        return getZipFile( NexusApplication.class );
    }
}
