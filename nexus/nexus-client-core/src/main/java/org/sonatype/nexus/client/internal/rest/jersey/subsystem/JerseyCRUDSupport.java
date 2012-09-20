/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.client.internal.rest.jersey.subsystem;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.CRUDSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

/**
 * Implementation for standard CRUD (Create,Read,Update,Delete) REST services. The class should be inherited by the
 * final class because it does some "generics magic" in {@link #getGenericClass(int)} which only works for inherited
 * classes that specify all 4 generic type paramters (RESOURCE_TYPE, LIST_RESPONSE, ITEM_RESPONSE, ITEM_REQUEST).
 * 
 * @author sherold
 * @param <RESOURCE_TYPE> resource type
 * @param <LIST_RESPONSE> type of list response
 * @param <ITEM_RESPONSE> type of a single response
 * @param <ITEM_REQUEST> type of a single request
 */
public abstract class JerseyCRUDSupport<RESOURCE_TYPE, LIST_RESPONSE, ITEM_RESPONSE, ITEM_REQUEST>
    extends SubsystemSupport<JerseyNexusClient>
    implements CRUDSupport<RESOURCE_TYPE>
{
    protected final String basePath;

    protected Type[] genericTypes;

    protected JerseyCRUDSupport( JerseyNexusClient nexusClient, String basePath )
    {
        super( nexusClient );
        this.basePath = basePath;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public List<RESOURCE_TYPE> list()
    {
        final LIST_RESPONSE response =
            getNexusClient().serviceResource( basePath ).get( (Class<LIST_RESPONSE>) getGenericClass( 1 ) );
        return getListData( response );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public RESOURCE_TYPE get( String id )
    {
        final ITEM_RESPONSE response =
            getNexusClient().serviceResource( itemPath( id ) ).get( (Class<ITEM_RESPONSE>) getGenericClass( 2 ) );
        return getData( response );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public RESOURCE_TYPE create( RESOURCE_TYPE item )
    {
        final ITEM_RESPONSE response =
            getNexusClient().serviceResource( basePath ).post( (Class<ITEM_RESPONSE>) getGenericClass( 2 ),
                createRequest( item ) );
        return getData( response );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public RESOURCE_TYPE update( RESOURCE_TYPE item )
    {
        final ITEM_RESPONSE response =
            getNexusClient().serviceResource( itemPath( getId( item ) ) ).post(
                (Class<ITEM_RESPONSE>) getGenericClass( 2 ), createRequest( item ) );
        return getData( response );
    }

    @Override
    public void delete( String id )
    {
        getNexusClient().serviceResource( itemPath( id ) ).delete();
    }

    protected abstract String getId( RESOURCE_TYPE item );

    protected abstract List<RESOURCE_TYPE> getListData( LIST_RESPONSE response );

    protected abstract RESOURCE_TYPE getData( ITEM_RESPONSE response );

    protected abstract void setData( ITEM_REQUEST request, RESOURCE_TYPE item );

    protected Class<?> getGenericClass( int position )
    {
        if ( genericTypes == null )
        {
            final ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
            genericTypes = superclass.getActualTypeArguments();
        }

        return (Class<?>) genericTypes[position];
    }

    @SuppressWarnings( "unchecked" )
    protected ITEM_REQUEST createRequest(RESOURCE_TYPE item) {
        ITEM_REQUEST request;
        try
        {
            request = ( (Class<ITEM_REQUEST>) getGenericClass( 3 ) ).newInstance();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Could not instanciate request class.", e );
        }
        setData(request, item);
        return request;
    }

    protected String itemPath( String id )
    {
        return basePath + "/" + id;
    }
}
