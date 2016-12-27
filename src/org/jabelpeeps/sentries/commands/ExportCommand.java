package org.jabelpeeps.sentries.commands;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentinelExporter;
import org.jabelpeeps.sentries.SentinelImporter;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import com.google.common.io.Files;


public class ExportCommand implements SentriesComplexCommand {
    
    private String helpText;

    @Override
    public String getShortHelp() { return "export NPC settings to Sentinel"; }
    @Override
    public String getPerm() { return "sentries.export"; }
    @Override
    public String getLongHelp() {

        if ( helpText == null ) {
            helpText = String.join( "",
                    "do  ", Col.GOLD, "/sentry export (all)", Col.RESET, 
                    " to export settings and targets for Combat NPC's to Sentinel",
                    " During the export, the original trait will be removed.", System.lineSeparator(),
                    "  use the optional argument ", Col.GOLD, "all ", Col.RESET, "to export all NPC's from Sentries.",
                    System.lineSeparator(), "  or export a single NPC, by having it selected, ",
                    " or putting its NPC id number as the first argument. ", System.lineSeparator(),
                    Col.RED, "IMPORTANT:  As exports are irreversible, an attempt will be made to backup the Citizens saves.yml "
                            + " before exporting, and the export will not proceed if the attempt fails." );
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
            else {
                
                if ( checkSentinel( sender ) ) {
                    imported = SentinelExporter.exportAll();
                }
                if ( imported == 0 )
                    Utils.sendMessage( sender, Col.YELLOW, "No NPC's were found to export." );
                else
                    Utils.sendMessage( sender, Col.GREEN, "Successfully exported ", String.valueOf( imported ), 
                                                " NPC's into Senntinel" );
            }
            return;
        }
        if ( checkSentinel( sender ) && SentinelImporter.importNPC( inst.getNPC() ) ) 
            Utils.sendMessage( sender, Col.GREEN, npcName, " has been succesfully exprted to Sentinel" );
        else {
            Utils.sendMessage( sender, Col.RED, "Exporting ", npcName, 
                    " has failed, please check the NPC's current traits by selecting them and then doing '/npc'" );
        }  
    }
   
    private boolean checkSentinel( CommandSender sender ) {
        if ( !Bukkit.getPluginManager().isPluginEnabled( "Sentinel" ) ) {
            Utils.sendMessage( sender, S.ERROR, "You need install Sentinel to export to Sentinel." );
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
            Utils.sendMessage( sender, S.ERROR, "Unable to backup Citizens saves.yml. Exmport aborted." );
            e.printStackTrace();
            return false;
        }
    }
}
