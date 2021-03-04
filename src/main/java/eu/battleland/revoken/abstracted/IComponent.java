package eu.battleland.revoken.abstracted;

public interface IComponent {
    /**
     * Called upon initialization of Component
     */
    public void initialize() throws Exception;
    /**
     * Called upon termination of Component
     */
    public void terminate();

    /**
     * Called upon reload request
     */
    public void reload();
}
