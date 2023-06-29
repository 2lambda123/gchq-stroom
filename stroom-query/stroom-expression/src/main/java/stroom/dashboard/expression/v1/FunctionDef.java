package stroom.dashboard.expression.v1;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionDef {

    String UNDEFINED = "[UNDEFINED]";

    String name();

    /**
     * The html link anchor to the section of the documentation page for this function.
     * It should only need to be set if the name when converted into anchor format differs
     * from the actual anchor.
     */
    String helpAnchor() default UNDEFINED;

    /**
     * Any alias names for the function
     */
    String[] aliases() default {};

    /**
     * The single category of functions that this function signature belongs to unless overridden at the
     * signature level.
     * Defined as an array to allow us to not have one by default.
     */
    FunctionCategory[] commonCategory() default {};

    /**
     * An array of sub-categories that this function belongs to. The sub-categories represent a path
     * in a tree of categories from root to leaf. E.g. if the main category is String the sub categories
     * could be [Conversion, Case], i.e. String -> Conversion -> Case.
     * Can be overridden at the signature level.
     */
    String[] commonSubCategories() default {};

    /**
     * A description of what the function does that is common to all signatures unless overridden
     * at the signature level.
     */
    String commonDescription() default "";

    /**
     * A single return type that is common to all signatures unless overridden at the signature level.
     * You must specify either this or {@link FunctionSignature#returnType()}
     * Defined as an array to allow us to not have one by default.
     */
    Class<? extends Val>[] commonReturnType() default {};

    /**
     * A return description that is common to all signatures unless overridden at the signature level
     * You must specify either this or {@link FunctionSignature#returnDescription()}
     */
    String commonReturnDescription() default "";

    /**
     * All the overloaded function signatures for the method,
     * e.g. parseDate(dateStr) & parseDate(dateStr, format).
     * Must have at least one signature.
     */
    FunctionSignature[] signatures();
}
