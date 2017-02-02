package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class FollowDistanceCommand implements SentriesNumberCommand {

    @Getter private String shortHelp = "set how close the sentry follows when guarding";
    @Getter private String perm = S.PERM_FOLLOW_DIST;
    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            Utils.sendMessage( sender, S.Col.GOLD, npcName, "'s follow distance is ", 
                                                    String.valueOf( Math.sqrt( inst.followDistance ) ) );
        }
        else {
            int dist = Utils.string2Int( number );
            if ( dist < 1 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( dist > 32 ) dist = 32;
            
            inst.followDistance = dist * dist;
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s follow distance set to ", String.valueOf( dist ) );
        }
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.FOLLOW, " (#)", Col.RESET, 
                    ", where # is the number (1-32) of blocks that a sentry configured to guard will follow behind their guardees.",
                    System.lineSeparator(), "  If no number is given the current value is shown. (Default = 2)");
        }
        return helpTxt;
    }
}
