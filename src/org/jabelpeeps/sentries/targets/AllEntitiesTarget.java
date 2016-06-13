package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;

/**
 *  A TargetType that will always return true to {@link#includes()} to 
 *  include all LivingEntities within its definition.
 */
public class AllEntitiesTarget extends AbstractTargetType {

    AllEntitiesTarget() { super( 1 ); }
    
    @Override
    public boolean includes( LivingEntity entity ) {
        return true;
    }
    @Override
    public boolean equals( Object o ) { 
        
        if ( o != null && o instanceof AllEntitiesTarget ) return true;
        
        return false;
    }
    @Override
    public int hashCode() {
        return 0;
    }
}
