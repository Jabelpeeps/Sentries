package org.jabelpeeps.sentries;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        
        for ( EntityType each : EntityType.values() ) {
            EnumSet<SentinelTarget> targets = EnumSet.copyOf( SentinelTarget.forEntityType( each ) );
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
        sentry.arrowRate = sentinel.attackRate / 20;
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
            CommandHandler.getCommand( "target" ).call( null, npc.getName(), sentry, 0, "target", "add", "named", "npc", each );
        }
        for ( String each : sentinel.groupTargets ) {
            CommandHandler.getCommand( "group" ).call( null, npc.getName(), sentry, 0, "group", "target", each );            
        }
        for ( String each : sentinel.playerNameTargets ) {
            CommandHandler.getCommand( "target" ).call( null, npc.getName(), sentry, 0, "target", "add", "named", "player", each );        
        }
        for ( String each : sentinel.eventTargets ) {
            CommandHandler.getCommand( "event" ).call( null, npc.getName(), sentry, 0, "event", "add", each );        
        }
        for ( String each : sentinel.otherTargets ) {
            if ( each.startsWith( "factions:" ) )
                CommandHandler.getCommand( "factions" ).call( null, npc.getName(), sentry, 0, "factions", "target", each );  
            else if ( each.startsWith( "towny:" ) )
                CommandHandler.getCommand( "towny" ).call( null, npc.getName(), sentry, 0, "towny", "target", each );
            else if ( each.startsWith( "sbteam:" ) )
                CommandHandler.getCommand( "scoreboard" ).call( null, npc.getName(), sentry, 0, "scoreboard", "target", each );
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
                    UUID uuid = npc.getTrait( Owner.class ).getOwnerId();
                    if ( uuid != null )
                        sentry.ignores.add( new OwnerTarget( uuid ) ); break;
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
            CommandHandler.getCommand( "ignore" ).call( null, npc.getName(), sentry, 0, "ignore", "add", "named", "npc", each );
        }
        for ( String each : sentinel.groupIgnores ) {
            CommandHandler.getCommand( "group" ).call( null, npc.getName(), sentry, 0, "group", "ignore", each );            
        }
        for ( String each : sentinel.playerNameIgnores ) {
            CommandHandler.getCommand( "ignore" ).call( null, npc.getName(), sentry, 0, "ignore", "add", "named", "player", each );        
        }
        for ( String each : sentinel.otherIgnores ) {
            if ( each.startsWith( "factions:" ) )
                CommandHandler.getCommand( "factions" ).call( null, npc.getName(), sentry, 0, "factions", "ignore", each );  
            else if ( each.startsWith( "towny:" ) )
                CommandHandler.getCommand( "towny" ).call( null, npc.getName(), sentry, 0, "towny", "ignore", each );
            else if ( each.startsWith( "sbteam:" ) )
                CommandHandler.getCommand( "scoreboard" ).call( null, npc.getName(), sentry, 0, "scoreboard", "ignore", each );
        }
        /*
        These ignore specifiers are not implemented in Sentinel yet and so can't be imported
        "NATION", "WARTEAM", "CLAN"
        */ 
        
        npc.removeTrait( SentinelTrait.class );
        
        return true;
    }
}
