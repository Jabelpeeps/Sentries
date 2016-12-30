package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class RangeCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {

        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName, "'s attack range is:- ", String.valueOf( inst.range ) );
        }
        else {
            int range = Utils.string2Int( number );
            if ( range < 1 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( range > 100 ) range = 100;
            
            inst.range = range;
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s attack range set to:- ", String.valueOf( range ) );
        }
    }

    @Override
    public String getShortHelp() { return "set the sentry's target detection range"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.RANGE, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the number (1-100) of blocks distance at which the sentry will assess entities as possible targets",
            System.lineSeparator(), "If no number is given the current value is shown. (Default = 10)" );
        }
        return helpTxt;
    }
    @Override
    public String getPerm() { return S.PERM_RANGE; }
}
