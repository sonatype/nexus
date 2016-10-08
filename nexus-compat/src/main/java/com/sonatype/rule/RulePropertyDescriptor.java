package com.sonatype.rule;

public class RulePropertyDescriptor
{
    /** Property Id */
    private String id;

    /** Property Name */
    private String name;

    /** Property Type */
    private String type;

    /** Property Help Text */
    private String helpText;

    /** If this property is required */
    private boolean required = false;

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getHelpText()
    {
        return helpText;
    }

    public void setHelpText( String helpText )
    {
        this.helpText = helpText;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

}
