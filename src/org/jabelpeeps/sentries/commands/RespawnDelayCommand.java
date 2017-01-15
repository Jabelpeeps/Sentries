package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class RespawnDelayCommand implements SentriesNumberCommand {

    private String helpTxt;
    @Getter private String shortHelp = "set the delay before a sentry respawns";
    @Getter private String perm = S.PERM_RESPAWN_DELAY;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            respawnCommandMessage( inst.respawnDelay, npcName, sender );
        }
        else {
            int respawn = Utils.string2Int( number );
            if ( respawn < -1 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( respawn > 2000000 ) respawn = 2000000;
            
            inst.respawnDelay = respawn;
            Utils.sendMessage( sender, Col.GREEN, "Command Successfull!" );
            respawnCommandMessage( inst.respawnDelay, npcName, sender );
        }
    }
    /**
     * Convenience method to send a formatted message to the player regarding
     * the respawn status of the npc.
     * 
     * @param value
     *            - the number of seconds set as the respawn value.
     * @param npc
     *            - the npc
     * @param player
     *            - the player who sent the command.
     */
    static void respawnCommandMessage( int value, String npcName, CommandSender player ) {

        if ( value == -1 )
            Utils.sendMessage( player, Col.GOLD, npcName, " will be deleted upon death" );
        if ( value == 0 )
            Utils.sendMessage( player, Col.GOLD, npcName, " will not automatically respawn" );
        if ( value > 0 )
            Utils.sendMessage( player, Col.GOLD, npcName, " currently respawns after ", String.valueOf( value ), " ", S.SECONDS );
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.RESPAWN, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the delay in seconds before the sentry will respawn when killed. (max value = 2000000)",
            System.lineSeparator(), "  If set to 0, the sentry will not respawn automatically.",
            System.lineSeparator(), "  If set to -1, the sentry will be deleted on death.",
            System.lineSeparator(), "  If no number is given the current value is shown." );
        }
        return helpTxt;
    }
}
