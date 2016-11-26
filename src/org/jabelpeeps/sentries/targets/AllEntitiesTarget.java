package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;

/**
 *  A TargetType that will always return true to {@link#includes()} to 
 *  include all LivingEntities within its definition.
 */
public class AllEntitiesTarget extends AbstractTargetType implements TargetType.Internal {

    public AllEntitiesTarget() { 
        super( 1 ); 
        targetString = "All:Entities";
    }    
    @Override
    public boolean includes( LivingEntity entity ) {
        return true;
    }
    @Override
    public boolean equals( Object o ) {         
        return o != null && o instanceof AllEntitiesTarget;
    }
}
