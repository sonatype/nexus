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
package org.sonatype.security.ldap.realms.persist;

import org.sonatype.security.ldap.dao.LdapAuthConfiguration;

import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;
import org.sonatype.security.ldap.realms.persist.model.CUserAndGroupAuthConfiguration;
import org.sonatype.security.ldap.realms.persist.model.Configuration;

public interface LdapConfiguration
{


    void save();

    void clearCache();

    Configuration getConfiguration();
    
    // connection info
    
    CConnectionInfo readConnectionInfo();
    
    void updateConnectionInfo( CConnectionInfo connectionInfo ) throws InvalidConfigurationException;

    
    // user and group info
    
    CUserAndGroupAuthConfiguration readUserAndGroupConfiguration();

    void updateUserAndGroupConfiguration( CUserAndGroupAuthConfiguration userAndGroupConf ) throws InvalidConfigurationException;
    
    LdapAuthConfiguration getLdapAuthConfiguration();
    
}
