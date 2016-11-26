package org.jabelpeeps.sentries.commands;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.targets.AllMobsTarget;
import org.jabelpeeps.sentries.targets.AllMonstersTarget;
import org.jabelpeeps.sentries.targets.AllNPCsTarget;
import org.jabelpeeps.sentries.targets.AllPlayersTarget;
import org.jabelpeeps.sentries.targets.MobTypeTarget;
import org.jabelpeeps.sentries.targets.NamedNPCTarget;
import org.jabelpeeps.sentries.targets.NamedPlayerTarget;
import org.jabelpeeps.sentries.targets.OwnerTarget;
import org.jabelpeeps.sentries.targets.TargetType;
import org.jabelpeeps.sentries.targets.TraitTypeTarget;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Owner;


public class IgnoreCommand implements SentriesComplexCommand {

    String ignoreCommandHelp;

    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= nextArg + 1 ) {
            sender.sendMessage( getLongHelp() );
            return;
        } 
        
        String subCommand = args[nextArg + 1].toLowerCase();
        
        if ( S.LIST.equals( subCommand ) ) {
            StringJoiner joiner = new StringJoiner( ", " );           
            inst.ignores.forEach( t -> joiner.add( t.getPrettyString() ) );
            
            Util.sendMessage( sender, Col.GREEN, npcName, "'s Ignores: ", joiner.toString() );
            return;
        }

        if ( S.CLEARALL.equals( subCommand ) ) {
            inst.ignores.removeIf( i -> i instanceof TargetType.Internal );
            Util.sendMessage( sender, Col.GREEN, npcName, ": ALL Ignores cleared" );
            return;
        }

        if ( (S.ADD + S.REMOVE).contains( subCommand ) ) {

            // TODO add more user feedback for success or failure conditions
            
            if ( args.length <= nextArg + 2 ) {
                Util.sendMessage( sender, S.ERROR, "Missing arguments!", Col.RESET, " try '/sentry help ignore'" );
                return;
            }
            TargetType target = null;
            String[] targetArgs = Util.colon.split( args[nextArg + 2] );           
            String firstSubArg = targetArgs[0].toLowerCase();
            
            if ( targetArgs.length == 1 && firstSubArg.equals( "owner" ) ) {
                target = new OwnerTarget( inst.getNPC().getTrait( Owner.class ).getOwnerId() );
            }
            else if ( targetArgs.length > 1 ) {
                String secondSubArg = targetArgs[1].toLowerCase();
                
                if ( firstSubArg.equals( "all" ) ) {
                    if ( secondSubArg.equals( "monsters" ) ) 
                        target = new AllMonstersTarget();
                    else if ( secondSubArg.equals( "mobs" ) ) 
                        target = new AllMobsTarget();
                    else if ( secondSubArg.equals( "npcs" ) ) 
                        target = new AllNPCsTarget();
                    else if ( secondSubArg.equals( "players" ) ) 
                        target = new AllPlayersTarget();
                }
                
                else if ( firstSubArg.equals( "mobtype" ) ) {
                    EntityType type = EntityType.valueOf( secondSubArg.toUpperCase() );
                    if ( type != null && Sentries.mobs.contains( type ) )
                        target = new MobTypeTarget( type );
                }

                else if ( firstSubArg.equals( "trait" ) ) {
                    Class<? extends Trait> clazz = CitizensAPI.getTraitFactory().getTraitClass( secondSubArg );
                    if ( clazz != null ) 
                        target = new TraitTypeTarget( secondSubArg, clazz );
                }
                
                else if ( targetArgs.length > 2 && firstSubArg.equals( "named" ) ) {
                    if ( secondSubArg.equals( "player" ) ) { 
                        try {
                        target = new NamedPlayerTarget( 
                                Arrays.stream( Bukkit.getOfflinePlayers() )
                                      .filter( p -> p.getName().equalsIgnoreCase( targetArgs[2] ) )
                                      .findAny()
                                      .orElse( Bukkit.getOfflinePlayer( UUID.fromString( targetArgs[2] ) ) )
                                      .getUniqueId() );
                        } catch (IllegalArgumentException e) {
                            Util.sendMessage( sender, S.ERROR, "No player called:- ",targetArgs[2], " was found." );
                        }
                    }
                    else if ( secondSubArg.equals( "npc" ) ) {
    
                        for ( NPC npc : Sentries.registry ) {
                            if ( npc.getName().equalsIgnoreCase( targetArgs[2] ) ) {
                                target = new NamedNPCTarget( npc.getUniqueId() );
                                break;
                            }
                            else if ( npc.getUniqueId().toString().equals( targetArgs[2] ) ) {
                                target = new NamedNPCTarget( UUID.fromString( targetArgs[2] ) );
                                break;
                            }
                        }
                    }
                }
            }
            
            if ( target == null )
                Util.sendMessage( sender, "The intended target was not recognised" );
            else if ( S.ADD.equals( subCommand ) && inst.ignores.add( target ) )
                Util.sendMessage( sender, "Ignore Added" );
            else if ( S.REMOVE.equals( subCommand ) && inst.ignores.remove( target ) )
                Util.sendMessage( sender, "Ignore Removed" );  
        }
    }

    @Override
    public String getShortHelp() { return "set targets to ignore"; }

    @Override
    public String getLongHelp() {

        if ( ignoreCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ) .add( "" );

            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry ", S.IGNORE, " <add|remove|list|clearall> <TargetType>", 
                                                Col.RESET, " to add a target for a sentry to ignore."  ) );
            joiner.add( String.join( "", Col.BOLD, "Ignores override targets and/or events (if both apply to an entity).", Col.RESET ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.LIST, Col.RESET, " to display current list of ignores" ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.CLEARALL, Col.RESET, " to clear the ALL the current ignores" ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.ADD, Col.RESET, " to add ", Col.GOLD, "<TargetType> ", Col.RESET, "as an ignore" ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.REMOVE, Col.RESET, " to remove ", Col.GOLD, "<TargetType> ", Col.RESET, "as an ignore" ) );
            joiner.add( String.join( "", Col.BOLD, Col.GOLD, "<TargetType> ", Col.RESET, S.HELP_ADD_REMOVE_TYPES ) );
            joiner.add( String.join( "", Col.GOLD, "  Owner ", Col.RESET, "to ignore the owner of the sentry") );
            joiner.add( String.join( "", Col.GOLD, "  All:Players ", Col.RESET, "to ignore all (human) Players.") );
            joiner.add( String.join( "", Col.GOLD, "  All:Monsters ", Col.RESET, "to ignore all hostile mobs.") );
            joiner.add( String.join( "", Col.GOLD, "  All:Mobs ", Col.RESET, "to ignore all mobs (passive and hostile)") );
            joiner.add( String.join( "", Col.GOLD, "  Mobtype:<Type> ", Col.RESET, "to ignore mobs of <Type>.") );
            joiner.add( String.join( "", Col.GOLD, "  All:NPCs ", Col.RESET, "to ignore all Citizens NPC's.") );
            joiner.add( String.join( "", Col.GOLD, "  Trait:<TraitName> ", Col.RESET, "to ignore NPC's with the named Trait" ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, "/sentry help ", S.LIST_MOBS, Col.RESET, " to list valid mob type names." ) );
//            joiner.add( String.join( "", Col.GOLD, "", Col.RESET, "") );
            joiner.add( Util.getAdditionalTargets() );

            ignoreCommandHelp = joiner.toString();
        }
        return ignoreCommandHelp;
    }
    @Override
    public String getPerm() { return S.PERM_IGNORE; }
}
