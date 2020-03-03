/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.util.shared;

import stroom.docref.SharedObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * List that knows how big the whole set is.
 */
public class BaseResultList<T extends SharedObject> extends SharedList<T> implements ResultList<T> {
    private static final long serialVersionUID = 6482769757822956315L;

    private PageResponse pageResponse;

    public BaseResultList() {
        // Default constructor necessary for GWT serialisation.
    }

    /**
     * @param exact
     *            is the complete size exactly correct
     */
    public BaseResultList(final List<T> list, final Long offset, final Long completeSize, final boolean exact) {
        super(list);
        pageResponse = new PageResponse(offset, size(), completeSize, exact);
    }

    public <R extends ResultPage<T>> R toResultPage(final R resultPage) {
        resultPage.setValues(new ArrayList<T>(this));
        resultPage.setPageResponse(pageResponse);
        return resultPage;
    }

    /**
     * Creates a list limited to a result page from a full list of results.
     * @param fullList The full list of results to create the result page from.
     * @param pageRequest The page request to limit the result list by.
     * @param <T> The type of list item.
     * @return A list limited to a result page from a full list of results.
     */
    public static <T extends SharedObject> BaseResultList<T> createPageLimitedList(final List<T> fullList, final PageRequest pageRequest) {
        if (pageRequest != null) {
            int offset = 0;
            if (pageRequest.getOffset() != null) {
                offset = pageRequest.getOffset().intValue();
            }

            int length = fullList.size() - offset;
            if (pageRequest.getLength() != null) {
                length = Math.min(length, pageRequest.getLength());
            }

            // If the page request will lead to a limited number of results then apply that limit here.
            if (offset != 0 || length < fullList.size()) {
                // Ideally we'd use List.subList here but can't as GWT can't serialise the returned list type.
//                final List<T> limited = fullList.subList(offset, offset + length);
                final List<T> limited = new ArrayList<>(length);
                for (int i = offset; i < offset + length; i++) {
                    limited.add(fullList.get(i));
                }

                return new BaseResultList<>(limited, (long) offset, (long) fullList.size(), true);
            }
        }

        return new BaseResultList<>(fullList, 0L, (long) fullList.size(), true);
    }

    /**
     * Used for full queries (not bounded).
     */
    public static <T extends SharedObject> BaseResultList<T> createUnboundedList(final List<T> realList) {
        if (realList != null) {
            return new BaseResultList<>(realList, 0L, (long) realList.size(), true);
        } else {
            return new BaseResultList<>(new ArrayList<T>(), 0L, 0L, true);
        }
    }

    /**
     * Used for filter queries (maybe bounded).
     */
    public static <T extends SharedObject> BaseResultList<T> createCriterialBasedList(final List<T> realList,
            final BaseCriteria baseCriteria) {
        return createPageResultList(realList, baseCriteria.getPageRequest(), null);
    }

    /**
     * Used for filter queries (maybe bounded).
     */
    public static <T extends SharedObject> BaseResultList<T> createCriterialBasedList(final List<T> realList,
            final BaseCriteria baseCriteria, final Long totalSize) {
        return createPageResultList(realList, baseCriteria.getPageRequest(), totalSize);
    }

    /**
     * Used for filter queries (maybe bounded).
     */
    public static <T extends SharedObject> BaseResultList<T> createPageResultList(final List<T> realList,
                                                                                      final PageRequest pageRequest,
                                                                                      final Long totalSize) {
        final boolean limited =pageRequest != null
                && pageRequest.getLength() != null;
        boolean moreToFollow = false;
        Long calulatedTotalSize = totalSize;
        long offset = 0;
        if (pageRequest != null && pageRequest.getOffset() != null) {
            offset = pageRequest.getOffset();
        }
        if (limited) {
            if (realList.size() > (pageRequest.getLength() + 1)) {
                // Here we check that if the query was supposed to be limited
                // make sure we have
                // get to process more that 1 + that limit. If this fails it
                // will be a coding error
                // or not applying the limit.
                throw new IllegalStateException(
                        "For some reason we returned more rows that we were limited to. Did you apply the restriction criteria?");
            }
        }

        // If we have not been given the total size see if we can work it out
        // based on hitting the end
        if (totalSize == null && limited) {
            // All our queries are + 1 on the limit so that we know there is
            // more to come
            moreToFollow = realList.size() > pageRequest.getLength();
            if (!moreToFollow) {
                calulatedTotalSize = pageRequest.getOffset() + realList.size();
            }
        }

        final BaseResultList<T> results = new BaseResultList<>(realList, offset, calulatedTotalSize, !moreToFollow);
        if (moreToFollow) {
            // All our queries are + 1 to we need to remove the last element
            results.remove(results.size() - 1);
            results.pageResponse = new PageResponse(results.pageResponse.getOffset(),
                    results.pageResponse.getLength() - 1, results.pageResponse.getTotal(),
                    results.pageResponse.isExact());
        }
        return results;
    }


    public PageResponse getPageResponse() {
        return pageResponse;
    }

    /**
     * @return the first item or null if the list is empty
     */
    public T getFirst() {
        if (size() > 0) {
            return get(0);
        } else {
            return null;
        }
    }

    @Override
    public int getStart() {
        if (pageResponse.getOffset() == null) {
            return 0;
        }
        return pageResponse.getOffset().intValue();
    }

    @Override
    public List<T> getValues() {
        return this;
    }

    @Override
    public int getSize() {
        if (pageResponse.getTotal() == null) {
            return getStart() + size();
        }
        return pageResponse.getTotal().intValue();
    }

    @Override
    public boolean isExact() {
        return pageResponse.isExact();
    }

    public static <T extends SharedObject> Collector<T, List<T>, BaseResultList<T>> collector(final PageRequest pageRequest) {

        // Explicit typing needed for GWT
        return new Collector<T, List<T>, BaseResultList<T>>() {

            long counter = 0L;

            @Override
            public Supplier<List<T>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<T>, T> accumulator() {
                return (accumulator, item) -> {
                    if (pageRequest == null
                        || (
                        counter++ >= pageRequest.getOffset()
                            && accumulator.size() < pageRequest.getLength())) {

                        accumulator.add(item);
                    }
                };
            }

            @Override
            public BinaryOperator<List<T>> combiner() {
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public Function<List<T>, BaseResultList<T>> finisher() {
                return accumulator ->
                    new BaseResultList<>
                        (accumulator,
                            pageRequest != null ? pageRequest.getOffset() : 0,
                            counter,
                            true);
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        };
    }
}
