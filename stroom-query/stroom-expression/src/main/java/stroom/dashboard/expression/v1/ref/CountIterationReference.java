package stroom.dashboard.expression.v1.ref;

public class CountIterationReference extends CountReference {

    private final int iteration;

    CountIterationReference(final int index,
                            final String name,
                            final int iteration) {
        super(index, name);
        this.iteration = iteration;
    }
}
