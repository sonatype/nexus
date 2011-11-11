/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.nexus.email.NexusEmailer;
import org.sonatype.nexus.logging.Slf4jPlexusLogger;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.security.usermanagement.UserSearchCriteria;

@Component( role = Carrier.class, hint = EmailCarrier.KEY )
public class EmailCarrier
    implements Carrier
{
    public static final String KEY = "email";

    private Logger logger = Slf4jPlexusLogger.getPlexusLogger( getClass() );

    @Requirement
    private NexusEmailer nexusEmailer;

    @Requirement
    private SecuritySystem securitySystem;

    // --

    protected Logger getLogger()
    {
        return logger;
    }

    // --
    public void notifyTarget( NotificationTarget target, NotificationMessage message )
        throws NotificationException
    {
        MailRequest request = nexusEmailer.getDefaultMailRequest( message.getMessageTitle(), message.getMessageBody() );

        request.getToAddresses().addAll( resolveToAddresses( target ) );

        if ( request.getToAddresses().size() > 0 )
        {
            getLogger().info(
                "Sending out e-mail notification to notification group \"" + target.getTargetId() + "\" (total of "
                    + request.getToAddresses().size() + " recipients)." );

            nexusEmailer.sendMail( request );
        }
        else
        {
            getLogger().info(
                "Not sending out e-mail notification to notification group \"" + target.getTargetId()
                    + "\", there were no recipients (does users have e-mail accessible to Realm?)." );
        }
    }

    // --

    public List<Address> resolveToAddresses( NotificationTarget target )
        throws NotificationException
    {
        // TODO: should we use Set instead? One user may be in multiple roles....
        // Right now, he would get multiple mails too!
        // Or, make a map with emails as keys, that would keep em unique, and
        // at the end return the Values of it.

        ArrayList<Address> toAddresses = new ArrayList<Address>();

        // resolve roles to mails
        if ( target.getTargetRoles().size() > 0 )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Role ID's to notify (" + target.getTargetRoles().toString() + ")" );
            }

            UserSearchCriteria criteria = new UserSearchCriteria();

            criteria.setOneOfRoleIds( target.getTargetRoles() );

            Set<User> users = securitySystem.searchUsers( criteria );

            if ( users.size() > 0 )
            {
                for ( User user : users )
                {
                    if ( StringUtils.isNotBlank( user.getEmailAddress() ) )
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug(
                                "Adding user \"" + user.getName() + "\" (" + user.getEmailAddress() + ")." );
                        }

                        toAddresses.add( new Address( user.getEmailAddress(), user.getName() ) );
                    }
                }
            }
        }

        // resolve users to mails
        if ( target.getTargetUsers().size() > 0 )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "User ID's to notify (" + target.getTargetUsers().toString() + ")." );
            }

            for ( String userId : target.getTargetUsers() )
            {
                if ( StringUtils.isNotBlank( userId ) )
                {
                    try
                    {
                        User user = securitySystem.getUser( userId );

                        if ( StringUtils.isNotBlank( user.getEmailAddress() ) )
                        {
                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug(
                                    "Adding user \"" + user.getName() + "\" (" + user.getEmailAddress() + ")." );
                            }

                            toAddresses.add( new Address( user.getEmailAddress(), user.getName() ) );
                        }
                    }
                    catch ( UserNotFoundException e )
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        // resolve externals to mails
        if ( target.getExternalTargets().size() > 0 )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "External emails to notify (" + target.getExternalTargets().toString() + ")." );
            }

            for ( String email : target.getExternalTargets() )
            {
                if ( StringUtils.isNotBlank( email ) )
                {
                    toAddresses.add( new Address( email ) );
                }
            }
        }

        return toAddresses;
    }

    // --

}
