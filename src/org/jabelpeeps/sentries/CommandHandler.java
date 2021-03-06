package org.jabelpeeps.sentries;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.commands.ArmourCommand;
import org.jabelpeeps.sentries.commands.AttackRateCommand;
import org.jabelpeeps.sentries.commands.CriticalsCommand;
import org.jabelpeeps.sentries.commands.DebugInfoCommand;
import org.jabelpeeps.sentries.commands.DropsCommand;
import org.jabelpeeps.sentries.commands.EquipCommand;
import org.jabelpeeps.sentries.commands.EventCommand;
import org.jabelpeeps.sentries.commands.FollowDistanceCommand;
import org.jabelpeeps.sentries.commands.GreetingCommand;
import org.jabelpeeps.sentries.commands.GuardCommand;
import org.jabelpeeps.sentries.commands.HealRateCommand;
import org.jabelpeeps.sentries.commands.HealthCommand;
import org.jabelpeeps.sentries.commands.IgnoreCommand;
import org.jabelpeeps.sentries.commands.InfoCommand;
import org.jabelpeeps.sentries.commands.InvincibleCommand;
import org.jabelpeeps.sentries.commands.KillsDropCommand;
import org.jabelpeeps.sentries.commands.ListAllCommand;
import org.jabelpeeps.sentries.commands.MobsAttackCommand;
import org.jabelpeeps.sentries.commands.MountCommand;
import org.jabelpeeps.sentries.commands.NightVisionCommand;
import org.jabelpeeps.sentries.commands.RangeCommand;
import org.jabelpeeps.sentries.commands.RespawnDelayCommand;
import org.jabelpeeps.sentries.commands.RetaliateCommand;
import org.jabelpeeps.sentries.commands.SentriesCommand;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.commands.SentriesNumberCommand;
import org.jabelpeeps.sentries.commands.SentriesSimpleCommand;
import org.jabelpeeps.sentries.commands.SentriesToggleCommand;
import org.jabelpeeps.sentries.commands.SetSpawnCommand;
import org.jabelpeeps.sentries.commands.SpeedCommand;
import org.jabelpeeps.sentries.commands.StrengthCommand;
import org.jabelpeeps.sentries.commands.TargetComand;
import org.jabelpeeps.sentries.commands.VoiceRangeCommand;
import org.jabelpeeps.sentries.commands.WarningCommand;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private static Map<String, SentriesCommand> commandMap = new TreeMap<>();
    private static String mainHelpIntro, mainHelpOutro, shortEquipList, mobsList;

    static {
        commandMap.put( S.TARGET,       new TargetComand() );
        commandMap.put( S.IGNORE,       new IgnoreCommand() );
        commandMap.put( S.EVENT,        new EventCommand() );
        commandMap.put( S.EQUIP,        new EquipCommand() );
        commandMap.put( S.GUARD,        new GuardCommand() );
        commandMap.put( S.SET_SPAWN,    new SetSpawnCommand() );
        commandMap.put( S.INVINCIBLE,   new InvincibleCommand() );
        commandMap.put( S.RETALIATE,    new RetaliateCommand() );
        commandMap.put( S.CRITICALS,    new CriticalsCommand() );
        commandMap.put( S.DROPS,        new DropsCommand() );
        commandMap.put( S.KILLS_DROP,   new KillsDropCommand() ); 
        commandMap.put( S.MOBS_ATTACK,  new MobsAttackCommand() );
        commandMap.put( S.MOUNT,        new MountCommand() );
        commandMap.put( S.INFO,         new InfoCommand() );
        commandMap.put( S.GREETING,     new GreetingCommand() );
        commandMap.put( S.WARNING,      new WarningCommand() );
        commandMap.put( S.FOLLOW,       new FollowDistanceCommand() );
        commandMap.put( S.HEALTH,       new HealthCommand() );
        commandMap.put( S.STRENGTH,     new StrengthCommand() );
        commandMap.put( S.NIGHT_VISION, new NightVisionCommand() );
        commandMap.put( S.RESPAWN,      new RespawnDelayCommand() );
        commandMap.put( S.SPEED,        new SpeedCommand() );
        commandMap.put( S.ATTACK_RATE,  new AttackRateCommand() );
        commandMap.put( S.HEALRATE,     new HealRateCommand() );
        commandMap.put( S.RANGE,        new RangeCommand() );
        commandMap.put( S.VOICE_RANGE,  new VoiceRangeCommand() );
        commandMap.put( "listall",      new ListAllCommand() );
        commandMap.put( "debuginfo",    new DebugInfoCommand() );
        
        SentriesNumberCommand command = new ArmourCommand();
        commandMap.put( S.ARMOUR,       command ); 
        commandMap.put( S.ARMOR,        command );  // for people who can't spell =)
    }
    
    /**
     * Adds a command option to the Sentries command structure as a second 
     * level sub-command (i.e. an option immediately following '/sentry')
     * 
     * @param name - the name of the sub-command that will need to be used by users to access it.
     * @param command - either a {@link#SentriesComplexCommand}, a {@link#SentriesToggleCommand} 
     * or a {@link#SentriesNumberCommand} object.
     */
    public static void addCommand( String name, SentriesCommand command ) {
        commandMap.put( name, command );
    }
    
    /** Static method to call a registered Sentries sub-command with arguments, only works for 
     *  'complex' commands - ones that have multiple possible arguments.
     * @param inst - the instance of SentryTrait on which to call the command 
     * @param args - all the args that would normally be used following the '/sentry' 
     *              (i.e. the sub-command & the sub-command's args ) 
     * @return true if a sub-command with the name give as the first member of args was found */
    static boolean callCommand( SentryTrait inst, String ... args ) {
        SentriesComplexCommand command = (SentriesComplexCommand) commandMap.get( args[0].toLowerCase() );
        if ( command == null ) return false;
        
        command.call( null, null, inst, 0, args );
        return true;
    }

    /**
     * Convenience method to check perms on Command usage. The method includes
     * informing the player if they lack the required perms.
     * 
     * @param command
     *            - The perm node to be checked.
     * @param player
     *            - The sender of the command.
     * @return true - if the player has the required permission.
     */
    static boolean checkCommandPerm( String command, CommandSender player ) {

        if ( player.hasPermission( command ) ) return true;

        player.sendMessage( Col.RED.concat( S.ERROR_NO_COMMAND_PERM ) );
        return false;
    }

    /**
     * Check that the String[] args contains enough arguments, directing the
     * player on how to get help if false.
     * 
     * @param number
     *            - the number of required args
     * @param args
     *            - the argument array
     * @param player
     *            - the player who entered the command.
     * @return true - args.length >= number
     */
    private static boolean enoughArgs( int number, String[] args, CommandSender player ) {

        if ( args.length >= number ) return true;

        player.sendMessage( Col.RED.concat( S.GET_COMMAND_HELP ) );
        return false;
    }
    
    public static String getAdditionalTargets( CommandSender sender ) {
        
        StringJoiner joiner = new StringJoiner( System.lineSeparator() );
        joiner.add( "You can also use:- " ); 
        
        commandMap.entrySet().stream()
                  .filter( e -> e.getValue() instanceof SentriesCommand.Targetting 
                                && sender.hasPermission( e.getValue().getPerm() ) )
                  .forEach( e -> joiner.add( 
                          String.join( " ", Col.GOLD, " /sentry", e.getKey(), "...", Col.RESET, e.getValue().getShortHelp() ) ) );
        
        return joiner.toString();
    }
    
    @Override
    public List<String> onTabComplete( CommandSender sender, Command command, String alias, String[] args ) {       
        
        int nextArg = ( Utils.string2Int( args[0] ) > 0 ) ? 1 : 0;
        
        if ( args.length == 1 + nextArg ) {
            return getCommandList( sender, args[nextArg] );
        }  
        else if (   args.length == 2 
                    && S.HELP.equalsIgnoreCase( args[0] ) ) {
            return getCommandList( sender, args[1] );
        }
        else if (   args.length >= 2 + nextArg
                    && commandMap.containsKey( args[nextArg] ) ) {
            SentriesCommand subcommand = commandMap.get( args[nextArg] );
            
            if (    subcommand instanceof SentriesCommand.Tabable
                    && sender.hasPermission( subcommand.getPerm() ) )
                return ((SentriesCommand.Tabable) subcommand).onTab( nextArg, args );
        }
        return null;       
    }
    
    private List<String> getCommandList( CommandSender sender, String arg ) {
        List<String> tabs = new ArrayList<>( commandMap.keySet() );
        tabs.removeIf( t -> !t.startsWith( arg ) 
                            || !sender.hasPermission( commandMap.get( t ).getPerm() ) );
        return tabs;   
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command com, String label, String[] inargs) {
        
        if ( !enoughArgs( 1, inargs, sender ) ) return true;

        // ----------------------------------- help command -----------------
        if ( S.HELP.equalsIgnoreCase( inargs[0] ) ) {

            if ( inargs.length > 1 ) {
                String subcommand = inargs[1].toLowerCase();
                
                SentriesCommand command = commandMap.get( subcommand );
                
                if ( command != null && sender.hasPermission( command.getPerm() ) ) {
                    sender.sendMessage( command.getLongHelp() );
                    
                    if ( (S.TARGET + S.IGNORE).contains( subcommand ) )
                        sender.sendMessage( getAdditionalTargets( sender ) );
                }
                else if ( S.LIST_MOBS.equals( subcommand ) ) {
                    if ( mobsList == null ) {
                        mobsList = String.join( ", ", Sentries.mobs.stream()
                                                                   .map( m -> m.toString() )
                                                                   .toArray( String[]::new ) );
                    }
                    sender.sendMessage( mobsList ); 
                }
                else if ( "listequips".equalsIgnoreCase( subcommand ) ) {
                    if ( shortEquipList == null ) {
                        Set<Material> allowed = EnumSet.copyOf( Sentries.helmets );
                        allowed.addAll( Sentries.chestplates );
                        allowed.addAll( Sentries.leggings );
                        allowed.addAll( Sentries.boots );
                        
                        for ( AttackType each : AttackType.values() ) {
                            if ( each == AttackType.BRAWLER ) continue;
                            allowed.add( each.getWeapon() );
                        }

                        StringJoiner joiner = new StringJoiner( ", " );
                        for ( Material each : allowed ) {
                            joiner.add( each.toString() );
                        }
                        shortEquipList = "Allowed equipment includes:- " + joiner.toString();
                    }
                    sender.sendMessage( shortEquipList );
                }                
                else sender.sendMessage( S.ERROR_UNKNOWN_COMMAND );
                
                return true;
            }
            // if no arguments are added after '/sentry help'
            if ( mainHelpIntro == null ) {
                StringJoiner joiner = new StringJoiner( System.lineSeparator() );
                
                joiner.add( Utils.join( Col.GOLD, "---------- Sentries Commands ----------", Col.RESET ) );
                joiner.add( Utils.join( "Select NPC's with ", Col.GOLD, "'/npc sel'", Col.RESET, " before running commands, or" ) );
                joiner.add( Utils.join( " use ", Col.GOLD, "/sentry #npcid <command> ", Col.RESET, "to run a command on the sentry with the given npcid." ) );
                joiner.add( Utils.join( Col.GOLD, "-------------------------------", Col.RESET ) );
                
                mainHelpIntro = joiner.toString();
            }
            sender.sendMessage( mainHelpIntro );

            for ( Entry<String, SentriesCommand> each : commandMap.entrySet() ) {                
                if ( S.ARMOR.equals( each.getKey() ) ) continue;
                
                SentriesCommand command = each.getValue();
                
                if ( sender.hasPermission( command.getPerm() ) ) {
                    Utils.sendMessage( sender, Col.GOLD, "/sentry ", each.getKey(), " ... ", Col.RESET, command.getShortHelp() );
                }
            }
            // two special commands to add to the list...
            if ( checkCommandPerm( S.PERM_RELOAD, sender ) )
                Utils.sendMessage( sender, Col.GOLD, "/sentry reload", Col.RESET, " - reloads the config file" );
            
            if ( sender instanceof ConsoleCommandSender )
                Utils.sendMessage( sender, Col.GOLD, "/sentry debug", Col.RESET, 
                        " - toggles display of debug info on the console  ", Col.RED, "NOTE: Debug Reduces Performance!" );

            if ( mainHelpOutro == null ) {
                StringJoiner joiner = new StringJoiner( System.lineSeparator() );
        
                joiner.add( Utils.join( Col.GOLD, "-------------------------------", Col.RESET ) );
                joiner.add( Utils.join( "do ", Col.GOLD, "/sentry help <command>", Col.RESET, " for further help on each command" ) );
                joiner.add( Utils.join( Col.GOLD, "-------------------------------", Col.RESET ) );
        
                mainHelpOutro = joiner.toString();
            }           
            sender.sendMessage( mainHelpOutro );
            return true;
        }
        // ------------------------------------- Debug Command --------------
        if (    sender instanceof ConsoleCommandSender 
                && "debug".equalsIgnoreCase( inargs[0] ) ) {

            Sentries.debug = !Sentries.debug;
            sender.sendMessage( Col.GREEN + "Debug is now: " + (Sentries.debug ? S.ON : S.OFF) );
            return true;
        }
        // -------------------------------------- Reload Command ------------
        if ( S.RELOAD.equalsIgnoreCase( inargs[0] ) ) {

            if ( checkCommandPerm( S.PERM_RELOAD, sender ) ) {

                Sentries.plugin.reloadMyConfig();
                sender.sendMessage( Col.GREEN + "reloaded Sentries's config.yml file" );
            }
            return true;
        }
        // -------------------------------------------------------------------------------------
        
        // did player specify an integer as the first argument?
        int npcid = Utils.string2Int( inargs[0] );

        // if a positive number was found, the next argument to parse will be at position 1, otherwise 0.
        int nextArg = (npcid > 0) ? 1 : 0;

        if ( !enoughArgs( 1 + nextArg, inargs, sender ) ) return true;
        
        if  (   inargs[nextArg].toLowerCase().equals( "import" )
                && inargs.length > nextArg + 1
                && commandMap.containsKey( "import" )
                && inargs[nextArg + 1].toLowerCase().equals( "all" ) 
                && checkCommandPerm( commandMap.get( "import" ).getPerm(), sender ) ) {
            ((SentriesComplexCommand) commandMap.get( "import" )).call( sender, null, null, nextArg, inargs );
            return true;
        }
        NPC thisNPC;

        if ( npcid == Integer.MIN_VALUE ) {
            thisNPC = CitizensAPI.getDefaultNPCSelector().getSelected( sender );
            if ( thisNPC == null )
                Utils.sendMessage( sender, S.ERROR, S.ERROR_NO_NPC );
        }
        else {
            thisNPC = Sentries.registry.getById( npcid );
            if ( thisNPC == null )
                Utils.sendMessage( sender, S.ERROR, S.ERROR_ID_INVALID, String.valueOf( npcid ) );
        }       
        if ( thisNPC == null ) return true;
            
        // We are now sure that thisNPC is valid.
        if  (   !inargs[nextArg].toLowerCase().equals( "import" ) 
                && !thisNPC.hasTrait( SentryTrait.class ) ) {
            sender.sendMessage( S.ERROR.concat( S.ERROR_NOT_SENTRY ) );
            return true;
        }
        
        if  (   sender instanceof Player
                && !sender.isOp()
                && !((Entity) sender).hasMetadata( "NPC" ) ) {
            // TODO consider changing this section to allow admins to modify other players' npcs.

            if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( sender.getName() ) ) {
                if ( !((Player) sender).hasPermission( S.PERM_CITS_ADMIN ) ) {
                    sender.sendMessage( Col.RED.concat( "You must be the owner of the sentry to use commands on it." ) );
                    return true;
                }
                if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( "server" ) ) {
                    sender.sendMessage( Col.RED.concat( "You, or the server, must be the owner of the sentry to use commands on it." ) );
                    return true;
                }
            }
        }
        // We now know that player is either the owner, op'ed, or an admin with a server-owned npc. 
        SentryTrait inst = thisNPC.getTrait( SentryTrait.class );
        String npcName = thisNPC.getName();
     
        // ------------------------------ hand off to the separate command classes -----    
        SentriesCommand command = commandMap.get( inargs[nextArg].toLowerCase() );

        if ( command != null ) {            
            if ( checkCommandPerm( command.getPerm(), sender ) ) {
                
                if ( command instanceof SentriesSimpleCommand )
                    ((SentriesSimpleCommand) command).call( sender, npcName, inst );
                
                else if ( command instanceof SentriesComplexCommand )
                    ((SentriesComplexCommand) command).call( sender, npcName, inst, nextArg, inargs ); 
                
                else if ( command instanceof SentriesToggleCommand ) {
                    
                    // This is held as an object not a primitive to allow for a third state - 'null'.
                    Boolean set = null;
                    
                    if ( inargs.length > nextArg + 2 ) {
                        Utils.sendMessage( sender, S.ERROR, "Too many arguments given.", Col.RESET, " This command accepts 1 argument (at most)." );
                        return true;
                    }
                    else if ( inargs.length == 2 + nextArg ) {
                        String arg = inargs[1 + nextArg];
                        if (    S.TRUE.equalsIgnoreCase( arg )
                                || S.ON.equalsIgnoreCase( arg ) )
                            set = true;
                        if (    S.FALSE.equalsIgnoreCase( arg )
                                || S.OFF.equalsIgnoreCase( arg ) )
                            set = false;
                    }                   
                    ((SentriesToggleCommand) command).call( sender, npcName, inst, set );
                }
                
                else if ( command instanceof SentriesNumberCommand ) {
                    
                    if ( inargs.length > nextArg + 2 )
                        Utils.sendMessage( sender, S.ERROR, "Too many arguments given.", Col.RESET, " Only one number value can be processesd." );
                    else {
                        String number = null;
                        if ( inargs.length == nextArg + 2 ) number = inargs[nextArg + 1];
                        
                        ((SentriesNumberCommand) command).call( sender, npcName, inst, number );                    
                    }
                }
            }
            return true;
        }
        Utils.sendMessage( sender, S.ERROR, "Command not recognised, try '/sentry help'" );
        return true;
    }
}
