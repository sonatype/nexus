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
package org.sonatype.security.ldap.realms.persist;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;
import org.sonatype.security.ldap.realms.persist.model.CUserAndGroupAuthConfiguration;
import org.sonatype.security.ldap.realms.persist.model.CUserRoleMapping;
import org.sonatype.security.ldap.realms.persist.model.Configuration;

@Component( role = ConfigurationValidator.class )
public class DefaultLdapConfigurationValidator
    implements ConfigurationValidator
{

    @SuppressWarnings("unchecked")
    public ValidationResponse validateModel( ValidationRequest request )
    {
        ValidationResponse response = new ValidationResponse();

        Configuration configuration = request.getConfiguration();

        if ( configuration == null )
        {
            ValidationMessage msg = new ValidationMessage( "*", "Configuration is missing." );
            response.addValidationError( msg );
        }
        else
        {
            if(configuration.getConnectionInfo() != null )
            {
                ValidationResponse vr = this.validateConnectionInfo( null, configuration.getConnectionInfo() );
                this.mergeValidationResponse( vr, response );
            }
            else
            {
                ValidationMessage msg = new ValidationMessage( "*", "Connection Configuration is missing." );
                response.addValidationError( msg );
            }
            
            if(configuration.getUserAndGroupConfig() != null )
            {
                ValidationResponse vr = this.validateUserAndGroupAuthConfiguration( null, configuration.getUserAndGroupConfig() );
                this.mergeValidationResponse( vr, response );
            }
            else
            {
                ValidationMessage msg = new ValidationMessage( "*", "User And Group Configuration is missing." );
                response.addValidationError( msg );
            }             
        }

        return response;
    }

    public ValidationResponse validateConnectionInfo( ValidationContext ctx, CConnectionInfo connectionInfo )
    {
        ValidationResponse response = new ValidationResponse();

        if ( StringUtils.isEmpty( connectionInfo.getHost() ) )
        {
            ValidationMessage msg = new ValidationMessage( "host", "Host cannot be empty." );
            response.addValidationError( msg );
        }
        if ( StringUtils.isEmpty( connectionInfo.getAuthScheme() ) )
        {
            ValidationMessage msg = new ValidationMessage( "authScheme", "Authorization Scheme cannot be empty." );
            response.addValidationError( msg );
        }
        if ( StringUtils.isEmpty( connectionInfo.getProtocol() ) )
        {
            ValidationMessage msg = new ValidationMessage( "protocol", "Protocol cannot be empty." );
            response.addValidationError( msg );
        }
        if ( StringUtils.isEmpty( connectionInfo.getSearchBase() ) )
        {
            ValidationMessage msg = new ValidationMessage( "searchBase", "Search Base cannot be empty." );
            response.addValidationError( msg );
        }
        if ( connectionInfo.getPort() < 1 )
        {
            ValidationMessage msg = new ValidationMessage( "port", "Port cannot be empty." );
            response.addValidationError( msg );
        }

        if ( StringUtils.isNotEmpty( connectionInfo.getAuthScheme() )
            && !connectionInfo.getAuthScheme().toLowerCase().equals( "none" ) )
        {

            // user and pass required if authScheme != none
            if ( StringUtils.isEmpty( connectionInfo.getSystemUsername() ) )
            {
                ValidationMessage msg = new ValidationMessage(
                    "systemUsername",
                    "Username cannot be empty unless the 'Authorization Scheme' is 'Anonymous Authentication'." );
                response.addValidationError( msg );
            }

            if ( StringUtils.isEmpty( connectionInfo.getSystemPassword() ) )
            {
                ValidationMessage msg = new ValidationMessage(
                    "systemPassword",
                    "Password cannot be empty unless the 'Authorization Scheme' is 'Anonymous Authentication'." );
                response.addValidationError( msg );
            }
        }
        
        return response;
    }

    public ValidationResponse validateUserAndGroupAuthConfiguration( ValidationContext ctx,
        CUserAndGroupAuthConfiguration userAndGroupAuthConf )
    {
        ValidationResponse response = new ValidationResponse();


        if ( StringUtils.isEmpty( userAndGroupAuthConf.getUserIdAttribute() ) )
        {
            ValidationMessage msg = new ValidationMessage( "userIdAttribute", "User ID Attribute cannot be empty." );
            response.addValidationError( msg );
        }
        if ( StringUtils.isEmpty( userAndGroupAuthConf.getUserObjectClass() ) )
        {
            ValidationMessage msg = new ValidationMessage( "userObjectClass", "User Object Class cannot be empty." );
            response.addValidationError( msg );
        }
        if ( StringUtils.isEmpty( userAndGroupAuthConf.getUserRealNameAttribute() ) )
        {
            ValidationMessage msg = new ValidationMessage(
                "userRealNameAttribute",
                "User Real Name Attribute cannot be empty." );
            response.addValidationError( msg );
        }
        if ( StringUtils.isEmpty( userAndGroupAuthConf.getEmailAddressAttribute() ) )
        {
            ValidationMessage msg = new ValidationMessage(
                "emailAddressAttribute",
                "Email Address Attribute cannot be empty." );
            response.addValidationError( msg );
        }
        
        if ( userAndGroupAuthConf.isLdapGroupsAsRoles() && StringUtils.isEmpty( userAndGroupAuthConf.getUserMemberOfAttribute() ) )
        {
            if ( StringUtils.isEmpty( userAndGroupAuthConf.getGroupIdAttribute() ) )
            {
                ValidationMessage msg = new ValidationMessage(
                    "groupIdAttribute",
                    "Group ID Attribute cannot be empty when Use LDAP Groups as Roles is true." );
                response.addValidationError( msg );
            }
            if ( StringUtils.isEmpty( userAndGroupAuthConf.getGroupMemberAttribute() ) )
            {
                ValidationMessage msg = new ValidationMessage(
                    "groupMemberAttribute",
                    "Group Member Attribute cannot be empty when Use LDAP Groups as Roles is true." );
                response.addValidationError( msg );
            }
            if ( StringUtils.isEmpty( userAndGroupAuthConf.getGroupMemberFormat() ) )
            {
                ValidationMessage msg = new ValidationMessage(
                    "groupMemberFormat",
                    "Group Member Format cannot be empty when Use LDAP Groups as Roles is true." );
                response.addValidationError( msg );
            }
            if ( StringUtils.isEmpty( userAndGroupAuthConf.getGroupObjectClass() ) )
            {
                ValidationMessage msg = new ValidationMessage(
                    "groupObjectClass",
                    "Group Object Class cannot be empty when Use LDAP Groups as Roles is true." );
                response.addValidationError( msg );
            }
        }
        return response;
    }

    public ValidationResponse validateUserRoleMapping( ValidationContext ctx, CUserRoleMapping userRoleMapping )
    {
        ValidationResponse response = new ValidationResponse();

        if ( StringUtils.isEmpty( userRoleMapping.getUserId() ) )
        {
            ValidationMessage msg = new ValidationMessage( "userId", "UserId cannot be empty." );
            response.addValidationError( msg );
        }
        if ( userRoleMapping == null || userRoleMapping.getRoles().size() == 0 )
        {
            ValidationMessage msg = new ValidationMessage( "roles", "Roles cannot be empty." );
            response.addValidationError( msg );
        }

        return response;
    }

    protected ValidationResponse mergeValidationResponse(ValidationResponse source, ValidationResponse dest)
    {
        for ( ValidationMessage message : source.getValidationErrors() )
        {
            dest.addValidationError( message );
        }
        
        for ( ValidationMessage message : source.getValidationWarnings() )
        {
            dest.addValidationError( message );
        }
        
        return dest;
    }
}
