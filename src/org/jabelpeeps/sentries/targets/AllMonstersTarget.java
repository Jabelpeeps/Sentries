package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;


public class AllMonstersTarget extends AbstractTargetType implements TargetType.Internal {

    public AllMonstersTarget() { 
        super( 4 );
        targetString = "All:Monsters";
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return entity instanceof Monster;
    }
    @Override
    public boolean equals( Object o ) {
        return o != null && o instanceof AllMonstersTarget;
    }
}
