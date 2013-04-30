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
package org.sonatype.nexus.log.internal;

import java.io.IOException;
import java.io.InputStream;


import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.log.LogConfigurationParticipant;

/**
 * Contributes "logback-events" to logback configuration.
 * 
 * @author adreghiciu@gmail.com
 */
@Component( role = LogConfigurationParticipant.class, hint="logback-events" )
public class LogbackNexusEventSystemLogConfigurationParticipant
    implements LogConfigurationParticipant, LogConfigurationParticipant.NonEditable
{
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return "logback-events.xml";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getConfiguration()
    {
        try
        {
            return this.getClass().getResource( "/META-INF/log/logback-events.xml" ).openStream();
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Could not access logback-events.xml", e );
        }
    }

}
