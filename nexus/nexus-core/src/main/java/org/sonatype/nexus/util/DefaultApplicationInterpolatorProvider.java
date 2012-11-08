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
package org.sonatype.nexus.util;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * A simple class that holds Regex interpolator and has reference to Plexus context too, to centralize Plexus coupling
 * but make application Plexus interpolation capable too. This interpolator interpolates with Plexus Context,
 * Environment variables and System Properties, in this order.
 * 
 * @author cstamas
 */
@Component( role = ApplicationInterpolatorProvider.class )
public class DefaultApplicationInterpolatorProvider
    implements ApplicationInterpolatorProvider, Contextualizable
{
    private RegexBasedInterpolator regexBasedInterpolator;

    public DefaultApplicationInterpolatorProvider()
    {
        super();

        regexBasedInterpolator = new RegexBasedInterpolator();
    }

    public Interpolator getInterpolator()
    {
        return regexBasedInterpolator;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        regexBasedInterpolator.addValueSource( new MapBasedValueSource( context.getContextData() ) );

        // FIXME: bad, everything should come from Plexus context
        regexBasedInterpolator.addValueSource( new MapBasedValueSource( System.getenv() ) );

        // FIXME: bad, everything should come from Plexus context
        regexBasedInterpolator.addValueSource( new MapBasedValueSource( System.getProperties() ) );
    }

}
