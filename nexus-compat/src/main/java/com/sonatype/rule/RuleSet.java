package com.sonatype.rule;

import java.util.Set;

public interface RuleSet<E extends Rule>
    extends Set<E>
{
    String getId();

    String getName();

    String getDescription();

    RuleSetResult<E> evaluate();
}
