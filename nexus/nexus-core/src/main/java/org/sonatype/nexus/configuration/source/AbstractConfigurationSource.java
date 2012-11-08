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
package org.sonatype.nexus.configuration.source;

import org.sonatype.configuration.source.ConfigurationSource;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.logging.AbstractLoggingComponent;

/**
 * Abstract class that encapsulates Modello model loading and saving with interpolation.
 * 
 * @author cstamas
 */
public abstract class AbstractConfigurationSource
    extends AbstractLoggingComponent
    implements ConfigurationSource<Configuration>
{
    /** Flag to mark update. */
    private boolean configurationUpgraded;

    /** The validation response */
    private ValidationResponse validationResponse;

    public ValidationResponse getValidationResponse()
    {
        return validationResponse;
    }

    protected void setValidationResponse( ValidationResponse validationResponse )
    {
        this.validationResponse = validationResponse;
    }

    /**
     * Is configuration updated?
     */
    public boolean isConfigurationUpgraded()
    {
        return configurationUpgraded;
    }

    /**
     * Setter for configuration pugraded.
     * 
     * @param configurationUpgraded
     */
    public void setConfigurationUpgraded( boolean configurationUpgraded )
    {
        this.configurationUpgraded = configurationUpgraded;
    }

    /**
     * Returns the default source of ConfigurationSource. May be null.
     */
    public ConfigurationSource<Configuration> getDefaultsSource()
    {
        return null;
    }
}
