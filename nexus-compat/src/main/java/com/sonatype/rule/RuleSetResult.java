package com.sonatype.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleSetResult<E extends Rule>
{
    private RuleSet<E> ruleSet;

    private List<RuleResult> failures = new ArrayList<RuleResult>();

    private List<RuleResult> successes = new ArrayList<RuleResult>();

    public RuleSetResult( RuleSet<E> ruleSet )
    {
        this.ruleSet = ruleSet;
    }

    public void addFailure( RuleResult ruleResult )
    {
        this.failures.add( ruleResult );
    }

    public void addSuccess( RuleResult ruleResult )
    {
        this.successes.add( ruleResult );
    }

    public boolean isSuccessful()
    {
        return this.failures.isEmpty();
    }

    public List<RuleResult> getFailures()
    {
        return Collections.unmodifiableList( failures );
    }

    public List<RuleResult> getSuccesses()
    {
        return Collections.unmodifiableList( successes );
    }

    public RuleSet<E> getRuleSet()
    {
        return ruleSet;
    }

}
