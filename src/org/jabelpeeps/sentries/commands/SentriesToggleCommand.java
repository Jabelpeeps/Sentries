package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.SentryTrait;

public interface SentriesToggleCommand extends SentriesCommand {
    
    void call( CommandSender sender, 
                  String npcName, 
                  SentryTrait inst, 
                  Boolean set );
}
