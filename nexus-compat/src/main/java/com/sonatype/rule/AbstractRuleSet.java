package com.sonatype.rule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractRuleSet<E extends Rule>
    implements RuleSet<E>
{
    private Set<E> rules = new HashSet<E>();

    private String id;

    private String name;

    private String description;

    public AbstractRuleSet( String id, String name )
    {
        this( id, name, "" );
    }

    public AbstractRuleSet( String id, String name, String description )
    {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public RuleSetResult<E> evaluate()
    {
        RuleSetResult<E> result = new RuleSetResult<E>( this );

        for ( E rule : rules )
        {
            RuleResult ruleResult = rule.evaluate();

            if ( ruleResult.isSuccessful() )
            {
                result.addSuccess( ruleResult );
            }
            else
            {
                result.addFailure( ruleResult );
            }
        }

        return result;
    }

    public boolean add( E e )
    {
        return rules.add( e );
    }

    public boolean addAll( Collection<? extends E> c )
    {
        return rules.addAll( c );
    }

    public void clear()
    {
        rules.clear();
    }

    public boolean contains( Object o )
    {
        return rules.contains( o );
    }

    public boolean containsAll( Collection<?> c )
    {
        return rules.containsAll( c );
    }

    public boolean isEmpty()
    {
        return rules.isEmpty();
    }

    public Iterator<E> iterator()
    {
        return rules.iterator();
    }

    public boolean remove( Object o )
    {
        return rules.remove( o );
    }

    public boolean removeAll( Collection<?> c )
    {
        return rules.removeAll( c );
    }

    public boolean retainAll( Collection<?> c )
    {
        return rules.retainAll( c );
    }

    public int size()
    {
        return rules.size();
    }

    public Object[] toArray()
    {
        return rules.toArray();
    }

    public <T> T[] toArray( T[] a )
    {
        return rules.toArray( a );
    }

}
