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
package org.sonatype.nexus.plugins.capabilities.support.activation;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.eventbus.NexusEventBus;
import org.sonatype.nexus.plugins.capabilities.api.activation.Condition;
import org.sonatype.nexus.plugins.capabilities.internal.activation.InversionCondition;

/**
 * Factory of logical {@link Condition}s.
 *
 * @since 1.10.0
 */
@Named
@Singleton
public class LogicalConditions
{

    private final NexusEventBus eventBus;

    @Inject
    public LogicalConditions( final NexusEventBus eventBus )
    {
        this.eventBus = checkNotNull( eventBus );
    }

    /**
     * Creates a new condition that is satisfied when both conditions are satisfied (logical AND).
     *
     * @param conditions to be AND-ed
     * @return created condition
     */
    public Condition and( final Condition... conditions )
    {
        return new ConjunctionCondition( eventBus, conditions );
    }

    /**
     * Creates a new condition that is satisfied when at least one condition is satisfied (logical OR).
     *
     * @param conditions to be OR-ed
     * @return created condition
     */
    public Condition or( final Condition... conditions )
    {
        return new DisjunctionCondition( eventBus, conditions );
    }

    /**
     * Creates a new condition that is satisfied when at another condition is not satisfied (logical NOT).
     *
     * @param condition negated condition
     * @return created condition
     */
    public Condition not( final Condition condition )
    {
        return new InversionCondition( eventBus, condition );
    }

    /**
     * A condition that applies a logical AND between conditions.
     *
     * @since 1.10.0
     */
    private static class ConjunctionCondition
        extends AbstractCompositeCondition
        implements Condition
    {

        private Condition lastNotSatisfied;

        public ConjunctionCondition( final NexusEventBus eventBus,
                                     final Condition... conditions )
        {
            super( eventBus, conditions );
        }

        @Override
        protected boolean reevaluate( final Condition... conditions )
        {
            for ( final Condition condition : conditions )
            {
                if ( !condition.isSatisfied() )
                {
                    lastNotSatisfied = condition;
                    return false;
                }
            }
            lastNotSatisfied = null;
            return true;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            for ( final Condition condition : getConditions() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( " AND " );
                }
                sb.append( condition );
            }
            return sb.toString();
        }

        @Override
        public String explainSatisfied()
        {
            final StringBuilder sb = new StringBuilder();
            for ( final Condition condition : getConditions() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( " AND " );
                }
                sb.append( condition.explainSatisfied() );
            }
            return sb.toString();
        }

        @Override
        public String explainUnsatisfied()
        {
            if ( lastNotSatisfied != null )
            {
                return lastNotSatisfied.explainUnsatisfied();
            }
            final StringBuilder sb = new StringBuilder();
            for ( final Condition condition : getConditions() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( " OR " );
                }
                sb.append( condition.explainUnsatisfied() );
            }
            return sb.toString();
        }
    }

    /**
     * A condition that applies a logical OR between conditions.
     *
     * @since 1.10.0
     */
    private static class DisjunctionCondition
        extends AbstractCompositeCondition
        implements Condition
    {

        private Condition lastSatisfied;

        public DisjunctionCondition( final NexusEventBus eventBus,
                                     final Condition... conditions )
        {
            super( eventBus, conditions );
        }

        @Override
        protected boolean reevaluate( final Condition... conditions )
        {
            for ( final Condition condition : conditions )
            {
                if ( condition.isSatisfied() )
                {
                    lastSatisfied = condition;
                    return true;
                }
            }
            lastSatisfied = null;
            return false;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            for ( final Condition condition : getConditions() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( " OR " );
                }
                sb.append( condition );
            }
            return sb.toString();
        }

        @Override
        public String explainSatisfied()
        {
            if(lastSatisfied !=null)
            {
                return lastSatisfied.explainSatisfied();
            }
            final StringBuilder sb = new StringBuilder();
            for ( final Condition condition : getConditions() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( " OR " );
                }
                sb.append( condition.explainSatisfied() );
            }
            return sb.toString();
        }

        @Override
        public String explainUnsatisfied()
        {
            final StringBuilder sb = new StringBuilder();
            for ( final Condition condition : getConditions() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( " AND " );
                }
                sb.append( condition.explainUnsatisfied() );
            }
            return sb.toString();
        }

    }

}
