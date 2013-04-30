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
package org.sonatype.nexus.rest.user;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.sonatype.nexus.rest.model.UserAccount;
import org.sonatype.nexus.rest.model.UserAccountRequestResponseWrapper;
import org.sonatype.nexus.user.UserAccountManager;
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserStatus;

public abstract class AbstractUserAccountPlexusResource
    extends AbstractSecurityPlexusResource
{
    @Requirement
    protected UserAccountManager userAccountManager;

    @Override
    public void configureXStream( XStream xstream )
    {
        super.configureXStream( xstream );

        xstream.processAnnotations( UserAccount.class );
        xstream.processAnnotations( UserAccountRequestResponseWrapper.class );
    }

    protected UserAccount nexusToRestModel( User user, Request request )
    {
        UserAccount dto = new UserAccount();

        dto.setUserId( user.getUserId() );

        dto.setFirstName( user.getFirstName() );
        dto.setLastName( user.getLastName() );

        dto.setEmail( user.getEmailAddress() );

        return dto;
    }

    protected User restToNexusModel( UserAccount dto )
    {
        User user = new DefaultUser();

        user.setUserId( dto.getUserId() );

        user.setFirstName( dto.getFirstName() );
        user.setLastName( dto.getLastName() );

        user.setEmailAddress( dto.getEmail() );

        user.setSource( DEFAULT_SOURCE );

        user.setStatus( UserStatus.active );

        return user;
    }
}
