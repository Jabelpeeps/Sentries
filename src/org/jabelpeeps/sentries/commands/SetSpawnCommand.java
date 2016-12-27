package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class SetSpawnCommand implements SentriesComplexCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        LivingEntity entity = inst.getMyEntity();
        
        if ( entity == null ) 
            sender.sendMessage( Col.RED.concat( "You cannot set a spawn point while a sentry is dead" ) );
        else {
            inst.spawnLocation = entity.getLocation();
            Utils.sendMessage( sender, Col.GREEN, npcName, " will respawn at its present location" );
        }
    }
    
    @Override
    public String getShortHelp() { return "set the sentry's spawn point"; }
    
    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry spawn", Col.RESET, " to set the sentry's spawn point to its current location" );
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_SET_SPAWN; }
}
