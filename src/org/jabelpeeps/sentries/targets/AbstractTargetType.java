package org.jabelpeeps.sentries.targets;

public abstract class AbstractTargetType implements TargetType {

    protected int order;
    protected String targetString;
    
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
    public String getTargetString() { 
        return targetString; 
    }
    @Override
    public String getPrettyString() {
        return targetString;
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
