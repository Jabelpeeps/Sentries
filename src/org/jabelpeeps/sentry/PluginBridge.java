package org.jabelpeeps.sentry;

import org.bukkit.entity.Player;

/**
 * An abstract class to act as a bridge between Sentry and other Server plugins.
 * <p>
 * Child classes need to interface with the plugin they are bridging to, in
 * order to be able to find out how the plugin groups players, and then decide
 * whether they are friend or foe.
 * <p>
 * It is suggested the child classes implement some form of caching of target
 * and ignore information in order to avoid the potential for causing lag during
 * battles.
 * <p>
 * Some such bridges are provided with Sentry, but in future it should be
 * possible for others to be added by third parties.
 */
public abstract class PluginBridge {

    private final int bitFlag;

    public int getBitFlag() {
        return bitFlag;
    }

    protected PluginBridge( int flag ) {
        bitFlag = flag;
    }

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
    public abstract boolean activate();

    /**
     * Implementations may specify a string to be used for logging once a call
     * has been made to 'activate()'; this can either be a static string, or
     * dynamic (e.g. to explain a failure to activate.)
     * 
     * @return the string.
     */
    public abstract String getActivationMessage();

    /**
     * Determines whether the supplied Player is a valid target of the supplied
     * SentryInstance.
     * <p>
     * This method should return a result as quickly as possible, so that server
     * performance is not affected if it is called often.
     * 
     * @param entity
     *            - the possible target player
     * @param inst
     *            - the SentryInstance that is asking.
     * @return true - if the player is a valid target.
     */
    public abstract boolean isTarget( Player player, SentryInstance inst );

    /**
     * Determines whether the supplied Player should be ignored as a possible
     * target of the supplied SentryInstance.
     * <p>
     * This method should return a result as quickly as possible, so that server
     * performance is not affected if it is called often.
     * 
     * @param entity
     *            - the player that should possibly be ignored.
     * @param inst
     *            - the SentryInstance that is asking.
     * @return true - if the player should be ignored.
     */
    public abstract boolean isIgnoring( Player player, SentryInstance inst );

    /**
     * Adds an entity - identified by the supplied string - as either a target
     * or ignore for the supplied for the supplied SentryInstance.
     * <p>
     * The PluginBridge should achieve this task without modifying the
     * SentryInstance (which knows nothing about the third party plugin) but
     * should store relevant references in preparation for a call to
     * 'isTarget()' or 'isIgnoring()'
     * 
     * @param target
     *            - a String identifying the entity to reference (the exact
     *            contents can vary, as long as the pluginBridge knows how to
     *            parse the String).
     * @param inst
     *            - the SentryInstance instance that will have the target
     *            recorded against it.
     * @param asTarget
     *            - send true to add to the targets list, false to add to
     *            ignores.
     * @return a string that will be displayed to the player (either for success
     *         or failure)
     */
    public abstract String add( String target, SentryInstance inst, boolean asTarget );

    /**
     * Removes the entity - identified by the supplied string - as either a
     * target or ignore for the supplied for the supplied SentryInstance.
     * <p>
     * The PluginBridge should achieve this task without modifying the
     * SentryInstance (which knows nothing about the third party plugin) but
     * should store relevant references in preparation for a call to
     * 'isTarget()' or 'isIgnoring()'
     * 
     * @param target
     *            - a String identifying the entity to reference (the exact
     *            contents can vary, as long as the pluginBridge knows how to
     *            parse the String).
     * @param inst
     *            - the SentryInstance instance that will have the target
     *            recorded against it.
     * @param fromTargets
     *            - send true to remove from the targets list, false to remove
     *            from ignores.
     * @return a string that will be displayed to the player (either for success
     *         or failure)
     */
    public abstract String remove( String entity, SentryInstance inst, boolean fromTargets );

    /**
     * @return a string to be used as the first part of the command argument to
     *         refer to this PluginBridge.
     */
    public abstract String getPrefix();

    /**
     * @return the help text describing how to identify targets and ignores for
     *         this PluginBridge - so that they will be recognised when parsed
     *         by 'addTarget()' and 'addIgnore()'
     */
    public abstract String getCommandHelp();

    /**
     * A method to check whether the supplied SentryInstance has any targets or
     * ignores tracked by this bridge.
     * 
     * @param inst
     *            - the SentryInstance instance to check
     * @param asTarget
     *            - supply true to check target list(s), and false to check
     *            ignores.
     * @return true - if the SentryInstance is listed.
     */
    public abstract boolean isListed( SentryInstance inst, boolean asTarget );
}
