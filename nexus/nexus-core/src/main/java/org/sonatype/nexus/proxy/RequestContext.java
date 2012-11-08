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
package org.sonatype.nexus.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.codehaus.plexus.util.StringUtils;

public class RequestContext
    extends HashMap<String, Object>
    implements Map<String, Object>
{
    private static final long serialVersionUID = 7200598686103502266L;

    /** Context URL of the app root on the incoming connector. */
    public static final String CTX_REQUEST_APP_ROOT_URL = "request.appRootUrl";

    /** Context URL of the original resource requested on the incoming connector. */
    public static final String CTX_REQUEST_URL = "request.url";

    /** Context flag to mark a request local only. */
    public static final String CTX_LOCAL_ONLY_FLAG = "request.localOnly";

    /** Context flag to mark a request local only. */
    public static final String CTX_REMOTE_ONLY_FLAG = "request.remoteOnly";

    /** Context flag to mark a request local only. */
    public static final String CTX_GROUP_LOCAL_ONLY_FLAG = "request.groupLocalOnly";

    /** Context key for condition "if-modified-since" */
    public static final String CTX_CONDITION_IF_MODIFIED_SINCE = "request.condition.ifModifiedSince";

    /** Context key for condition "if-none-match" */
    public static final String CTX_CONDITION_IF_NONE_MATCH = "request.condition.ifNoneMatch";

    /**
     * Context key to mark request as used for auth check only, so repo impl will know there is no work required (i.e.
     * interpolation, etc.)
     */
    public static final String CTX_AUTH_CHECK_ONLY = "request.auth.check.only";

    private RequestContext parent;

    public RequestContext()
    {
        super();

        this.parent = null;
    }

    public RequestContext( RequestContext parent )
    {
        this();

        setParentContext( parent );
    }

    public RequestContext getParentContext()
    {
        return parent;
    }

    public void setParentContext( RequestContext context )
    {
        if ( context != null )
        {
            if ( this == context )
            {
                throw new IllegalArgumentException(
                    "The context cannot be parent of itself! The parent instance cannot equals to this instance!" );
            }
            RequestContext otherParentContext = context.getParentContext();
            while ( otherParentContext != null )
            {
                if ( this == otherParentContext )
                {
                    throw new IllegalArgumentException( "The context cannot be an ancestor of itself! Cycle detected!" );
                }
                otherParentContext = otherParentContext.getParentContext();
            }
        }

        this.parent = context;
    }

    public boolean containsKey( Object key )
    {
        return containsKey( key, true );
    }

    public boolean containsKey( Object key, boolean fallBackToParent )
    {
        boolean result = super.containsKey( key );

        if ( fallBackToParent && !result && getParentContext() != null && getParentContext() != this )
        {
            result = getParentContext().containsKey( key );
        }

        return result;
    }

    public Object get( Object key )
    {
        return get( key, true );
    }

    public Object get( Object key, boolean fallBackToParent )
    {
        if ( containsKey( key, false ) )
        {
            return super.get( key );
        }
        else if ( fallBackToParent && getParentContext() != null && getParentContext() != this )
        {
            return getParentContext().get( key );
        }
        else
        {
            return null;
        }
    }

    /**
     * Checks if is request local only.
     * 
     * @return true, if is request local only
     */
    public boolean isRequestLocalOnly()
    {
        if ( containsKey( CTX_LOCAL_ONLY_FLAG ) )
        {
            return (Boolean) get( CTX_LOCAL_ONLY_FLAG );
        }
        else
        {
            return false;
        }
    }

    /**
     * Sets the request local only.
     * 
     * @param requestLocalOnly the new request local only
     */
    public void setRequestLocalOnly( boolean requestLocalOnly )
    {
        put( CTX_LOCAL_ONLY_FLAG, requestLocalOnly );
    }

    /**
     * Checks if is request remote only.
     * 
     * @return true, if is request remote only
     */
    public boolean isRequestRemoteOnly()
    {
        if ( containsKey( CTX_REMOTE_ONLY_FLAG ) )
        {
            return (Boolean) get( CTX_REMOTE_ONLY_FLAG );
        }
        else
        {
            return false;
        }
    }

    /**
     * Sets the request remote only.
     * 
     * @param requestremoteOnly the new request remote only
     */
    public void setRequestRemoteOnly( boolean requestRemoteOnly )
    {
        put( CTX_REMOTE_ONLY_FLAG, requestRemoteOnly );
    }

    /**
     * Checks if is request group local only.
     * 
     * @return true, if is request group local only
     */
    public boolean isRequestGroupLocalOnly()
    {
        if ( containsKey( CTX_GROUP_LOCAL_ONLY_FLAG ) )
        {
            return (Boolean) get( CTX_GROUP_LOCAL_ONLY_FLAG );
        }
        else
        {
            return false;
        }
    }

    /**
     * Sets the request group local only.
     * 
     * @param requestremoteOnly the new request group local only
     */
    public void setRequestGroupLocalOnly( boolean requestGroupLocal )
    {
        put( CTX_GROUP_LOCAL_ONLY_FLAG, requestGroupLocal );
    }

    /**
     * Returns true if the request is conditional.
     * 
     * @return true if this request is conditional.
     */
    public boolean isConditional()
    {
        return containsKey( CTX_CONDITION_IF_MODIFIED_SINCE ) || containsKey( CTX_CONDITION_IF_NONE_MATCH );
    }

    /**
     * Returns the timestamp to check against.
     * 
     * @return
     */
    public long getIfModifiedSince()
    {
        if ( containsKey( CTX_CONDITION_IF_MODIFIED_SINCE ) )
        {
            return (Long) get( CTX_CONDITION_IF_MODIFIED_SINCE );
        }
        else
        {
            return 0;
        }
    }

    /**
     * Sets the timestamp to check against.
     * 
     * @param ifModifiedSince
     */
    public void setIfModifiedSince( long ifModifiedSince )
    {
        if ( ifModifiedSince != 0 )
        {
            put( CTX_CONDITION_IF_MODIFIED_SINCE, Long.valueOf( ifModifiedSince ) );
        }
        else
        {
            remove( CTX_CONDITION_IF_MODIFIED_SINCE );
        }
    }

    /**
     * Gets the ETag (SHA1 in Nexus) to check item against.
     * 
     * @return
     */
    public String getIfNoneMatch()
    {
        return (String) get( CTX_CONDITION_IF_NONE_MATCH );
    }

    /**
     * Sets the ETag (SHA1 in Nexus) to check item against.
     * 
     * @param tag
     */
    public void setIfNoneMatch( String tag )
    {
        if ( !StringUtils.isEmpty( tag ) )
        {
            put( CTX_CONDITION_IF_NONE_MATCH, tag );
        }
        else
        {
            remove( CTX_CONDITION_IF_NONE_MATCH );
        }
    }

    /**
     * Returns the URL of the original request.
     * 
     * @return
     */
    public String getRequestUrl()
    {
        return (String) get( CTX_REQUEST_URL );
    }

    /**
     * Sets the URL of the original request.
     * 
     * @param url
     */
    public void setRequestUrl( String url )
    {
        if ( !StringUtils.isBlank( url ) )
        {
            put( CTX_REQUEST_URL, url );
        }
        else
        {
            remove( CTX_REQUEST_URL );
        }
    }

    /**
     * Returns the URL of the AppRoot of the incoming request.
     * 
     * @return
     */
    public String getRequestAppRootUrl()
    {
        return (String) get( CTX_REQUEST_APP_ROOT_URL );
    }

    /**
     * Sets the URL of the AppRoot of the incoming request.
     * 
     * @param url
     */
    public void setRequestAppRootUrl( String url )
    {
        if ( !StringUtils.isBlank( url ) )
        {
            put( CTX_REQUEST_APP_ROOT_URL, url );
        }
        else
        {
            remove( CTX_REQUEST_APP_ROOT_URL );
        }
    }

    // ==

    /**
     * Returns a new map instance that contains flattened RequestContext as it is "viewed" by callers (overlaid in
     * proper order).
     */
    public Map<String, Object> flatten()
    {
        HashMap<String, Object> result = new HashMap<String, Object>();

        RequestContext ctx = this;

        Stack<RequestContext> stack = new Stack<RequestContext>();

        while ( ctx != null )
        {
            stack.push( ctx );

            ctx = ctx.getParentContext();
        }

        while ( !stack.isEmpty() )
        {
            ctx = stack.pop();

            result.putAll( ctx );
        }

        return result;
    }
}
