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
package org.sonatype.nexus.email;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.EmailerConfiguration;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailRequestStatus;
import org.sonatype.micromailer.imp.DefaultMailType;
import org.sonatype.nexus.configuration.model.CSmtpConfiguration;
import org.sonatype.nexus.logging.AbstractLoggingComponent;

/**
 * @author velo
 */
@Component( role = SmtpSettingsValidator.class )
public class DefaultSmtpSettingsValidator
    extends AbstractLoggingComponent
    implements SmtpSettingsValidator
{
    @Requirement
    private EMailer emailer;

    private static final String NEXUS_MAIL_ID = "Nexus";

    public boolean sendSmtpConfigurationTest( CSmtpConfiguration smtp, String email )
        throws EmailerException
    {
        EmailerConfiguration config = new EmailerConfiguration();
        config.setDebug( smtp.isDebugMode() );
        config.setMailHost( smtp.getHostname() );
        config.setMailPort( smtp.getPort() );
        config.setPassword( smtp.getPassword() );
        config.setSsl( smtp.isSslEnabled() );
        config.setTls( smtp.isTlsEnabled() );
        config.setUsername( smtp.getUsername() );

        emailer.configure( config );

        MailRequest request = new MailRequest( NEXUS_MAIL_ID, DefaultMailType.DEFAULT_TYPE_ID );
        request.setFrom( new Address( smtp.getSystemEmailAddress(), "Nexus Repository Manager" ) );
        request.getToAddresses().add( new Address( email ) );
        request.getBodyContext().put( DefaultMailType.SUBJECT_KEY, "Nexus: SMTP Configuration validation." );

        StringBuilder body = new StringBuilder();
        body.append( "Your current SMTP configuration is valid!" );

        request.getBodyContext().put( DefaultMailType.BODY_KEY, body.toString() );

        MailRequestStatus status = emailer.sendSyncedMail( request );

        if ( status.getErrorCause() != null )
        {
            getLogger().error( "Unable to send e-mail", status.getErrorCause() );
            throw new EmailerException( "Unable to send e-mail", status.getErrorCause() );
        }

        return status.isSent();
    }
}
