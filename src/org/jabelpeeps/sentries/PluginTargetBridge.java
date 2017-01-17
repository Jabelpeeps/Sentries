package org.jabelpeeps.sentries;

/**
 * An abstract class to act as a bridge between Sentries and other Server plugins.
 * <p>
 * Child classes need to interface with the plugin they are bridging to, in
 * order to be able to find out how the plugin groups players, and then decide
 * whether they are friend or foe.
 * <p>
 * Some such bridges are provided with Sentries, but in future it should be
 * possible for others to be added by third parties.
 */
public interface PluginTargetBridge extends PluginBridge {

    /**
     * @return a string to be used as the first part of the command argument to
     *         refer to this PluginBridge.  The lowercase version of the same string 
     *         is often used as the subCommand (one level below '/sentry').
     */
    public String getPrefix();

    /**
     * @return a short String giving info on how to access the commands added by this PluginBridge.
     */
    public String getCommandHelp();
    
}
