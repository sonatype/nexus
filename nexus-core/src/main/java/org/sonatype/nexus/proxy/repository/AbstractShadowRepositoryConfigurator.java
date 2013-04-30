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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;

public abstract class AbstractShadowRepositoryConfigurator
    extends AbstractProxyRepositoryConfigurator
{
    @Override
    public void doApplyConfiguration( Repository repository, ApplicationConfiguration configuration,
                                      CRepositoryCoreConfiguration coreConfig )
        throws ConfigurationException
    {
        // Shadows are read only
        repository.setWritePolicy( RepositoryWritePolicy.READ_ONLY );
        
        super.doApplyConfiguration( repository, configuration, coreConfig );

        ShadowRepository shadowRepository = repository.adaptToFacet( ShadowRepository.class );

        AbstractShadowRepositoryConfiguration extConf =
            (AbstractShadowRepositoryConfiguration) coreConfig.getExternalConfiguration().getConfiguration( false );

        try
        {
            // undone change made in https://github.com/sonatype/nexus/commit/64fb67dc16ef2e18300d3df3eb4f87d419b584cc
            // original change was for NEXUS-4715
            // undone due to NEXUS-4901
            // ultimate fix will come with NEXUS-4909 
            // that will get rid among other thing, tricks like this one below
            // https://github.com/sonatype/nexus/blob/master/nexus/nexus-app/src/main/java/org/sonatype/nexus/configuration/application/DefaultNexusConfiguration.java#L557
            shadowRepository.setMasterRepositoryId( extConf.getMasterRepositoryId() );
        }
        catch ( IncompatibleMasterRepositoryException e )
        {
            ValidationMessage message =
                new ValidationMessage( "shadowOf", e.getMessage(),
                                       "The source nexus repository is of an invalid Format." );

            ValidationResponse response = new ApplicationValidationResponse();

            response.addValidationError( message );

            throw new InvalidConfigurationException( response );
        }
        catch ( NoSuchRepositoryException e )
        {
            ValidationMessage message =
                new ValidationMessage( "shadowOf", e.getMessage(), "The source nexus repository is not existing." );

            ValidationResponse response = new ApplicationValidationResponse();

            response.addValidationError( message );

            throw new InvalidConfigurationException( response );
        }
    }

}
