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
package org.sonatype.nexus.security.ldap.realms.api;

import com.thoughtworks.xstream.XStream;
import org.sonatype.nexus.rest.model.AliasingListConverter;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserListResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserResponseDTO;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapAuthenticationTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequest;

/**
 * XStream configurator for LDAP.
 * 
 * @author cstamas
 */
public class LdapXStreamConfigurator
{
    public static XStream configureXStream( XStream xstream )
    {
        xstream.processAnnotations( LdapConnectionInfoResponse.class );
        xstream.processAnnotations( LdapUserAndGroupConfigurationResponse.class );
        xstream.processAnnotations( LdapUserListResponse.class );
        xstream.processAnnotations( LdapAuthenticationTestRequest.class );
        xstream.processAnnotations( LdapUserAndGroupConfigTestRequest.class );

        xstream.registerLocalConverter( LdapUserListResponse.class, "data", new AliasingListConverter(
            LdapUserResponseDTO.class, "user" ) );

        return xstream;
    }

}
