package com.sonatype.rule;

import java.util.Map;

public interface Rule
{
    String getName();

    RuleType getRuleType();

    RuleResult evaluate();

    Map<String, String> getProperties();
}
