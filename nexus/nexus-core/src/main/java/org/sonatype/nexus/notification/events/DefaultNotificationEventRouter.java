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
package org.sonatype.nexus.notification.events;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.notification.NotificationCheat;
import org.sonatype.nexus.notification.NotificationManager;
import org.sonatype.nexus.notification.NotificationRequest;
import org.sonatype.nexus.notification.NotificationTarget;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeChanged;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeSet;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.plexus.appevents.Event;

/**
 * This component routes based on Application event. Currently it is hardwired, but we would have some "mediation"
 * happen, and the best would be to have the "routing" of events to notifications saved in Nexus configuration. There
 * would be event-to-notificationGroup mapping, and routing should use that.
 * 
 * @author cstamas
 */
@Component( role = NotificationEventRouter.class )
public class DefaultNotificationEventRouter
    implements NotificationEventRouter
{
    @Requirement
    private NotificationManager notificationManager;

    public NotificationRequest getRequestForEvent( Event<?> evt )
    {
        // this is for notifying about repository being blocked
        if ( evt instanceof RepositoryEventProxyModeSet )
        {
            // this event is _always_ fired! Do not mix it with RepositoryEventProxyModeChanged event! Read their
            // JavaDoc!
            RepositoryEventProxyModeSet rpmevt = (RepositoryEventProxyModeSet) evt;

            // targets to be notified
            HashSet<NotificationTarget> targets = new HashSet<NotificationTarget>();

            // send out email notification only if event is AutoBlocked and repo is blocked for at least
            // 60secs TODO: 2 minutes, since we already see
            if ( ProxyMode.BLOCKED_AUTO.equals( rpmevt.getNewProxyMode() )
                && rpmevt.getRepository().getCurrentRemoteStatusRetainTime() >= 120000 )
            {
                // notify only once
                if ( !getAutoBlockedRepositoryIds().contains( rpmevt.getRepository().getId() ) )
                {
                    // currently we "hardwire" this one only
                    // later we should back this with real routing configuration
                    NotificationTarget autoBlockTarget =
                        notificationManager.readNotificationTarget( NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID );

                    if ( autoBlockTarget != null )
                    {
                        // add this target to set of targets
                        targets.add( autoBlockTarget );

                        // add this repo's ID to the "remembered" ones, since it is notified
                        getAutoBlockedRepositoryIds().add( rpmevt.getRepository().getId() );
                    }
                }
            }

            // we could lookup other groups too, like SMS notif, and even RSS feed could be one group
            // ...
            // stuff below this line is "generic" - hardwired stuff is above only

            if ( !targets.isEmpty() )
            {
                RepositoryEventProxyModeMessage message = new RepositoryEventProxyModeMessage( rpmevt, null );

                return new NotificationRequest( message, targets );
            }
            else
            {
                return NotificationRequest.EMPTY;
            }
        }
        // this is for notifying about repository being unblocked
        else if ( evt instanceof RepositoryEventProxyModeChanged )
        {
            // this event is fired _only_ on transition! Do not mix it with RepositoryEventProxyModeSet event! Read
            // their JavaDoc!
            RepositoryEventProxyModeChanged rpmevt = (RepositoryEventProxyModeChanged) evt;

            // targets to be notified
            HashSet<NotificationTarget> targets = new HashSet<NotificationTarget>();

            // notify about unblock only if it was notified about block
            if ( getAutoBlockedRepositoryIds().contains( rpmevt.getRepository().getId() ) )
            {
                // currently we "hardwire" this one only
                // later we should back this with real routing configuration
                NotificationTarget autoBlockTarget =
                    notificationManager.readNotificationTarget( NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID );

                if ( autoBlockTarget != null )
                {
                    // add this target to set of targets
                    targets.add( autoBlockTarget );

                    // add this repo's ID to the "remembered" ones, since it is notified
                    getAutoBlockedRepositoryIds().remove( rpmevt.getRepository().getId() );
                }
            }

            // we could lookup other groups too, like SMS notif, and even RSS feed could be one group
            // ...
            // stuff below this line is "generic" - hardwired stuff is above only

            if ( !targets.isEmpty() )
            {
                RepositoryEventProxyModeMessage message = new RepositoryEventProxyModeMessage( rpmevt, null );

                return new NotificationRequest( message, targets );
            }
            else
            {
                return NotificationRequest.EMPTY;
            }
        }
        else
        {
            return NotificationRequest.EMPTY;
        }
    }

    // ==

    private Set<String> autoBlockedRepositoryIds;

    protected Set<String> getAutoBlockedRepositoryIds()
    {
        if ( autoBlockedRepositoryIds == null )
        {
            autoBlockedRepositoryIds = new HashSet<String>();
        }

        return autoBlockedRepositoryIds;
    }
}
