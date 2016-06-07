package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class VoiceRangeCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, String number ) {

        if ( number == null ) {
            sender.sendMessage( String.join( "", Col.GOLD, npcName, "'s voice range is:- " + inst.warningRange ) );
        }
        else {
            int range = Util.string2Int( number );
            if ( range < 0 ) {
                sender.sendMessage( String.join( "", S.ERROR, number, S.ERROR_NOT_NUMBER ) );
                return true;
            }
            if ( range > 50 ) range = 50;
            
            inst.warningRange = range;
            sender.sendMessage( String.join( "", Col.GREEN, npcName, "'s voice range set to:- ", String.valueOf( range ) ) );
        }
        return true;
    }

    @Override
    public String getShortHelp() {
        return "set the range of warnings and greetings";
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.VOICE_RANGE, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the number (0-50) of blocks beyond the attack range that a sentry will warn, or greet, players.",
            System.lineSeparator(), "  If no number is given the current value is shown. (Default = 0)");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_WARNING_RANGE;
    }
}
