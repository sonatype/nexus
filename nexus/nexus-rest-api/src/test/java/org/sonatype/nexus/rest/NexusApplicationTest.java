package org.sonatype.nexus.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.web.ProtectedPathManager;

public class NexusApplicationTest
{

    private NexusApplication nexusApp;

    private ProtectedPathManager pathManager;

    @Before
    public void createApp()
    {
        nexusApp = new NexusApplication();
        pathManager = mock( ProtectedPathManager.class );
        nexusApp.setProtectedPathManager( pathManager );
    }

    @Test
    public void regularResource()
    {
        PlexusResource resource = mock( PlexusResource.class );
        PathProtectionDescriptor desc = mock( PathProtectionDescriptor.class );
        when( resource.getResourceProtection() ).thenReturn( desc );

        nexusApp.handlePlexusResourceSecurity( resource );

        verify( resource ).getResourceProtection();
        verify( desc ).getFilterExpression();
        verify( desc ).getPathPattern();
        verify( pathManager ).addProtectedResource( "/service/*null", null );
        verifyNoMoreInteractions( resource, desc, pathManager );
    }

    @Test
    public void regularAdvancedSecurityResource()
    {
        AdvancedSecurityPlexusResource resource = mock( AdvancedSecurityPlexusResource.class );
        PathProtectionDescriptor desc1 = mock( PathProtectionDescriptor.class );
        PathProtectionDescriptor desc2 = mock( PathProtectionDescriptor.class );
        when( resource.getResourceProtections() ).thenReturn( new PathProtectionDescriptor[] { desc1, desc2 } );

        nexusApp.handlePlexusResourceSecurity( resource );

        verify( resource, never() ).getResourceProtection();
        verify( resource ).getResourceProtections();
        verify( desc1 ).getFilterExpression();
        verify( desc1 ).getPathPattern();
        verify( desc2 ).getFilterExpression();
        verify( desc2 ).getPathPattern();
        verify( pathManager, times( 2 ) ).addProtectedResource( "/service/*null", null );
        verifyNoMoreInteractions( resource, desc1, desc2, pathManager );
    }

}
