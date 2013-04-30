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
package org.sonatype.nexus.plugins.ithelper.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.log.LogConfigurationParticipant;
import com.google.common.io.Closeables;

/**
 * TODO
 *
 * @since 1.0
 */
@Named
@Singleton
public class TestLogConfigurationParticipant
    implements LogConfigurationParticipant
{

    @Override
    public String getName()
    {
        return "logback-test.xml";
    }

    @Override
    public InputStream getConfiguration()
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( baos );
            writer.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
            writer.println();
            writer.println( "<included/>" );
        }
        finally
        {
            Closeables.closeQuietly( writer );
        }
        return new ByteArrayInputStream( baos.toByteArray() );
    }

}
