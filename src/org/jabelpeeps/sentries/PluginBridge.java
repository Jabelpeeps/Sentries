package org.jabelpeeps.sentries;

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
    boolean activate();

    /**
     * Implementations may specify a string to be used for logging once a call
     * has been made to 'activate()'; this can either be a static string, or
     * dynamic (e.g. to explain a failure to activate.)
     * 
     * @return the string.
     */
    String getActivationMessage();

}
