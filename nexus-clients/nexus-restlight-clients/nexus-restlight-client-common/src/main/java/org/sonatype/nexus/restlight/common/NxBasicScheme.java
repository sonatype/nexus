package org.sonatype.nexus.restlight.common;

import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.BasicScheme;

/**
 * {@link AuthScheme} for use with commons-httpclient that implements Nexus' NxBASIC
 * HTTP authentication scheme. This is just an extension of {@link BasicScheme} that uses the name
 * 'NxBASIC' for registration with httpclient.
 */
public class NxBasicScheme
    extends BasicScheme
{

    static final String NAME = "NxBASIC";

    @Override
    public String getSchemeName()
    {
        return NAME;
    }

    @Override
    public String getID()
    {
        return NAME;
    }
}
