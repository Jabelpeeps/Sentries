package org.jabelpeeps.sentries.commands;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentinelImporter;
import org.jabelpeeps.sentries.SentryImporter;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import com.google.common.io.Files;


public class ImportCommand implements SentriesComplexCommand {
    
    private String helpText;

    @Override
    public String getShortHelp() { return "import NPC settings from Sentry & Sentinel"; }
    @Override
    public String getPerm() { return "sentries.import"; }

    @Override
    public String getLongHelp() {

        if ( helpText == null ) {
            helpText = String.join( "",
                    "do  ", Col.GOLD, "/sentry import (all) <sentry|sentinel>", Col.RESET, 
                    " to import settings and targets for Combat NPC's using the named traits into Sentries",
                    " During the import, the original trait will be removed.", System.lineSeparator(),
                    "  use the optional argument ", Col.GOLD, "all ", Col.RESET, "to import all NPC's with the named trait.",
                    System.lineSeparator(), "  or import a single NPC, by having it selected, ",
                    " or putting its NPC id number as the first argument. ", System.lineSeparator(),
                    Col.RED, "IMPORTANT:  As imports are irreversible, an attempt will be made to backup the Citizens saves.yml "
                            + " before importing, and the import will not proceed if the attempt fails." );
        }
        return helpText;
    }

    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= nextArg + 1 ) {
            sender.sendMessage( getLongHelp() );
            return;
        }
        String subCommand = args[nextArg + 1].toLowerCase();
        int imported = 0;
        
        if ( "all".equals( subCommand ) ) {
            if ( nextArg != 0 )
                Utils.sendMessage( sender, S.ERROR, "You have used an NPC id number, as well as the 'all' argument." );
            else if ( args.length <= 2 )
                Utils.sendMessage( sender, S.ERROR, "You need to name the plugin to import NPC's from." );
            else {
                String pluginName = args[2].toLowerCase();
                
                if ( "sentry".equals( pluginName ) && checkSentry( sender) )
                    imported = SentryImporter.importAll();
                else if ( "sentinel".equals( pluginName ) && checkSentinel( sender ) )
                    imported = SentinelImporter.importAll();
                else {
                    Utils.sendMessage( sender, S.ERROR, "plugin - ", pluginName, ", was not recognised." );
                    return;
                }
                if ( imported == 0 )
                    Utils.sendMessage( sender, Col.YELLOW, "No NPC's with trait ", pluginName, " were found to import." );
                else
                    Utils.sendMessage( sender, Col.GREEN, "Successfully imported ", String.valueOf( imported ), 
                                                " NPC's into Sentries from ", pluginName );
            }
            return;
        }
        if ( "sentry".equals( subCommand ) && checkSentry( sender ) && SentryImporter.importNPC( inst.getNPC() ) ) 
            Utils.sendMessage( sender, Col.GREEN, npcName, " has been successfully imported from Sentry(v1)" );
        else if ( "sentinel".equals( subCommand ) && checkSentinel( sender ) && SentinelImporter.importNPC( inst.getNPC() ) ) 
            Utils.sendMessage( sender, Col.GREEN, npcName, " has been succesfully imported from Sentinel" );
        else {
            Utils.sendMessage( sender, Col.RED, "Importing ", npcName, 
                    " has failed, please check the NPC's current traits by selecting them and then doing '/npc'" );
        }  
    }
    
    private boolean checkSentry( CommandSender sender ) {
        if ( !Bukkit.getPluginManager().isPluginEnabled( "Sentry" ) ) {
            Utils.sendMessage( sender, S.ERROR, "You need install Sentry(v1) to import from Sentry." );
            return false;
        }        
        return backupSavesYml( sender );
    }
    
    private boolean checkSentinel( CommandSender sender ) {
        if ( !Bukkit.getPluginManager().isPluginEnabled( "Sentinel" ) ) {
            Utils.sendMessage( sender, S.ERROR, "You need install Sentinel to import from Sentinel." );
            return false;
        }
        return backupSavesYml( sender );
    }
    
    private boolean backupSavesYml( CommandSender sender ) {
        File folder = Bukkit.getPluginManager().getPlugin( "Citizens" ).getDataFolder();
        File saves = new File( folder, "saves.yml" );
        File backup = new File( folder, "backup_of_saves.yml" );
        int i = 1;
        while ( backup.exists() ) {
            backup = new File( folder, "backup_of_saves(" + (++i) + ").yml" );
        }
        try {
            Files.copy( saves, backup );
            return true;
        } 
        catch ( IOException e ) {
            Utils.sendMessage( sender, S.ERROR, "Unable to backup Citizens saves.yml. Import aborted." );
            e.printStackTrace();
            return false;
        }
    }
}
