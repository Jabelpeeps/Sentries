package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class VoiceRangeCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {

        if ( number == null ) {
            Util.sendMessage( sender, Col.GOLD, npcName, "'s voice range is:- " + inst.voiceRange );
        }
        else {
            int range = Util.string2Int( number );
            if ( range < 0 ) {
                Util.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( range > 50 ) range = 50;
            
            inst.voiceRange = range;
            Util.sendMessage( sender, Col.GREEN, npcName, "'s voice range set to:- ", String.valueOf( range ) );
        }
    }

    @Override
    public String getShortHelp() { return "set the range of warnings and greetings"; }

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
    public String getPerm() { return S.PERM_WARNING_RANGE; }
}
