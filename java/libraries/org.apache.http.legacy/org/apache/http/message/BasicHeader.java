package org.apache.http.message;

import org.apache.http.Header;

public class BasicHeader implements Header, Cloneable
{
    private final String name;
    private final String value;

    public BasicHeader(String name, String value)
    {
        if (name == null) throw new IllegalArgumentException("Name may not be null");

        this.name = name;
        this.value = value;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return value == null ? name + ": " : name + ": " + value;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
