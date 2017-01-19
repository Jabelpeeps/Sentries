package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.SentryTrait;

public interface SentriesSimpleCommand extends SentriesCommand {
    
    public void call( CommandSender sender, String npcName, SentryTrait inst );

}
