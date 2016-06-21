package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;


public class AllMonstersTarget extends AbstractTargetType implements TargetType.Internal {

    public AllMonstersTarget() { super( 3 ); }

    @Override
    public boolean includes( LivingEntity entity ) {
        return entity instanceof Monster;
    }
    @Override
    public String getTargetString() { 
        return "All:Monsters"; 
    }
    @Override
    public boolean equals( Object o ) {
        return o != null && o instanceof AllMonstersTarget;
    }
    @Override
    public int hashCode() {
        return 2;
    }
}
