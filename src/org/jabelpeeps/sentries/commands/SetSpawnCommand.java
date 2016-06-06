package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.SentriesCommand;
import org.jabelpeeps.sentries.SentryTrait;

import net.citizensnpcs.api.npc.NPC;


public class SetSpawnCommand implements SentriesCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, NPC npc, String npcName, SentryTrait inst, Boolean set, int nextArg, String... args ) {
        
        if ( CommandHandler.checkCommandPerm( S.SPAWN, sender ) ) {

            if ( npc.getEntity() == null ) 
                sender.sendMessage( S.Col.RED.concat( "Cannot set spawn while a sentry is dead" ) );
            else {
                inst.spawnLocation = npc.getEntity().getLocation();
                sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "will respawn at its present location" ) );
            }
            return true;
        }
        return false;
    }
    
    @Override
    public String getShortHelp() {
        return "";
    }
    
    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", S.Col.GOLD, "/sentry spawn", S.Col.RESET, " to set the sentry's spawn point to its current location" );
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_SPAWN;
    }
}
