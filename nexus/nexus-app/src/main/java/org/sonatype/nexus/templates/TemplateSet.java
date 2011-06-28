/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions.
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
package org.sonatype.nexus.templates;

import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TemplateSet<T extends Template>
    extends HashSet<T>
    implements TemplateProvider
{
    private static final long serialVersionUID = 552419423510140977L;

    private final Object clazz;

    private final TemplateSet<T> parent;

    public TemplateSet( Object clazz )
    {
        this( clazz, null );
    }

    public TemplateSet( Object clazz, TemplateSet<T> templates )
    {
        super();

        this.clazz = clazz;

        this.parent = templates;

        if ( parent != null )
        {
            if ( clazz == null )
            {
                addAll( templates );
            }
            else
            {
                for ( T template : parent )
                {
                    if ( template.targetFits( clazz ) )
                    {
                        add( template );
                    }
                }
            }
        }
    }

    @Override
    public boolean add( T elem )
    {
        if ( getClazz() != null && elem.targetFits( getClazz() ) )
        {
            return super.add( elem );
        }
        else if ( getClazz() == null )
        {
            return super.add( elem );
        }
        else
        {
            return false;
        }
    }

    /**
     * Picks the only one remained template in this set.
     *
     * @return
     * @throws IllegalStateException
     */
    public T pick()
        throws IllegalStateException
    {
        return pick( true );
    }

    /**
     * Picks one template from this set. Will enforce that set has only one member if the forceSingleHit is true.
     *
     * @param forceSingleHit
     * @return
     * @throws IllegalStateException
     */
    public T pick( boolean forceSingleHit )
        throws IllegalStateException
    {
        if ( !forceSingleHit || size() == 1 )
        {
            return iterator().next();
        }
        else
        {
            throw new IllegalStateException( "The TemplateSet has size()==\"" + size() + "\" and not 1 as forced!" );
        }
    }

    public Object getClazz()
    {
        return clazz;
    }

    public TemplateSet<T> getParent()
    {
        return parent;
    }

    public List<T> getTemplatesList()
    {
        return new ArrayList<T>( this );
    }

    public TemplateSet<T> getTemplates()
    {
        return this;
    }

    public TemplateSet<T> getTemplates( Object filter )
    {
        return new TemplateSet( filter, this );
    }

    public TemplateSet<T> getTemplates( Object... filters )
    {
        TemplateSet par = this;

        for ( Object filter : filters )
        {
            par = par.getTemplates( filter );
        }

        return par;
    }

    public T getTemplateById( String id )
        throws NoSuchTemplateIdException
    {
        for ( T template : this )
        {
            if ( StringUtils.equals( id, template.getId() ) )
            {
                return template;
            }
        }

        throw new NoSuchTemplateIdException( "Template with Id='" + id + "' not found!" );
    }
}
