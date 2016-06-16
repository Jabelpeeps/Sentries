package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class FollowDistanceCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            sender.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s follow distance is ", String.valueOf( inst.followDistance ) ) );
        }
        else {
            int dist = Util.string2Int( number );
            if ( dist < 0 ) {
                Util.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( dist > 32 ) dist = 32;
            
            inst.followDistance = dist * dist;
            Util.sendMessage( sender, Col.GREEN, npcName, "'s follow distance set to ", String.valueOf( dist ) );
        }
    }

    @Override
    public String getShortHelp() { return "set how close the sentry follows when guarding"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.FOLLOW, " (#)", Col.RESET, 
                    ", where # is the number (0-32) of blocks that a sentry configured to guard will follow behind their guardees.",
                    System.lineSeparator(), "  If no number is given the current value is shown. (Default = 4)");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_FOLLOW_DIST; }
}
