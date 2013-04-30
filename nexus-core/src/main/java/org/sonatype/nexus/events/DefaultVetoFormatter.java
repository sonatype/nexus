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
package org.sonatype.nexus.events;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.ExceptionUtils;
import org.sonatype.nexus.proxy.events.Veto;
import org.sonatype.nexus.proxy.events.VetoFormatter;
import org.sonatype.nexus.proxy.events.VetoFormatterRequest;
import org.sonatype.nexus.util.StringDigester;

@Component( role = VetoFormatter.class )
public class DefaultVetoFormatter
    implements VetoFormatter
{
    public String format( VetoFormatterRequest request )
    {
        StringBuilder sb = new StringBuilder();
        
        if ( request != null
            && request.getEvent() != null 
            && request.getEvent().isVetoed() )
        {
            sb.append( "Event " + request.getEvent().toString() + " has been vetoed by one or more components." );
            
            if ( request.isDetailed() )
            {
                sb.append( StringDigester.LINE_SEPERATOR );
                
                for ( Veto veto : request.getEvent().getVetos() )
                {
                    sb.append( "vetoer: " + veto.getVetoer().toString() );
                    sb.append( "cause:" );
                    sb.append( StringDigester.LINE_SEPERATOR );
                    sb.append( ExceptionUtils.getFullStackTrace( veto.getReason() ) );
                    sb.append( StringDigester.LINE_SEPERATOR );
                }
            }
        }
        
        return sb.toString();
    }
}
