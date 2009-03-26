package org.sonatype.nexus.mock;

import java.util.ArrayList;
import java.util.Iterator;

import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.sonatype.plexus.rest.resource.ManagedPlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResource;

/**
 * Rude listener implementation that: inhibits Nexus and all PlexusResource components from nexus-rest-api
 * 
 * @author cstamas
 */
public class InhibitingComponentDiscovererListener
    implements ComponentDiscoveryListener
{
    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor set = event.getComponentSetDescriptor();

        String source = set.getSource();

        // looking into jars only
        if ( source != null && source.startsWith( "jar:file:" ) && source.endsWith( "!/META-INF/plexus/components.xml" ) )
        {
            ArrayList<ComponentDescriptor<?>> toRemove = new ArrayList<ComponentDescriptor<?>>();

            for ( Iterator<ComponentDescriptor<?>> i = set.getComponents().iterator(); i.hasNext(); )
            {
                ComponentDescriptor<?> comp = i.next();

                if ( PlexusResource.class.getName().equals( comp.getRole() ) )
                {
                    toRemove.add( comp );

                    System.out.println( "PlexusResource removed (impl='" + comp.getImplementation() + "'), came from "
                        + event.getComponentSetDescriptor().getSource() );
                }
                // else if ( ManagedPlexusResource.class.getName().equals( comp.getRole() ) )
                // {
                // toRemove.add( comp );
                //
                // System.out.println( "ManagedPlexusResource removed (impl='" + comp.getImplementation() +
                // "'), came from "
                // + event.getComponentSetDescriptor().getSource() );
                // }
            }

            set.getComponents().removeAll( toRemove );
        }
    }

    public String getId()
    {
        return "Inhibit";
    }

}
