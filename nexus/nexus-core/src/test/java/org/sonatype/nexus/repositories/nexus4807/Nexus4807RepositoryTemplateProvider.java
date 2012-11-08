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
package org.sonatype.nexus.repositories.nexus4807;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

// see org/sonatype/nexus/repositories/nexus4807/Nexus4807Test.xml
//@Component( role = TemplateProvider.class, hint = Nexus4807RepositoryTemplateProvider.PROVIDER_ID )
public class Nexus4807RepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
{
    public static final String PROVIDER_ID = "nexus4807";

    public TemplateSet getTemplates()
    {
        TemplateSet templates = new TemplateSet( null );

        templates.add( new Nexus4807RepositoryTemplate( this, "nexus4807", "NEXUS-4807 (test repository)" ) );
        
        return templates;
    }
}
