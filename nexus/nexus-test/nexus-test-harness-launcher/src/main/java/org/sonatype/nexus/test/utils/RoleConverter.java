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

import java.util.List;

import org.sonatype.security.model.CRole;
import org.sonatype.security.rest.model.RoleResource;


public class RoleConverter
{
    

    public static RoleResource toRoleResource( CRole role )
    {
        //TODO: ultimately this method will take a parameter which is the nexus object
        //and will convert to the rest object
        RoleResource resource = new RoleResource();
        
        resource.setDescription( role.getDescription() );
        resource.setId( role.getId() );
        resource.setName( role.getName() );
        resource.setSessionTimeout( role.getSessionTimeout() );
        
        for ( String roleId : ( List<String>) role.getRoles() )
        {
            resource.addRole( roleId );
        }
        
        for ( String privId : ( List<String>) role.getPrivileges() )
        {
            resource.addPrivilege( privId );
        }
        
        return resource;
    }
    
    public static CRole toCRole( RoleResource resource )
    {
        CRole role = new CRole();
        
        role.setId( resource.getId()  );
        role.setDescription( resource.getDescription() );
        role.setName( resource.getName() );
        role.setSessionTimeout( resource.getSessionTimeout() );
        
        role.getRoles().clear();        
        for ( String roleId : ( List<String> ) resource.getRoles() )
        {
            role.addRole( roleId );
        }
        
        role.getPrivileges().clear();
        for ( String privId : ( List<String> ) resource.getPrivileges() )
        {
            role.addPrivilege( privId );
        }
        
        return role;
    }


}
