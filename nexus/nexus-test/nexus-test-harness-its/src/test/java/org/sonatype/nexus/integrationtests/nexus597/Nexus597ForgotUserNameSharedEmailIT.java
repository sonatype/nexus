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
package org.sonatype.nexus.integrationtests.nexus597;

import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.nexus395.AbstractForgotUserNameIT;
import org.sonatype.nexus.test.utils.ForgotUsernameUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author juven
 */
public class Nexus597ForgotUserNameSharedEmailIT
    extends AbstractForgotUserNameIT
{
    @Test
    public void recoverUserNameWithSharedEmail()
        throws Exception
    {
        String anonymousEmail = "changeme2@yourcompany.com";

        // username should be recovered with anonymous being ignored
        Status status = ForgotUsernameUtils.get( this ).recoverUsername( anonymousEmail );

        Assert.assertEquals( Status.SUCCESS_ACCEPTED.getCode(), status.getCode() );

        assertRecoveredUserName( "test-user-1" );
    }
}
