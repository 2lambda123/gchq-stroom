package stroom.data.meta.shared;

import stroom.query.api.v2.ExpressionOperator;

import java.util.Optional;

public interface DataSecurityFilter {
    Optional<ExpressionOperator> getExpression(String permission);
}
