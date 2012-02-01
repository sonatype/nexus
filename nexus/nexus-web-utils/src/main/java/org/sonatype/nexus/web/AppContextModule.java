package org.sonatype.nexus.web;

import org.sonatype.appcontext.AppContext;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;

/**
 * Exposing AppContext as component.
 * 
 * @author cstamas
 * @since 2.0
 */
public class AppContextModule
    extends AbstractModule
{
    private final AppContext appContext;

    public AppContextModule( final AppContext appContext )
    {
        this.appContext = Preconditions.checkNotNull( appContext );
    }

    @Override
    protected void configure()
    {
        bind( AppContext.class ).toInstance( appContext );
    }
}
