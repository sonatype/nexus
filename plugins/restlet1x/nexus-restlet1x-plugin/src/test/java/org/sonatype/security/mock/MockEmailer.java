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
package org.sonatype.security.mock;

import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.security.email.SecurityEmailer;

@Component( role = SecurityEmailer.class )
public class MockEmailer
    implements SecurityEmailer
{

    public void sendForgotUsername( String arg0, List<String> arg1 )
    {
        // TODO Auto-generated method stub

    }

    public void sendNewUserCreated( String arg0, String arg1, String arg2 )
    {
        // TODO Auto-generated method stub

    }

    public void sendResetPassword( String arg0, String arg1 )
    {
        // TODO Auto-generated method stub

    }

}
