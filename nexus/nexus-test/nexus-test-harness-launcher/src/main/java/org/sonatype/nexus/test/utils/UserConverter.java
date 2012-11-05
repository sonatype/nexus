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
package org.sonatype.nexus.test.utils;

import org.sonatype.security.model.CUser;
import org.sonatype.security.rest.model.UserResource;

public class UserConverter
{

    public static UserResource toUserResource( CUser user )
    {
        UserResource resource = new UserResource();
        resource.setEmail( user.getEmail() );
        resource.setFirstName( user.getFirstName() );
        resource.setLastName( user.getLastName() );
        resource.setStatus( user.getStatus() );
        resource.setUserId( user.getId() );

//        for ( String roleId : (List<String>) user.getRoles() )
//        {
//            resource.addRole( roleId );
//        }

        return resource;
    }

    public static CUser toCUser( UserResource resource )
    {
        CUser user = new CUser();

        user.setEmail( resource.getEmail() );
        user.setFirstName( resource.getFirstName() );
        user.setLastName( resource.getLastName() );
        user.setStatus( resource.getStatus() );
        user.setId( resource.getUserId() );

//        user.getRoles().clear();
//        for ( String roleId : (List<String>) resource.getRoles() )
//        {
//            user.addRole( roleId );
//        }

        return user;
    }

}
