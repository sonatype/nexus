package org.sonatype.nexus.plugins.security;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.security.realms.tools.AbstractStaticSecurityResource;
import org.sonatype.security.realms.tools.StaticSecurityResource;

@Component( role = StaticSecurityResource.class, hint = "ThreePrivsSecurityResource" )
public class ThreePrivsSecurityResource
    extends AbstractStaticSecurityResource
    implements StaticSecurityResource
{

    @Override
    public String getResourcePath()
    {
        return "/META-INF/nexus-three-privs-plugin-security.xml";
    }

}
