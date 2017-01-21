package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class SetSpawnCommand implements SentriesSimpleCommand {

    private String helpTxt;
    @Getter private String shortHelp = "set the sentry's spawn point";
    @Getter private String perm = S.PERM_SET_SPAWN;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst ) {
        
        Entity entity = inst.getNPC().getEntity();
        
        if ( entity == null ) 
            sender.sendMessage( Col.RED.concat( "You cannot set a spawn point while a sentry is not spawned" ) );
        else {
            inst.spawnLocation = entity.getLocation();
            Utils.sendMessage( sender, Col.GREEN, npcName, " will respawn at its present location" );
        }
    }
    @Override
    public String getLongHelp() {
        if ( helpTxt == null ) {
            helpTxt = Utils.join( "do ", Col.GOLD, "/sentry spawn", Col.RESET, " to set the sentry's spawn point to its current location" );
        }
        return helpTxt;
    }
}
