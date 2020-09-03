package stroom.util.filter;

import stroom.util.shared.filter.FilterFieldDefinition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A wrapper for a collection of {@link FilterFieldMapper<T_ROW>}
 */
public class FilterFieldMappers<T_ROW> {

    private final Map<String, FilterFieldMapper<T_ROW>> map;
    private final List<FilterFieldMapper<T_ROW>> defaultFieldMappers;

    private FilterFieldMappers(final Map<String, FilterFieldMapper<T_ROW>> map) {
        this.map = map;
        // Pre-compute the list of defaults to save doing it on each lookup
        this.defaultFieldMappers = map.values().stream()
                .filter(fieldMapper -> fieldMapper.getFieldDefinition().isDefaultField())
                .collect(Collectors.toList());
    }

    @SafeVarargs
    public static <T_ROW> FilterFieldMappers<T_ROW> of(final FilterFieldMapper<T_ROW>... fieldMappers) {
        return of(Arrays.asList(fieldMappers));
    }

    public static <T_ROW> FilterFieldMappers<T_ROW> of(final Collection<FilterFieldMapper<T_ROW>> fieldMappers) {

        return new FilterFieldMappers<>(Optional.ofNullable(fieldMappers)
                .map(fieldMappers2 -> fieldMappers2.stream()
                        .collect(Collectors.toMap(
                                fieldMapper -> fieldMapper.getFieldDefinition().getFilterQualifier().toLowerCase(),
                                Function.identity())))
                .orElseGet(Collections::emptyMap));
    }

    /**
     * @return A {@link FilterFieldMappers} object for a row/record that is just a single {@link String} field, i.e.
     * a {@link List<String>}.
     */
    public static FilterFieldMappers<String> singleStringField() {
        return FilterFieldMappers.of(
                FilterFieldMapper.of(
                        FilterFieldDefinition.defaultField("Value"),
                        Function.identity()));
    }



    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public FilterFieldMapper<T_ROW> get(final String fieldQualifier) {
        return map.get(fieldQualifier);
    }

    public Set<String> getFieldQualifiers() {
        return map.keySet();
    }

    public Collection<FilterFieldMapper<T_ROW>> getFieldMappers() {
        return map.values();
    }

    public Collection<FilterFieldMapper<T_ROW>> getDefaultFieldMappers() {
        return defaultFieldMappers;
    }

    @Override
    public String toString() {
        return map.values().toString();
    }
}
