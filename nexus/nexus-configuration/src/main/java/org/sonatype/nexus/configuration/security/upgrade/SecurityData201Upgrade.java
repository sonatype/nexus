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
package org.sonatype.nexus.configuration.security.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.nexus.logging.Slf4jPlexusLogger;
import org.sonatype.security.model.v2_0_2.CPrivilege;
import org.sonatype.security.model.v2_0_2.CProperty;
import org.sonatype.security.model.v2_0_2.CRole;
import org.sonatype.security.model.v2_0_2.Configuration;
import org.sonatype.security.model.v2_0_2.io.xpp3.SecurityConfigurationXpp3Reader;
import org.sonatype.security.model.upgrade.AbstractDataUpgrader;
import org.sonatype.security.model.upgrade.SecurityDataUpgrader;

@Component( role = SecurityDataUpgrader.class, hint = "2.0.1" )
public class SecurityData201Upgrade
    extends AbstractDataUpgrader<Configuration>
    implements SecurityDataUpgrader
{

    private Logger logger = Slf4jPlexusLogger.getPlexusLogger( getClass() );

    @Override
    public void doUpgrade( Configuration configuration )
        throws ConfigurationIsCorruptedException
    {
        // the view permissions are created on the fly
        // so we just need to add them where needed
        // the format is repository-<repositoryId>

        // put the privileges in a map so we can access them easily
        // only the target privileges

        // we need the static privileges too, and we need to directly parse that file from the classpath.
        Map<String, CPrivilege> privilegeMap = this.getStaticPrivilages();
        for ( CPrivilege privilege : (List<CPrivilege>) configuration.getPrivileges() )
        {
            if ( "target".equals( privilege.getType() ) )
            {
                privilegeMap.put( privilege.getId(), privilege );
            }
        }

        for ( CRole role : (List<CRole>) configuration.getRoles() )
        {

            // we need to add a 'view' permission for each target we find.
            Set<String> repositories = new HashSet<String>();

            List<String> privilegeIds = (List<String>) role.getPrivileges();

            for ( String privilegeId : privilegeIds )
            {
                CPrivilege privilege = privilegeMap.get( privilegeId );
                if ( privilege != null )
                {
                    repositories.addAll( this.getRepositoriesFromTargetPrivilege( privilege ) );
                }
                else
                {
                    this.logger.warn( "Failed to find privilege '" + privilegeId + "', but it was under the role '"
                        + role.getId() + "'." );
                }
            }

            // if repository-all is in the list, that is the only one we should add, 'all' and thats it
            if ( repositories.contains( "all" ) )
            {
                this.addViewPermissionToRole( role, privilegeIds, "all" );
            }
            else
            {
                // now we need to update the role with any new 'view' permissions
                for ( String repoId : repositories )
                {
                    this.addViewPermissionToRole( role, privilegeIds, repoId );
                }
            }
        }

    }

    private void addViewPermissionToRole( CRole role, List<String> existingPrivilegeIds, String repositoryId )
    {
        String permission = "repository-" + repositoryId;
        // make sure this privilege doesn't exists already
        if ( !existingPrivilegeIds.contains( permission ) )
        {
            role.addPrivilege( permission );
        }
    }

    @SuppressWarnings( { "unchecked" } )
    private Set<String> getRepositoriesFromTargetPrivilege( CPrivilege privilege )
    {
        // the properties we are looking for are:
        // repositoryId or repositoryGroupId
        // if not we need to use 'all'
        Set<String> repositories = new HashSet<String>();

        for ( CProperty property : (List<CProperty>) privilege.getProperties() )
        {
            if ( "repositoryId".equals( property.getKey() ) || "repositoryGroupId".equals( property.getKey() ) )
            {
                if ( StringUtils.isNotEmpty( property.getValue() ) )
                {
                    repositories.add( property.getValue() );
                }
            }
        }

        // if we didn't find a repository or group, add 'all' to the set
        if ( repositories.isEmpty() )
        {
            repositories.add( "all" );
        }

        return repositories;
    }

    private Map<String, CPrivilege> getStaticPrivilages()
    {
        String staticSecurityPath = "/META-INF/nexus/static-security.xml";
        Map<String, CPrivilege> privilegeMap = new HashMap<String, CPrivilege>();

        Reader fr = null;
        InputStream is = null;

        try
        {
            is = getClass().getResourceAsStream( staticSecurityPath );
            SecurityConfigurationXpp3Reader reader = new SecurityConfigurationXpp3Reader();

            fr = new InputStreamReader( is );

            Configuration staticConfig = reader.read( fr );

            for ( CPrivilege privilege : (List<CPrivilege>) staticConfig.getPrivileges() )
            {
                if ( "target".equals( privilege.getType() ) )
                {
                    privilegeMap.put( privilege.getId(), privilege );
                }
            }

        }
        catch ( IOException e )
        {
            this.logger.error( "IOException while retrieving configuration file", e );
        }
        catch ( org.codehaus.plexus.util.xml.pull.XmlPullParserException e )
        {
            this.logger.error( "Invalid XML Configuration", e );
        }
        finally
        {
            IOUtil.close( fr );
            IOUtil.close( is );
        }

        return privilegeMap;
    }

}
