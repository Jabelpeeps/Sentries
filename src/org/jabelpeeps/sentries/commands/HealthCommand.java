package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class HealthCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            Util.sendMessage( sender, Col.GOLD, npcName, "'s Health is:- ", String.valueOf( inst.maxHealth ) );
        }
        else {
            int HPs = Util.string2Int( number );
            if ( HPs < 1 ) {
                Util.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( HPs > 2000000 ) HPs = 2000000;
            
            inst.maxHealth = HPs;
            inst.setHealth( HPs );
            Util.sendMessage( sender, Col.GREEN, npcName, "'s health set to:- ", String.valueOf( HPs ) );
        }
    }

    @Override
    public String getShortHelp() { return "directly adjust a sentry's health"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.HEALTH, " (#)", Col.RESET, System.lineSeparator(),
                    "  where # is the number (1-2000000) of hit points you want the sentry to have.", System.lineSeparator(),
                    "  Note: Players usually have 20HP.  If no number is given the current value is shown.");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_HEALTH; }
}