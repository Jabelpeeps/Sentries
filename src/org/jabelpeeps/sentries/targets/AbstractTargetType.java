package org.jabelpeeps.sentries.targets;

public abstract class AbstractTargetType implements TargetType {

    protected int order;
    protected String targetString;
    
    protected AbstractTargetType( int i ) {
        order = i;
    }
    @Override
    public final int compareTo( TargetType o ) {
        return order - ((AbstractTargetType) o).order;
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
    public abstract boolean equals( Object o );
    
    @Override
    public abstract int hashCode();
}
