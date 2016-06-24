package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class NamedPlayerTarget extends AbstractTargetType implements TargetType.Internal {

    private UUID uuid;
    
    public NamedPlayerTarget( UUID player ) { 
        super( 10 ); 
        uuid = player;
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return  entity instanceof Player
                && !entity.hasMetadata( "NPC" )
                && uuid.equals( entity.getUniqueId() );
    }  
    @Override
    public String getTargetString() { 
        return String.join( ":", "Named", "Player", uuid.toString() ); 
    }
    @Override
    public String getPrettyString() {
        return String.join( ":", "Named", "Player", Bukkit.getPlayer( uuid ).getName() );
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null 
                && o instanceof NamedPlayerTarget 
                && uuid.equals( ((NamedPlayerTarget) o).uuid );
    }
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
