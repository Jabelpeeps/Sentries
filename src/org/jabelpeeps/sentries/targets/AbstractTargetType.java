package org.jabelpeeps.sentries.targets;

import lombok.Getter;

public abstract class AbstractTargetType implements TargetType {

    protected int order;
    @Getter protected String targetString;
    protected String prettyString;
    
    protected AbstractTargetType( int i ) {
        order = i;
    }
    @Override
    public final int compareTo( TargetType o ) {
        int same = order - ((AbstractTargetType) o).order;
        return ( same != 0 ) ? same : hashCode() - o.hashCode();
    }   
    @Override
    public TargetType setTargetString( String type ) {
        targetString = type;
        return this;
    }
    @Override
    public TargetType setPrettyString( String pretty ) {
        prettyString = pretty;
        return this;
    }
    @Override
    public String getPrettyString() {
        return prettyString == null ? targetString : prettyString;
    }    
    @Override
    public int hashCode() {
        return order;
    }
    @Override
    public abstract boolean equals( Object o );
    
    @Override
    public String toString() {
        return getPrettyString();
    }
}
