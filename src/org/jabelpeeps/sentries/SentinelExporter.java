package org.jabelpeeps.sentries;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jabelpeeps.sentries.targets.TargetType;
import org.mcmonkey.sentinel.SentinelTarget;
import org.mcmonkey.sentinel.SentinelTrait;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

public class SentinelExporter {
    
    public static int exportAll() {
        int convertedCount = 0;
        
        for ( NPC npc : CitizensAPI.getNPCRegistry() ) {
            if ( exportNPC( npc ) )
                convertedCount++;
        }
        return convertedCount;
    } 
    
    public static boolean exportNPC( NPC npc ) {
        
        if ( !npc.hasTrait( SentryTrait.class ) || npc.hasTrait( SentinelTrait.class )) return false;
        
        SentinelTrait sentinel = npc.getTrait( SentinelTrait.class );
        SentryTrait sentry = npc.getTrait( SentryTrait.class );
        
        sentinel.armor = Math.min( sentry.armour * 0.1, 1.0 );
        sentinel.attackRate = (int) ( sentry.arrowRate * 20 );
        
        if ( sentinel.attackRate > SentinelTrait.attackRateMax )
            sentinel.attackRate = SentinelTrait.attackRateMax;
        
        sentinel.chaseRange = sentry.range;
        sentinel.range = sentry.range;
        sentinel.closeChase = true;
        sentinel.rangedChase = false;
        sentinel.damage = sentry.strength;
        sentinel.enemyDrops = sentry.killsDrop;
        sentinel.fightback = sentry.iRetaliate;
        
        double hpHealedPerPeriod = 1;
        if ( sentry.healRate < .5 )
            hpHealedPerPeriod = .5 / sentry.healRate;
        
        double secondsPerHpPoint = sentry.healRate / hpHealedPerPeriod;
        sentinel.healRate = (int) ( 20 * secondsPerHpPoint );
        
        if ( sentinel.healRate > SentinelTrait.healRateMax )
            sentinel.healRate = SentinelTrait.healRateMax;

        double health = sentry.getHealth();
        if ( health < SentinelTrait.healthMin )
            health = SentinelTrait.healthMin;
        else if ( health > SentinelTrait.healthMax )
            health = SentinelTrait.healthMax;
        
        sentinel.setHealth( sentry.getHealth() );
        sentinel.invincible = sentry.invincible;
        sentinel.needsAmmo = false;
        sentinel.respawnTime = sentry.respawnDelay * 20;
        sentinel.safeShot = false;
        sentinel.spawnPoint = sentry.spawnLocation;
        
        if ( sentry.guardeeName != null && sentry.guardeeName.length() > 0 ) {
            @SuppressWarnings( "deprecation" )
            OfflinePlayer op = Bukkit.getOfflinePlayer( sentry.guardeeName );
            if ( op != null ) {
                UUID playerId = op.getUniqueId();
                if ( playerId != null )
                    sentinel.setGuarding( playerId );
            }
        }                  
        for ( TargetType each : sentry.targets ) {
            String t = each.getTargetString();
            
            if ( t.contains( "All:Entities" ) ) {
                sentinel.targets.add( SentinelTarget.MOBS );
                sentinel.targets.add( SentinelTarget.PLAYERS );
                sentinel.targets.add( SentinelTarget.NPCS );
            }
            else if ( t.contains( "All:Monsters" ) )
                sentinel.targets.add( SentinelTarget.MONSTERS );
            else if ( t.contains( "All:Mobs" ) )
                sentinel.targets.add( SentinelTarget.MOBS );
            else if ( t.contains( "All:Players" ) )
                sentinel.targets.add( SentinelTarget.PLAYERS );
            else if ( t.contains( "All:NPCs" ) )
                sentinel.targets.add( SentinelTarget.NPCS );
            else {
                String[] sections = Util.colon.split( t );
                if ( sections.length >= 2 ) {
    
                    sections[0] = sections[0].trim();
                    sections[1] = sections[1].trim();
                    
                    if ( sections[0].equals( "MobType" ) ) {
                        SentinelTarget target = SentinelTarget.forName( sections[1] );
                        if ( target != null )
                            sentinel.targets.add( target );
                    }
                    else if ( sections.length == 3 ) {
                        sections[2] = sections[2].trim();
                    
                        if ( sections[0].equals( "Named" ) ) {
                            if ( sections[1].equals( "NPC" ) ) {
                                NPC npcTarget = CitizensAPI.getNPCRegistry().getByUniqueId( UUID.fromString( sections[2] ) );
                                if ( npcTarget != null )
                                    sentinel.npcNameTargets.add( npcTarget.getName() );
                            }
                            else if (sections[1].equals( "Player" ) ) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer( UUID.fromString( sections[2] ) );
                                if ( player != null )
                                    sentinel.playerNameTargets.add( player.getName() );
                            }
                        }
                        else if ( sections[0].equals( "GROUP" ) )
                            sentinel.groupTargets.add( sections[2] );
                        else if ( sections[0].equals( "FACTIONS" ) && !sections[1].equals( "join" ) )
                            sentinel.otherTargets.add( "factions" + sections[2] );
                        else if ( sections[0].equals( "TOWNY" ) && !sections[1].equals( "join" ) )
                            sentinel.otherTargets.add( "towny" + sections[2] );
                        else if ( sections[0].equals( "SCOREBOARD" ) )
                            sentinel.otherTargets.add( "sbteam" + sections[2] );
                        /*
                        These target specifiers are not implemented in Sentinel yet and so can't be exported
                        "WARTEAM", "CLAN"
                        */         
                    }
                }
            }
        }
        for ( TargetType each : sentry.events ) {
            String t = each.getTargetString();
            if ( t.contains( "PvP" ) ) sentinel.eventTargets.add( "pvp" );
            else if ( t.contains( "PvE" ) ) sentinel.eventTargets.add( "pve" );
            else if ( t.contains( "PvNPC" ) ) sentinel.eventTargets.add( "pvnpc" );
            else if ( t.contains( "PvSentry" ) ) sentinel.eventTargets.add( "pvnsentinel" );
        }
        
