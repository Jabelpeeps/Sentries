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
    public void setTargetString( String type ) {
        targetString = type;
    }

    @Override
    public String getTargetString() { 
        return targetString; 
    }
}
