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
import com.thoughtworks.xstream.converters.basic.StringConverter;
import org.apache.commons.lang.StringEscapeUtils;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserListResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserResponseDTO;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapAuthenticationTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequestDTO;
import org.sonatype.plexus.rest.xstream.AliasingListConverter;

public class XStreamInitalizer
{

    public static XStream initXStream( XStream xstream )
    {
        
        xstream.processAnnotations( LdapConnectionInfoResponse.class );
        xstream.processAnnotations( LdapUserAndGroupConfigurationResponse.class );
        xstream.processAnnotations( LdapUserListResponse.class );
        xstream.processAnnotations( LdapAuthenticationTestRequest.class );
        xstream.processAnnotations( LdapUserAndGroupConfigTestRequest.class );

        // NXCM-2974 unescape html entities like "o=org&amp;org", they get escaped by nexus-rest-api json->DTO
        // conversion
        final StringConverter converter = new StringConverter()
        {
            // UI sends null for some fields, super class does NOT like that.
            @Override
            public boolean canConvert( Class type )
            {
                return type == null || super.canConvert( type );
            }

            @Override
            public Object fromString( String str )
            {
                return StringEscapeUtils.unescapeHtml( str );
            }
        };

        xstream.registerLocalConverter( LdapConnectionInfoDTO.class, "systemUsername", converter );
        xstream.registerLocalConverter( LdapConnectionInfoDTO.class, "systemPassword", converter );
        xstream.registerLocalConverter( LdapConnectionInfoDTO.class, "searchBase", converter );
        xstream.registerLocalConverter( LdapUserAndGroupConfigurationDTO.class, "groupBaseDn", converter );
        xstream.registerLocalConverter( LdapUserAndGroupConfigurationDTO.class, "userBaseDn", converter );
        xstream.registerLocalConverter( LdapUserAndGroupConfigurationDTO.class, "groupMemberFormat", converter );
        xstream.registerLocalConverter( LdapUserAndGroupConfigurationDTO.class, "ldapFilter", converter );

        xstream.registerLocalConverter( LdapUserAndGroupConfigTestRequestDTO.class, "systemUsername", converter );
        xstream.registerLocalConverter( LdapUserAndGroupConfigTestRequestDTO.class, "systemPassword", converter );
        xstream.registerLocalConverter( LdapUserAndGroupConfigTestRequestDTO.class, "searchBase", converter );

        xstream.registerLocalConverter( LdapUserListResponse.class, "data", new AliasingListConverter(
          LdapUserResponseDTO.class, "user" ) );

        return xstream;
    }

}
