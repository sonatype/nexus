package com.sonatype.rule;

import java.util.List;

public interface RuleType
{
    String getId();

    String getName();

    String getDescription();

    List<RulePropertyDescriptor> getPropertyDescriptors();

    void addPropertyDescriptor( RulePropertyDescriptor propertyDescriptor );

    void setPropertyDescriptors( List<RulePropertyDescriptor> propertyDescriptors );
}
