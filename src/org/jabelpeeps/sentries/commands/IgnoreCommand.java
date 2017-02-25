package org.jabelpeeps.sentries.commands;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;
import org.jabelpeeps.sentries.targets.AllMobsTarget;
import org.jabelpeeps.sentries.targets.AllMonstersTarget;
import org.jabelpeeps.sentries.targets.AllNPCsTarget;
import org.jabelpeeps.sentries.targets.AllPlayersTarget;
import org.jabelpeeps.sentries.targets.HoldingTarget;
import org.jabelpeeps.sentries.targets.MobTypeTarget;
import org.jabelpeeps.sentries.targets.NamedNPCTarget;
import org.jabelpeeps.sentries.targets.NamedPlayerTarget;
import org.jabelpeeps.sentries.targets.OwnerTarget;
import org.jabelpeeps.sentries.targets.TargetType;
import org.jabelpeeps.sentries.targets.TraitTypeTarget;

import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Owner;


public class IgnoreCommand implements SentriesComplexCommand, SentriesCommand.Tabable {

    private String ignoreCommandHelp;
    @Getter private String shortHelp = "set targets to ignore";
    @Getter private String perm = S.PERM_IGNORE;

    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= nextArg + 1 ) {
            sender.sendMessage( getLongHelp() );
            sender.sendMessage( CommandHandler.getAdditionalTargets( sender ) );
            return;
        } 
        
        String subCommand = args[nextArg + 1].toLowerCase();
        
        if ( S.LIST.equals( subCommand ) ) {
            StringJoiner joiner = new StringJoiner( ", " );           
            inst.ignores.forEach( t -> joiner.add( t.getPrettyString() ) );
            
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s Ignores: ", joiner.toString() );
            return;
        }

        if ( S.CLEARALL.equals( subCommand ) ) {
            inst.ignores.removeIf( i -> i instanceof TargetType.Internal );
            Utils.sendMessage( sender, Col.GREEN, npcName, ": ALL Ignores cleared" );
            return;
        }

        if ( (S.ADD + S.REMOVE).contains( subCommand ) ) {

            if ( args.length <= nextArg + 2 ) {
                Utils.sendMessage( sender, S.ERROR, "Missing arguments!", Col.RESET, " try '/sentry help ignore'" );
                return;
            }
            TargetType target = null;
            String[] targetArgs = Utils.colon.split( args[nextArg + 2] );           
            String firstSubArg = targetArgs[0].toLowerCase();
            
            if ( targetArgs.length == 1 && firstSubArg.equals( "owner" ) ) {
                Owner ownerTrait = inst.getNPC().getTrait( Owner.class );
                if ( !ownerTrait.isOwnedBy( Owner.SERVER ) )
                    target = new OwnerTarget( ownerTrait );
                else
                    Utils.sendMessage( sender, S.ERROR, "You cannot add an owner ignore for a server owned sentry" );
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
                
                else if ( firstSubArg.equals( "holding" ) ) {
                    Material type = Material.valueOf( secondSubArg.toUpperCase() );
                    if ( type != null )
                        target = new HoldingTarget( type );
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
                                Arrays.stream( Bukkit.getOfflinePlayers() ).parallel()
                                      .filter( p -> p.getName().equalsIgnoreCase( targetArgs[2] ) )
                                      .map( p -> p.getUniqueId() )
                                      .findAny()
                                      .orElse( UUID.fromString( targetArgs[2] ) ) );
                        } catch (IllegalArgumentException e) {
                            Utils.sendMessage( sender, S.ERROR, "No player called:- ", targetArgs[2], " was found." );
                        }
                    }
                    else if ( secondSubArg.equals( "npc" ) ) {
    
                        for ( NPC npc : Sentries.registry ) {
                            if  (   npc.getName().equalsIgnoreCase( targetArgs[2] )
                                    || npc.getUniqueId().toString().equals( targetArgs[2] ) ) {
                                target = new NamedNPCTarget( npc.getUniqueId() );
                                break;
                            }
                        }
                    }
                }
            }
            if ( target == null )
                Utils.sendMessage( sender, "The intended target was not recognised" );
            else if ( S.ADD.equals( subCommand ) && inst.ignores.add( target ) )
                Utils.sendMessage( sender, "Ignore Added" );
            else if ( S.REMOVE.equals( subCommand ) && inst.ignores.remove( target ) )
                Utils.sendMessage( sender, "Ignore Removed" );  
        }
    }

    @Override
    public List<String> onTab( int nextArg, String[] args ) {
        if ( args.length == nextArg + 2 ) {
            List<String> tabs = Arrays.asList( S.ADD, S.REMOVE, S.LIST, S.CLEARALL );
            tabs.removeIf( t -> !t.startsWith( args[1 + nextArg].toLowerCase() ) );
            return tabs;
        }
        return null;
    }
    
    @Override
    public String getLongHelp() {
        if ( ignoreCommandHelp == null ) {
            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry ", S.IGNORE, " <add|remove|list|clearall> <TargetType>", 
                                                Col.RESET, " to add a target for a sentry to ignore."  ) );
            joiner.add( Utils.join( Col.BOLD, "Ignores override targets and/or events (if both apply to an entity).", Col.RESET ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.LIST, Col.RESET, " to display current list of ignores" ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.CLEARALL, Col.RESET, " to clear all ignores added via ", Col.GOLD, S.ADD, Col.RESET ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.ADD, Col.RESET, " to add ", Col.GOLD, "<TargetType> ", Col.RESET, "as an ignore" ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.REMOVE, Col.RESET, " to remove ", Col.GOLD, "<TargetType> ", Col.RESET, "as an ignore" ) );
            joiner.add( Utils.join( Col.BOLD, Col.GOLD, "<TargetType> ", Col.RESET, S.HELP_ADD_REMOVE_TYPES ) );
            joiner.add( Utils.join( Col.GOLD, "  Owner ", Col.RESET, "to ignore the owner of the sentry") );
            joiner.add( Utils.join( Col.GOLD, "  All:Players ", Col.RESET, "to ignore all (human) Players.") );
            joiner.add( Utils.join( Col.GOLD, "  All:NPCs ", Col.RESET, "to ignore all Citizens NPC's.") );
            joiner.add( Utils.join( Col.GOLD, "  Trait:<TraitName> ", Col.RESET, "to ignore NPC's with the named Trait" ) );
            joiner.add( Utils.join( Col.GOLD, "  All:Monsters ", Col.RESET, "to ignore all hostile mobs.") );
            joiner.add( Utils.join( Col.GOLD, "  All:Mobs ", Col.RESET, "to ignore all mobs (passive and hostile)") );
            joiner.add( Utils.join( Col.GOLD, "  Named:<player|npc>:<name> ", Col.RESET, "to ignore the named player or npc only.") );
            joiner.add( Utils.join( Col.GOLD, "  Mobtype:<Type> ", Col.RESET, "to ignore mobs of <Type>.") );
            joiner.add( Utils.join( "  use ", Col.GOLD, "/sentry help ", S.LIST_MOBS, Col.RESET, " to list valid mob types" ) );

            ignoreCommandHelp = joiner.toString();
        }
        return ignoreCommandHelp;
    }
}
