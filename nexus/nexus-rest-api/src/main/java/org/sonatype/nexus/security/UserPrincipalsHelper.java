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
package org.sonatype.nexus.security;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.subject.PrincipalCollection;
import org.sonatype.security.usermanagement.RoleMappingUserManager;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.security.usermanagement.UserStatus;

/**
 * Helper component to map user principals to associated information.
 */
@Named
@Singleton
public class UserPrincipalsHelper
{
    @Inject
    private List<UserManager> userManagers;

    /**
     * Searches records to find the status of the user associated with the given principals.
     * 
     * @param principals Identifying principals
     * @return User status
     * @throws UserNotFoundException
     */
    public UserStatus getUserStatus( final PrincipalCollection principals )
        throws UserNotFoundException
    {
        final String userId = principals.getPrimaryPrincipal().toString();
        final UserManager associatedManager = findUserManager( principals );
        if ( associatedManager == null )
        {
            throw new UserNotFoundException( userId );
        }
        try
        {
            final User user = associatedManager.getUser( userId );
            if ( user != null )
            {
                return user.getStatus();
            }
        }
        catch ( final Exception e )
        {
            // fall through and check mappings...
        }
        final String source = associatedManager.getSource();
        for ( final UserManager userManager : userManagers )
        {
            try
            {
                // look for leftover role mappings which suggest user has only been temporarily disabled
                if ( userManager != associatedManager && userManager instanceof RoleMappingUserManager )
                {
                    if ( !( (RoleMappingUserManager) userManager ).getUsersRoles( userId, source ).isEmpty() )
                    {
                        return UserStatus.disabled;
                    }
                }
            }
            catch ( final Exception e )
            {
                // check next set of mappings
            }
        }
        throw new UserNotFoundException( userId );
    }

    /**
     * Searches for the {@link UserManager} associated with the given principals.
     * 
     * @param principals Identifying principals
     * @return UserManager component
     */
    public UserManager findUserManager( final PrincipalCollection principals )
    {
        final Iterator<String> itr = principals.getRealmNames().iterator();
        if ( itr.hasNext() )
        {
            final String primaryRealmName = itr.next();
            for ( final UserManager userManager : userManagers )
            {
                if ( primaryRealmName.equals( userManager.getAuthenticationRealmName() ) )
                {
                    return userManager;
                }
            }
        }
        return null;
    }
}
