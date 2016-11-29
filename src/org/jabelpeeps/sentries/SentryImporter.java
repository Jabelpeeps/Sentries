package org.jabelpeeps.sentries;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.jabelpeeps.sentries.targets.AllEntitiesTarget;
import org.jabelpeeps.sentries.targets.AllMonstersTarget;
import org.jabelpeeps.sentries.targets.AllNPCsTarget;
import org.jabelpeeps.sentries.targets.AllPlayersTarget;
import org.jabelpeeps.sentries.targets.MobTypeTarget;
import org.jabelpeeps.sentries.targets.NamedNPCTarget;
import org.jabelpeeps.sentries.targets.NamedPlayerTarget;
import org.jabelpeeps.sentries.targets.OwnerTarget;

import net.aufdemrand.sentry.SentryInstance;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public class SentryImporter {
        
    public static int importAll() {       
        int convertedCount = 0;
        
        for ( NPC npc : CitizensAPI.getNPCRegistry() ) {
            if ( importNPC( npc ) )
                convertedCount++;
        }
        return convertedCount;
    }
     
    public static boolean importNPC( NPC npc ) {
        if ( !npc.hasTrait( net.aufdemrand.sentry.SentryTrait.class ) ) return false;
        
        SentryInstance sentry = npc.getTrait( net.aufdemrand.sentry.SentryTrait.class ).getInstance();
        if ( sentry == null ) return false;

        SentryTrait newSentry = npc.getTrait( SentryTrait.class );
        
        newSentry.armour = sentry.Armor;
        newSentry.arrowRate = sentry.AttackRateSeconds;
        
        if ( newSentry.arrowRate < 1 ) newSentry.arrowRate = 1;
        else if ( newSentry.arrowRate > 30 ) newSentry.arrowRate = 30;
        
        newSentry.range = sentry.sentryRange;
        newSentry.strength = sentry.Strength;
        newSentry.killsDrop = sentry.KillsDropInventory;
        newSentry.warningMsg = sentry.WarningMessage;
        newSentry.greetingMsg = sentry.GreetingMessage;
        newSentry.acceptsCriticals = sentry.LuckyHits;
        newSentry.iRetaliate = sentry.Retaliate;
        newSentry.healRate = sentry.HealRate;   
        newSentry.setHealth( sentry.sentryHealth );
        newSentry.invincible = sentry.Invincible;
        newSentry.range = sentry.sentryRange;
        newSentry.respawnDelay = sentry.RespawnDelaySeconds;
        newSentry.spawnLocation = sentry.Spawn;
        newSentry.dropInventory = sentry.DropInventory;
        newSentry.followDistance = sentry.FollowDistance;
        newSentry.ignoreLOS = sentry.IgnoreLOS;
        newSentry.nightVision = sentry.NightVision;
        newSentry.speed = sentry.sentrySpeed;
        newSentry.mountID = sentry.MountID;
        newSentry.weight = sentry.sentryWeight;
        newSentry.voiceRange = sentry.WarningRange;
        newSentry.weaponSpecialEffects = sentry.potionEffects;
        
        if ( sentry.guardTarget != null && !sentry.guardTarget.isEmpty() ) {
            newSentry.guardeeName = sentry.guardTarget;
        }
        // Import targets
        for ( String t : sentry.validTargets ) {
            if ( t.contains( "ENTITY:ALL" ) )
                newSentry.targets.add( new AllEntitiesTarget() );
            else if ( t.contains( "ENTITY:MONSTER" ) )
                newSentry.targets.add( new AllMonstersTarget() );
            else if ( t.contains( "ENTITY:PLAYER" ) )
                newSentry.targets.add( new AllPlayersTarget() );
            else if ( t.contains( "ENTITY:NPC" ) )
                newSentry.targets.add( new AllNPCsTarget() );
            else {
                String[] sections = Util.colon.split( t );
                if ( sections.length != 2 ) continue;
                    
                sections[0] = sections[0].trim();
                sections[1] = sections[1].trim();
                
                if ( sections[0].equals( "NPC" ) ) {
                    for ( NPC each : CitizensAPI.getNPCRegistry() ) {
                        if ( each.getName() == sections[1] )
                            newSentry.targets.add( new NamedNPCTarget( each.getUniqueId() ) );
                    }
                }  
                
                else if ( sections[0].equals( "PLAYER" ) ) {
                    UUID uuid = Arrays.stream( Bukkit.getOfflinePlayers() )
                                      .parallel()
                                      .filter( p -> p.getName().equalsIgnoreCase( sections[1] ) )
                                      .map( p -> p.getUniqueId() )
                                      .findAny().get();
                    if ( uuid != null )
                        newSentry.targets.add( new NamedPlayerTarget( uuid ) );
                }
                
                else if ( sections[0].equals( "ENTITY" ) ) {
                    EntityType target = EntityType.valueOf( sections[1].toUpperCase() );
                    if ( target != null )
                        newSentry.targets.add( new MobTypeTarget( target ) );
                }
                
                else if ( sections[0].equals( "GROUP" ) ) {
                    CommandHandler.getCommand( "group" ).call( null, npc.getName(), newSentry, 0, "group", "target", sections[1]  ); 
                }
                else if ( sections[0].equals( "EVENT" ) ) {
                    CommandHandler.getCommand( "event" ).call( null, npc.getName(), newSentry, 0, "event", "add", sections[1] );
                }                   
                else if ( sections[0].equals( "FACTION" ) ) {
                    CommandHandler.getCommand( "faction" ).call( null, npc.getName(), newSentry, 0, "faction", "target", sections[1] );
                }
                else if ( sections[0].equals( "FACTIONENEMIES" ) ) {
                    CommandHandler.getCommand( "faction" ).call( null, npc.getName(), newSentry, 0, "faction", "join", sections[1] );
                }
                else if ( sections[0].equals( "TOWN" ) ) {
                    CommandHandler.getCommand( "towny" ).call( null, npc.getName(), newSentry, 0, "towny", "target", sections[1] );
                }
                else if ( sections[0].equals( "WARTEAM" ) ) {
                    CommandHandler.getCommand( "war" ).call( null, npc.getName(), newSentry, 0, "war", "target", sections[1] );             
                }
                else if ( sections[0].equals( "TEAM" ) ) {
                    CommandHandler.getCommand( "scoreboard" ).call( null, npc.getName(), newSentry, 0, "scoreboard", "target", sections[1] );
                }
                else if ( sections[0].equals( "CLAN" ) ) {
                    CommandHandler.getCommand( "clan" ).call( null, npc.getName(), newSentry, 0, "clan", "target", sections[1] );
                }
                else Sentries.logger.info( "[Sentries] NPC:" + npc.getName() + ". Target could not be imported:- " + t );
            }
        }        
        // import ignores
        for ( String t : sentry.ignoreTargets ) {
            if ( t.contains( "ENTITY:ALL" ) )
                newSentry.ignores.add( new AllEntitiesTarget() );
            else if ( t.contains( "ENTITY:MONSTER" ) )
                newSentry.ignores.add( new AllMonstersTarget() );
            else if ( t.contains( "ENTITY:PLAYER" ) )
                newSentry.ignores.add( new AllPlayersTarget() );
            else if ( t.contains( "ENTITY:NPC" ) )
                newSentry.ignores.add( new AllNPCsTarget() );
            else if ( t.contains( "ENTITY:OWNER" ) )
                newSentry.ignores.add( new OwnerTarget( npc.getTrait( Owner.class ).getOwnerId() ) );
            else {
                String[] sections = Util.colon.split( t );
                if ( sections.length != 2 ) continue;

                sections[0] = sections[0].trim();
                sections[1] = sections[1].trim();
                
                if ( sections[0].equals( "NPC" ) ) {
                    for ( NPC each : CitizensAPI.getNPCRegistry() ) {
                        if ( each.getName() == sections[1] )
                            newSentry.ignores.add( new NamedNPCTarget( each.getUniqueId() ) );
                    }
                }
                
                else if ( sections[0].equals( "PLAYER" ) ) {
                    UUID uuid = Arrays.stream( Bukkit.getOfflinePlayers() )
                                      .parallel()
                                      .filter( p -> p.getName().equalsIgnoreCase( sections[1] ) )
                                      .map( p -> p.getUniqueId() )
                                      .findAny().get();
                    if ( uuid != null )
                        newSentry.ignores.add( new NamedPlayerTarget( uuid ) );
                }
                
                else if ( sections[0].equals( "ENTITY" ) ) {
                    EntityType target = EntityType.valueOf( sections[1].toUpperCase() );
                    if ( target != null ) {
                        newSentry.ignores.add( new MobTypeTarget( target ) );
                    }
                }               
                else if ( sections[0].equals( "GROUP" ) ) {
                    CommandHandler.getCommand( "group" ).call( null, npc.getName(), newSentry, 0, "group", "ignore", sections[1] ); 
                }
                else if ( sections[0].equals( "FACTION" ) ) {
                    CommandHandler.getCommand( "faction" ).call( null, npc.getName(), newSentry, 0, "faction", "ignore", sections[1] );
                }
                else if ( sections[0].equals( "TOWN" ) ) {
                    CommandHandler.getCommand( "towny" ).call( null, npc.getName(), newSentry, 0, "towny", "ignore", sections[1] );
                }
                else if ( sections[0].equals( "WARTEAM" ) ) {
                    CommandHandler.getCommand( "war" ).call( null, npc.getName(), newSentry, 0, "war", "ignore", sections[1] );
                }
                else if ( sections[0].equals( "TEAM" ) ) {
                    CommandHandler.getCommand( "scoreboard" ).call( null, npc.getName(), newSentry, 0, "scoreboard", "ignore", sections[1] );
                }
                else if ( sections[0].equals( "CLAN" ) ) {
                    CommandHandler.getCommand( "clan" ).call( null, npc.getName(), newSentry, 0, "clan", "ignore", sections[1] );
                }
                else Sentries.logger.info( "[Sentries] NPC:" + npc.getName() + ". Ignore could not be imported:- " + t );
            }
        }
        npc.removeTrait( net.aufdemrand.sentry.SentryTrait.class );
        
        return true;
    }
}
