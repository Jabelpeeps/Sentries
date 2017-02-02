package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;


public class OwnerTarget extends AbstractTargetType implements TargetType.Internal {

    private UUID owner;
    
    private OwnerTarget() {
        super( 10 );
        targetString = "Owner";
    }
    public OwnerTarget( String name ) {
        this();
        owner = null;
        prettyString = "The Owner of this NPC:- " + name;
    }   
    public OwnerTarget( UUID uuid ) { 
        this(); 
        owner = uuid;
        prettyString = "The Owner of this NPC:- " + Bukkit.getPlayer( uuid ).getName();
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
