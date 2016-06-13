package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;


public abstract class AbstractTargetType implements TargetType {

    protected int order;
    protected String targetString;
    
    protected AbstractTargetType( int i ) {
        order = i;
    }
    
    @Override
    public abstract boolean includes( LivingEntity entity );
    
    @Override
    public abstract boolean equals( Object o );
    
    @Override
    public abstract int hashCode();

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
