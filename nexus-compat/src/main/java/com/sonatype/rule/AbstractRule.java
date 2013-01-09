package com.sonatype.rule;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRule
    implements Rule
{
    private String name;

    private RuleType ruleType;

    private Map<String, String> properties = new HashMap<String, String>();

    public AbstractRule( String name, RuleType ruleType )
    {
        this.name = name;

        this.ruleType = ruleType;
    }

    public String getName()
    {
        return name;
    }

    public RuleType getRuleType()
    {
        return ruleType;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

}
