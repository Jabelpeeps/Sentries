package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.SentryTrait;

public interface SentriesNumberCommand extends SentriesCommand {
    
    boolean call( CommandSender sender, 
                  String npcName, 
                  SentryTrait inst,
                  String number );
}
