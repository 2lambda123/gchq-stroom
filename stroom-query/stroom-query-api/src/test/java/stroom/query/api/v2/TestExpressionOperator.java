package stroom.query.api.v2;

import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.test.common.TestUtil;

import io.vavr.Tuple;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

class TestExpressionOperator {

    @TestFactory
    Stream<DynamicTest> testContainsField() {

        return TestUtil.buildDynamicTestStream()
                .withInputTypes(String.class, ExpressionOperator.class)
                .withOutputType(Boolean.class)
                .withTestFunction(testCase -> {
                    final var field = testCase.getInput()._1;
                    final var expressionItem = testCase.getInput()._2;
                    return expressionItem.containsField(field);
                })
                .withSimpleEqualityAssertion()

                .addCase(Tuple.of("foo", ExpressionOperator.builder()
                        .op(Op.NOT)
                        .build()), false)

                .addCase(Tuple.of("foo", ExpressionOperator.builder()
                        .addTerm(ExpressionTerm.builder()
                                .field("bar")
                                .condition(Condition.EQUALS)
                                .value("123")
                                .build())
                        .build()), false)

                .addCase(Tuple.of(null, ExpressionOperator.builder()
                        .addTerm(ExpressionTerm.builder().field("foo").build())
                        .build()), false)

                .addCase(Tuple.of("foo", ExpressionOperator.builder()
                        .addTerm(ExpressionTerm.builder().field("foo").build())
                        .build()), true)

                .addCase(Tuple.of("foo", ExpressionOperator.builder()
                        .addTerm(ExpressionTerm.builder().field("bar").build())
                        .addTerm(ExpressionTerm.builder().field("foo").build())
                        .build()), true)

                .addCase(Tuple.of("foo", ExpressionOperator.builder()
                        .addOperator(ExpressionOperator.builder()
                                .addOperator(ExpressionOperator.builder()
                                        .addTerm(ExpressionTerm.builder().field("foo").build())
                                        .build())
                                .build())
                        .build()), true)

                .build();
    }
}
