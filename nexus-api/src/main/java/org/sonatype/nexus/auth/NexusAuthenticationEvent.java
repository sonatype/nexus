package org.sonatype.nexus.auth;

import org.sonatype.nexus.proxy.events.AbstractEvent;

public class NexusAuthenticationEvent
    extends AbstractEvent
{
    private final AuthenticationItem item;
    
    public NexusAuthenticationEvent( AuthenticationItem item )
    {
        super();
        this.item = item;
    }
    
    public AuthenticationItem getItem()
    {
        return item;
    }
}
