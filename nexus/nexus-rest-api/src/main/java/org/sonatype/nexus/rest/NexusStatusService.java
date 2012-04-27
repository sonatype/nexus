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
package org.sonatype.nexus.rest;

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.service.StatusService;
import org.sonatype.plexus.rest.ReferenceFactory;

/**
 * Nexus specific status service that simply assembles an "error page", same one as assembled by
 * {@code com.noelios.restlet.StatusFilter} but with escaped description.
 * 
 * @author cstamas
 */
@Component( role = StatusService.class )
public class NexusStatusService
    extends StatusService
{
    @Requirement
    private ReferenceFactory referenceFactory;

    @Override
    public Representation getRepresentation( final Status status, final Request request, final Response response )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "<html>\n" );
        sb.append( "<head>\n" );
        sb.append( "   <title>Status page</title>\n" );
        sb.append( "</head>\n" );
        sb.append( "<body>\n" );

        sb.append( "<h3>" );
        if ( status.getDescription() != null )
        {
            sb.append( StringEscapeUtils.escapeHtml( status.getDescription() ) );
        }
        else
        {
            sb.append( "No description available for this result status" );
        }
        sb.append( "</h3>" );
        sb.append( "<p>You can get technical details <a href=\"" );
        sb.append( status.getUri() );
        sb.append( "\">here</a>.<br>\n" );

        sb.append( "Please continue your visit at our <a href=\"" );
        sb.append( referenceFactory.createReference( request.getRootRef(), null ) );
        sb.append( "\">home page</a>.\n" );

        sb.append( "</p>\n" );
        sb.append( "</body>\n" );
        sb.append( "</html>\n" );

        return new StringRepresentation( sb.toString(), MediaType.TEXT_HTML );
    }

}
