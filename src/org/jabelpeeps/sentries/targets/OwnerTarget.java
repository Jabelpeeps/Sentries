package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;


public class OwnerTarget extends AbstractTargetType implements TargetType.Internal {

    private final UUID owner;
    
    public OwnerTarget( UUID uuid ) { 
        super( 10 ); 
        owner = uuid;
        targetString = "Owner";
        prettyString = "The Onwer of this NPC:- " + Bukkit.getPlayer( uuid ).getName();
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        UUID uuid = entity.getUniqueId();
        
        return uuid != null && uuid.equals( owner );
    }   
    @Override
    public boolean equals( Object o ) {
        return  o != null 
                && o instanceof OwnerTarget 
                && owner.equals( ((OwnerTarget) o).owner );
    }
    @Override
    public int hashCode() {
        return owner.hashCode();
    }
}
