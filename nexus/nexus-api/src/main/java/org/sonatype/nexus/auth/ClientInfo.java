/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
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
package org.sonatype.nexus.auth;

/**
 * Client info about WHO is being doing something.
 * 
 * @author cstamas
 */
public class ClientInfo
{
    private final String userid;

    private final String remoteIP;

    private final String userAgent;

    public ClientInfo( final String userid, final String remoteIP, final String userAgent )
    {
        this.userid = userid;
        this.remoteIP = remoteIP;
        this.userAgent = userAgent;
    }

    public String getRemoteIP()
    {
        return remoteIP;
    }

    public String getUserid()
    {
        return userid;
    }

    public String getUserAgent()
    {
        return userAgent;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( remoteIP == null ) ? 0 : remoteIP.hashCode() );
        result = prime * result + ( ( userAgent == null ) ? 0 : userAgent.hashCode() );
        result = prime * result + ( ( userid == null ) ? 0 : userid.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        ClientInfo other = (ClientInfo) obj;
        if ( remoteIP == null )
        {
            if ( other.remoteIP != null )
                return false;
        }
        else if ( !remoteIP.equals( other.remoteIP ) )
            return false;
        if ( userAgent == null )
        {
            if ( other.userAgent != null )
                return false;
        }
        else if ( !userAgent.equals( other.userAgent ) )
            return false;
        if ( userid == null )
        {
            if ( other.userid != null )
                return false;
        }
        else if ( !userid.equals( other.userid ) )
            return false;
        return true;
    }
}
