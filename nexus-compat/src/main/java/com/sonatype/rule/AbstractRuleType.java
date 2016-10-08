package com.sonatype.rule;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRuleType
    implements RuleType
{

    private List<RulePropertyDescriptor> propertyDescriptors = new ArrayList<RulePropertyDescriptor>();

    public List<RulePropertyDescriptor> getPropertyDescriptors()
    {
        return propertyDescriptors;
    }

    public void setPropertyDescriptors( List<RulePropertyDescriptor> propertyDescriptors )
    {
        this.propertyDescriptors = propertyDescriptors;
    }

    public void addPropertyDescriptor( RulePropertyDescriptor propertyDescriptor )
    {
        propertyDescriptors.add( propertyDescriptor );
    }

}
