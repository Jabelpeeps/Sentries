package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;


public class MobTypeTarget extends AbstractTargetType implements TargetType.Internal {

    private final EntityType type;
    
    public MobTypeTarget( EntityType target ) { 
        super( 15 );
        type = target;
        targetString = "MobType:" + type.toString(); 
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return  entity instanceof Creature 
                && entity.getType().equals( type );
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null 
                && o instanceof MobTypeTarget 
                && type.equals( ((MobTypeTarget) o).type );
    }
    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
