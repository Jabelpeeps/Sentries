package org.jabelpeeps.sentries.commands;

import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class HealthCommand implements SentriesNumberCommand {

    private String helpTxt;
    @Getter private String shortHelp = "directly adjust a sentry's max health";
    @Getter private String perm = S.PERM_HEALTH;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName, "'s Health is:- ", String.valueOf( inst.maxHealth ) );
        }
        else {
            int HPs = Utils.string2Int( number );
            if ( HPs < 1 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( HPs > 2000000 ) HPs = 2000000;
            
            inst.maxHealth = HPs;
            ((LivingEntity) inst.getNPC().getEntity()).getAttribute( Attribute.GENERIC_MAX_HEALTH ).setBaseValue( HPs );
            inst.setHealth( HPs );
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s health set to:- ", String.valueOf( HPs ) );
        }
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.HEALTH, " (#)", Col.RESET, System.lineSeparator(),
                    "  where # is the number (1-2000000) of hit points you want the sentry to have.", System.lineSeparator(),
                    "  Note: Players usually have 20HP.  If no number is given the current value is shown.");
        }
        return helpTxt;
    }
}