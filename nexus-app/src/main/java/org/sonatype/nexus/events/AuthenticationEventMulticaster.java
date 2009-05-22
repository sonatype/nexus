package org.sonatype.nexus.events;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.nexus.proxy.EventMulticasterComponent;

@Component( role = AuthenticationEventMulticaster.class )
public class AuthenticationEventMulticaster
    extends EventMulticasterComponent
        implements Initializable
{
    @Requirement
    private EventInspectorHost eventInspectorHost;
    
    public void initialize()
        throws InitializationException
    {
        addProximityEventListener( eventInspectorHost );
    }
}
