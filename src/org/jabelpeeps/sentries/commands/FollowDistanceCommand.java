package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentriesNumberCommand;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class FollowDistanceCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            sender.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s follow distance is ", String.valueOf( inst.followDistance ) ) );
        }
        else {
            int dist = Util.string2Int( number );
            if ( dist < 0 ) {
                sender.sendMessage( String.join( "", S.ERROR, number, S.ERROR_NOT_NUMBER ) );
                return true;
            }
            if ( dist > 32 ) dist = 32;
            
            inst.followDistance = dist * dist;
            sender.sendMessage( String.join( "", Col.GREEN, npcName, "'s follow distance set to ", String.valueOf( dist ) ) );
        }
        return true;
    }

    @Override
    public String getShortHelp() {
        return "set how close the sentry follows when guarding";
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = "";
            Col.GOLD.concat( "Usage: /sentry follow [#]. Default is 4. " );
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_FOLLOW_DIST;
    }
}
