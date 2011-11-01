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
package org.sonatype.nexus.test.utils;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.InError;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsRedirecting;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsSuccessful;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsSuccessfulCode;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.RedirectLocationMatches;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.RespondsWithStatusCode;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.ResponseTextMatches;

public class ResponseMatchers
{

    @Factory
    public static RespondsWithStatusCode respondsWithStatusCode( final int expectedStatusCode )
    {
        return new RespondsWithStatusCode( expectedStatusCode );
    }

    @Factory
    public static InError inError()
    {
        return new InError();
    }

    @Factory
    public static IsSuccessful isSuccessful()
    {
        return new IsSuccessful();
    }

    @Factory
    public static ResponseTextMatches responseText( Matcher<String> matcher )
    {
        return new ResponseTextMatches( matcher );
    }

    @Factory
    public static IsRedirecting isRedirecting()
    {
        return new IsRedirecting();
    }

    @Factory
    public static RedirectLocationMatches redirectLocation( Matcher<String> matcher )
    {
        return new RedirectLocationMatches( matcher );
    }

    @Factory
    public static IsSuccessfulCode isSuccessfulCode()
    {
        return new IsSuccessfulCode();
    }

}
