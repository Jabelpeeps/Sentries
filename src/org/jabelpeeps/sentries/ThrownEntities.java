package org.jabelpeeps.sentries;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;

abstract class ThrownEntities {
    
    static Map<Entity, LivingEntity> thrown = new HashMap<>();
    
    static boolean hasThrower( Entity ent ) {
        return thrown.containsKey( ent );
    }
    static LivingEntity getThrower( Entity ent ) {        
        return thrown.get( ent ); 
    }
    
    static void addTNT( TNTPrimed tnt, LivingEntity thrower ) {
        thrown.put( tnt, thrower );
        
        Bukkit.getScheduler()
              .runTaskLaterAsynchronously( Sentries.plugin, () -> thrown.remove( tnt ), tnt.getFuseTicks() + 10 );
    }
    static void addFireball( Fireball fireball, LivingEntity thrower ) {
        thrown.put( fireball, thrower );
        
        Bukkit.getScheduler()
              .runTaskLaterAsynchronously( Sentries.plugin, () -> thrown.remove( fireball ), 60 );
    }    
    static void addEgg( Egg egg, LivingEntity thrower ) {
        thrown.put( egg, thrower );
        
        Bukkit.getScheduler()
              .runTaskLaterAsynchronously( Sentries.plugin, () -> thrown.remove( egg ), 60 );
    }
}
