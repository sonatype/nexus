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
package org.sonatype.nexus.util.configuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.sonatype.nexus.logging.AbstractLoggingComponent;

public class DefaultConfigurationHelper
    extends AbstractLoggingComponent
    implements ConfigurationHelper
{
    /**
     * Save the configuration to a file safely, utilizing an intermediate file for writing to prevent possible corruption.
     */
    @Override
    public <E> void save( E configuration, File configurationFile, ConfigurationWriter<E> writer) throws IOException
    {
        Writer fw = null;

        try
        {
            configurationFile.getParentFile().mkdirs();

            File tempFile = new File(configurationFile.getParentFile(), configurationFile.getName() + ".tmp");
            fw = new BufferedWriter( new FileWriter( tempFile ) );
            writer.write( fw, configuration );
            fw.flush();
            fw.close();

            Files.move( tempFile, configurationFile );
        }
        finally
        {
            IOUtils.closeQuietly( fw );
        }
    }
}
