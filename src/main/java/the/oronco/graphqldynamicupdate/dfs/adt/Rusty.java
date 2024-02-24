package the.oronco.graphqldynamicupdate.dfs.adt;

public interface Rusty<J> {

    /**
     * Turn the rust like type into its java equivalent.
     * @return a java equivalent depending on the type of this
     */
    J j();
}
