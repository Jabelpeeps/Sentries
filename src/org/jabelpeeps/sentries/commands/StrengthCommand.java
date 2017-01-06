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
            double strength = Utils.string2Double( number );
            if ( strength < -1  || strength == 0 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( strength > 1000 ) strength = 1000;
            
            if ( strength == -1.0 ) {
                inst.strengthFromWeapon = true;
                if ( inst.updateStrength() )
                    Utils.sendMessage( sender, Col.GREEN, npcName, "'s strength is now taken from their weapon,", 
                            System.lineSeparator(), "the current value is:- ", String.valueOf( inst.strength ) );
                else 
                    Utils.sendMessage( sender, Col.RED, "There was an error setting the strength: value unchanged" );
                
                return;
            }
            inst.strengthFromWeapon = false;
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
            "  where # is the ammount (1-1000) of damage you want the sentry to do with each attack.",
            System.lineSeparator(), "If no number is given the current value is shown. (Default = 1)",
            System.lineSeparator(), "Set to -1 to have the strength taken from the held weapon." );
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_STRENGTH; }
}
