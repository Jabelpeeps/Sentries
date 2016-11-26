package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;


public class AllMobsTarget extends AbstractTargetType implements TargetType.Internal {

    public AllMobsTarget() {
        super( 3 );
        targetString = "All:Mobs";
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return entity instanceof Creature;
    }
    @Override
    public boolean equals( Object o ) {
        return o != null && o instanceof AllMobsTarget;
    }
}
