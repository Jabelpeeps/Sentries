package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.SentryTrait;

public interface SentriesComplexCommand extends SentriesCommand {
    
    void call( CommandSender sender, // the sender of the command
                  String npcName,       // the NPC's name
                  SentryTrait inst,     // the NPC's SentryTrait
                  int nextArg,          // the next argument to be parsed
                  String... args );     // the arguments array.
  
}
