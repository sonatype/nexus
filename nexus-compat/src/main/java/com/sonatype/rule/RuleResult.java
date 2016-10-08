package com.sonatype.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleResult
{
    private Rule rule;

    private List<String> failures = new ArrayList<String>();

    private List<String> successes = new ArrayList<String>();

    public RuleResult( Rule rule )
    {
        this.rule = rule;
    }

    public Rule getRule()
    {
        return rule;
    }

    public void addFailure( String msg )
    {
        failures.add( msg );
    }

    public void addSuccess( String msg )
    {
        successes.add( msg );
    }

    public boolean isSuccessful()
    {
        return failures.isEmpty();
    }

    public List<String> getFailures()
    {
        return Collections.unmodifiableList( failures );
    }

    public List<String> getSuccesses()
    {
        return Collections.unmodifiableList( successes );
    }

}
