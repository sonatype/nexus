package com.sonatype.rule;

import java.util.Collection;

public interface RuleFactory
{
    /**
     * Get all the rule types supported by this rule factory
     * 
     * @return a collection of supported rule types
     */
    Collection<? extends RuleType> getSupportedRuleTypes();
}