        for ( TargetType each : sentry.ignores ) {
            String t = each.getTargetString();
            if ( t.contains( "All:Monsters" ) )
                sentinel.ignores.add( SentinelTarget.MONSTERS );
            else if ( t.contains( "All:Mobs" ) )
                sentinel.ignores.add( SentinelTarget.MOBS );
            else if ( t.contains( "All:Players" ) )
                sentinel.ignores.add( SentinelTarget.PLAYERS );
            else if ( t.contains( "All:NPCs" ) )
                sentinel.ignores.add( SentinelTarget.NPCS );
            else if ( t.contains( "Owner" ) )
                sentinel.ignores.add( SentinelTarget.OWNER );
            else {
                String[] sections = Util.colon.split( t );
                if ( sections.length >= 2 ) {
                    sections[0] = sections[0].trim();
                    sections[1] = sections[1].trim();
                    
                    if ( sections[0].equals( "MobType" ) ) {
                        SentinelTarget mob = SentinelTarget.forName( sections[1] );
                        if ( mob != null )
                            sentinel.ignores.add( mob );
                    }
                    else if ( sections.length == 3 ) {
                        sections[2] = sections[2].trim();
                        
                        if ( sections[0].equals( "Named" ) ) {
                            if ( sections[1].equals( "NPC" ) ) {
                                NPC npcTarget = CitizensAPI.getNPCRegistry().getByUniqueId( UUID.fromString( sections[2] ) );
                                if ( npcTarget != null )
                                    sentinel.npcNameIgnores.add( npcTarget.getName() );
                            }
                            else if ( sections[1].equals( "PLAYER" ) ) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer( UUID.fromString( sections[2] ) );
                                if ( player != null )
                                    sentinel.playerNameIgnores.add( player.getName() );
                            }
                        }
                        else if ( sections[0].equals( "GROUP" ) )
                            sentinel.groupIgnores.add( sections[2] );
                        else if ( sections[0].equals( "FACTIONS" ) && !sections[1].equals( "join" ) )
                            sentinel.otherIgnores.add( "factions" + sections[2] );
                        else if ( sections[0].equals( "TOWNY" ) && !sections[1].equals( "join" ) )
                            sentinel.otherIgnores.add( "towny" + sections[2] );
                        else if ( sections[0].equals( "SCOREBOARD" ) )
                            sentinel.otherIgnores.add( "sbteam" + sections[2] );      
                        /*
                        These ignore specifiers are not implemented in Sentinel yet and so can't be exported
                        "WARTEAM", "CLAN"
                        */ 
                    }
                }
            }
        }
        npc.removeTrait( SentryTrait.class );
       
        return true;
    }
}
