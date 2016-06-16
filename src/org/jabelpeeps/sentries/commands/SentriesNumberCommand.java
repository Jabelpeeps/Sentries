package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.SentryTrait;

public interface SentriesNumberCommand extends SentriesCommand {
    
    void call( CommandSender sender, 
                  String npcName, 
                  SentryTrait inst,
                  String number );
}
