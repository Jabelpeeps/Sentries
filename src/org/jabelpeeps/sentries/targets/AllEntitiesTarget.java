package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;

/**
 *  A TargetType that will always return true to {@link#includes()} to 
 *  include all LivingEntities within its definition.
 */
public class AllEntitiesTarget extends AbstractTargetType implements TargetType.Internal {

    public AllEntitiesTarget() { super( 1 ); }
    
    @Override
    public boolean includes( LivingEntity entity ) {
        return true;
    }
    @Override
    public String getTargetString() { 
        return "All:Entities"; 
    }
    
    @Override
    public boolean equals( Object o ) {         
        return o != null && o instanceof AllEntitiesTarget;
    }
    @Override
    public int hashCode() {
        return 0;
    }
}
