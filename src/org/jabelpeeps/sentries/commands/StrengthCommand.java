package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class StrengthCommand implements SentriesNumberCommand {

    @Getter private String shortHelp = "directly set attack damage for a sentry";
    @Getter private String perm = S.PERM_STRENGTH;
    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        if ( number == null ) {
            if ( inst.strengthFromWeapon ) 
                Utils.sendMessage( sender, Col.GREEN, npcName, "'s strength of ", Col.RESET, 
                        String.valueOf( inst.strength ), Col.GREEN, " was calculated from their held item" );
            else    
                Utils.sendMessage( sender, Col.GREEN, npcName, "'s strength is set to ", Col.RESET, 
                        String.valueOf( inst.strength ) );
        }
        else {
            double strength = Utils.string2Double( number );
            if ( strength < -1  || strength == 0 || strength > 1000 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            
            if ( strength == -1.0 ) {
                inst.strengthFromWeapon = true;
                if ( !inst.updateStrength() ) {
                    Utils.sendMessage( sender, Col.RED, "There was an error setting the strength: value unchanged" );
                    return;
                }
            }
            else {
                inst.strengthFromWeapon = false;
                inst.strength = strength;
            }
            call( sender, npcName, inst, null );
        }
    }
    
    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = Utils.join( "do ", Col.GOLD, "/sentry ", S.STRENGTH, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the ammount (1-1000) of damage you want the sentry to do with each attack.",
            System.lineSeparator(), "If no number is given the current value is shown. (Default = 1)",
            System.lineSeparator(), "Set to -1 to have the strength taken from the held weapon." );
        }
        return helpTxt;
    }
}
