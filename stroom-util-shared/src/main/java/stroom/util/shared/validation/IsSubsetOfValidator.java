package stroom.util.shared.validation;

import java.util.Collection;
import jakarta.validation.ConstraintValidator;

public interface IsSubsetOfValidator extends ConstraintValidator<IsSubsetOf, Collection<String>> {
    // De-couples the use of the constraint annotation from the implementation of
    // that constraint.
}
