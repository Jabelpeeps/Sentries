package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class StrengthCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName, "'s strength is:- ", String.valueOf( inst.strength ) );
        }
        else {
            int strength = Utils.string2Int( number );
            if ( strength < 1 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }

            if ( strength > 2000000 ) strength = 2000000;
            
            inst.strength = strength;
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s strength set to:- ", String.valueOf( strength ) );
        }
    }

    @Override
    public String getShortHelp() { return "directly set attack damage for a sentry"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.STRENGTH, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the ammount (1-2000000) of damage you want the sentry to do with each attack.",
            System.lineSeparator(), "If no number is given the current value is shown. (Default = 1)");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_STRENGTH; }
}
