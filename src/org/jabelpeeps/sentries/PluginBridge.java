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
public interface PluginBridge {

    /**
     * Carries out any initialisation that the implementation requires.
     * 
     * The caller will check that the third party plugin is installed and active
     * before calling this method, so implementations can assume this to be the
     * case.
     * 
     * @return true - if the activation was successful.
     * @return false - if not.
     */
    public boolean activate();

    /**
     * Implementations may specify a string to be used for logging once a call
     * has been made to 'activate()'; this can either be a static string, or
     * dynamic (e.g. to explain a failure to activate.)
     * 
     * @return the string.
     */
    public String getActivationMessage();

    /** 
     * Method for use when a sentry is reloaded. The String 'args' is the 'TargetString' 
     * retrieved from the TargetType instances when the sentry was saved, and should contain 
     * the information needed to recreate the TargetType instance.
     */
    public void add( SentryTrait inst, String args );

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
