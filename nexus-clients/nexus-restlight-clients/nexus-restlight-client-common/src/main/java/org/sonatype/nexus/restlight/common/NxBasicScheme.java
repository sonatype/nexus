package org.sonatype.nexus.restlight.common;

import org.apache.commons.httpclient.auth.BasicScheme;

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
