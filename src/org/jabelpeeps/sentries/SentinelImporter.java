package org.jabelpeeps.sentries;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.jabelpeeps.sentries.targets.AllEntitiesTarget;
import org.jabelpeeps.sentries.targets.AllMobsTarget;
import org.jabelpeeps.sentries.targets.AllMonstersTarget;
import org.jabelpeeps.sentries.targets.AllNPCsTarget;
import org.jabelpeeps.sentries.targets.AllPlayersTarget;
import org.jabelpeeps.sentries.targets.MobTypeTarget;
import org.jabelpeeps.sentries.targets.OwnerTarget;
import org.mcmonkey.sentinel.SentinelTarget;
import org.mcmonkey.sentinel.SentinelTrait;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public class SentinelImporter {
    
    private final static Map<SentinelTarget, EntityType> targetMap = new HashMap<>();
    
    static {
        EnumSet<SentinelTarget> ignored = EnumSet.of( SentinelTarget.MOBS, SentinelTarget.MONSTERS, SentinelTarget.PASSIVE_MOB );
        
        Set<SentinelTarget> targets = new HashSet<>();               
        for ( EntityType each : EntityType.values() ) {
            targets.clear();
            targets.addAll( SentinelTarget.forEntityType( each ) );
            targets.removeAll( ignored );
            
            if ( targets.isEmpty() || targets.size() > 1 ) continue;
            
            targetMap.put( targets.iterator().next(), each );
        }
    } 
    
    public static int importAll() {
        int convertedCount = 0;
        
        for ( NPC npc : CitizensAPI.getNPCRegistry() ) {
            if ( importNPC( npc ) )
                convertedCount++;
        }
        return convertedCount;
    }   
    
    public static boolean importNPC( NPC npc ) {
        if ( !npc.hasTrait( SentinelTrait.class ) ) return false;
        
        SentinelTrait sentinel = npc.getTrait( SentinelTrait.class );
        SentryTrait sentry = npc.getTrait( SentryTrait.class );
        
        sentry.armour = sentinel.armor;
        sentry.attackRate = sentinel.attackRate / 20;
        sentry.range = (int) Math.max( sentinel.chaseRange, sentinel.range );
        sentry.strength = (int) sentinel.damage;
        sentry.killsDrop = sentinel.enemyDrops;
        sentry.iRetaliate = sentinel.fightback;
        sentry.healRate = sentinel.healRate;
        sentry.setHealth( sentinel.health );
        sentry.invincible = sentinel.invincible;
        sentry.respawnDelay = (int) ( sentinel.respawnTime / 20 );
        sentry.spawnLocation = sentinel.spawnPoint;
        sentry.guardeeEntity = Bukkit.getPlayer( sentinel.getGuarding() );
        
        if ( sentry.guardeeEntity != null ) 
            sentry.guardeeName = sentry.guardeeEntity.getName();
        
        // Import targets
        if  (   sentinel.targets.contains( SentinelTarget.MOBS ) 
                && sentinel.targets.contains( SentinelTarget.PLAYERS )
                && sentinel.targets.contains( SentinelTarget.NPCS ) ) {
            sentry.targets.add( new AllEntitiesTarget() );
        }
        else {    
            for ( SentinelTarget each : sentinel.targets ) {
                switch( each ) {
                    case MONSTERS:
                        sentry.targets.add( new AllMonstersTarget() ); break;
                    case MOBS:
                        sentry.targets.add( new AllMobsTarget() ); break;
                    case PLAYERS:
                        sentry.targets.add( new AllPlayersTarget() ); break;
                    case NPCS:
                        sentry.targets.add( new AllNPCsTarget() ); break;
                    default:
                        EntityType type = targetMap.get( each );
                        if ( type != null )
                            sentry.targets.add( new MobTypeTarget( type ) );
                        break;
                    case PASSIVE_MOB: 
                        // What use is targeting only passive mobs?
                    case OWNER:
                }
            }
        }
        for ( String each : sentinel.npcNameTargets ) {
            CommandHandler.callCommand( sentry, "target", "add", "named:npc:" + each );
        }
        for ( String each : sentinel.groupTargets ) {
            CommandHandler.callCommand( sentry, "group", "target", each );            
        }
        for ( String each : sentinel.playerNameTargets ) {
            CommandHandler.callCommand( sentry, "target", "add", "named:player:" + each );        
        }
        for ( String each : sentinel.eventTargets ) {
            CommandHandler.callCommand( sentry, "event", "add", each );        
        }
        for ( String each : sentinel.otherTargets ) {
            if ( each.startsWith( "factions:" ) )
                CommandHandler.callCommand( sentry, "factions", "target", each );  
            else if ( each.startsWith( "towny:" ) )
                CommandHandler.callCommand( sentry, "towny", "target", each );
            else if ( each.startsWith( "sbteam:" ) )
                CommandHandler.callCommand( sentry, "scoreboard", "target", each );
        }
        /*
        These target specifiers are not implemented in Sentinel yet and so can't be imported
        "FACTIONENEMIES", "NATIONENEMIES", "NATION", "WARTEAM", "CLAN"
        */
                
        for ( SentinelTarget each : sentinel.ignores ) {
            switch( each ) {
                case MONSTERS:
                    sentry.ignores.add( new AllMonstersTarget() ); break;
                case MOBS:
                    sentry.ignores.add( new AllMobsTarget() ); break;
                case PLAYERS:
                    sentry.ignores.add( new AllPlayersTarget() ); break;
                case NPCS:
                    sentry.ignores.add( new AllNPCsTarget() ); break;
                case OWNER:
                    sentry.ignores.add( new OwnerTarget( npc.getTrait( Owner.class ) ) ); break;
                default:
                    EntityType type = targetMap.get( each );
                    if ( type != null )
                        sentry.ignores.add( new MobTypeTarget( type ) );
                    break;
                case PASSIVE_MOB: 
                    // What use is ignoring only passive mobs? Just target only the hostile mobs.
                    break;
            }
        }
        for ( String each : sentinel.npcNameIgnores ) {
            CommandHandler.callCommand( sentry, "ignore", "add", "named:npc:" + each );
        }
        for ( String each : sentinel.groupIgnores ) {
            CommandHandler.callCommand( sentry, "group", "ignore", each );            
        }
        for ( String each : sentinel.playerNameIgnores ) {
            CommandHandler.callCommand( sentry, "ignore", "add", "named:player:" + each );        
        }
        for ( String each : sentinel.otherIgnores ) {
            if ( each.startsWith( "factions:" ) )
                CommandHandler.callCommand( sentry, "factions", "ignore", each );  
            else if ( each.startsWith( "towny:" ) )
                CommandHandler.callCommand( sentry, "towny", "ignore", each );
            else if ( each.startsWith( "sbteam:" ) )
                CommandHandler.callCommand( sentry, "scoreboard", "ignore", each );
        }
        /*
        These ignore specifiers are not implemented in Sentinel yet and so can't be imported
        "NATION", "WARTEAM", "CLAN"
        */ 
        
        npc.removeTrait( SentinelTrait.class );
        
        return true;
    }
}
