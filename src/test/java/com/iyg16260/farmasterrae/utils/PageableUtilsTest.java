package com.iyg16260.farmasterrae.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PageableUtilsTest {

    private static final Set<String> SORTABLE = Set.of("name", "price");

    @Test
    void allowedPropertyIsSortedInTheRequestedDirectionWithTheIdAsTieBreaker() {
        assertThat(PageableUtils.sortOf("price", "desc", SORTABLE).toList())
                .containsExactly(Sort.Order.desc("price"), Sort.Order.desc("id"));
    }

    @ParameterizedTest
    @ValueSource (strings = {"desc", "DESC", "Desc"})
    void descIsRecognisedInAnyCase(String dir) {
        assertThat(PageableUtils.sortOf("name", dir, SORTABLE).getOrderFor("name"))
                .isEqualTo(Sort.Order.desc("name"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource (strings = {"asc", "", "descending", "up", "desc;drop table products"})
    void anyOtherDirectionIsAscending(String dir) {
        assertThat(PageableUtils.sortOf("name", dir, SORTABLE).toList())
                .containsExactly(Sort.Order.asc("name"), Sort.Order.asc("id"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource (strings = {"nonExistentField", "password", "user.password", "orderDetails.product.name", "   ",
            "id", "NAME", "name,desc"})
    void unknownNestedOrNotAllowedPropertiesFallBackToTheDefaultOrder(String property) {
        assertThat(PageableUtils.sortOf(property, "desc", SORTABLE)).isEqualTo(PageableUtils.defaultSort());
        assertThat(PageableUtils.defaultSort().toList()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void requestedPropertyIsTrimmed() {
        assertThat(PageableUtils.sortOf("  price ", "asc", SORTABLE).getOrderFor("price")).isNotNull();
    }

    @Test
    void sortingByIdDoesNotRepeatTheTieBreaker() {
        assertThat(PageableUtils.sortOf("id", "desc", Set.of("id", "createdAt")).toList())
                .containsExactly(Sort.Order.desc("id"));
    }

    @Test
    void pageInsideTheRangeIsQueriedOnce() {
        FakeRepository repository = new FakeRepository(25);

        Page<Integer> page = PageableUtils.findClampedPage(1, 10, Sort.unsorted(), repository);

        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getContent()).containsExactlyElementsOf(range(10, 20));
        assertThat(repository.requestedPages()).containsExactly(1);
    }

    @ParameterizedTest
    @ValueSource (ints = {-1, -5, Integer.MIN_VALUE})
    void negativePageIsTheFirstPage(int requested) {
        FakeRepository repository = new FakeRepository(25);

        Page<Integer> page = PageableUtils.findClampedPage(requested, 10, Sort.unsorted(), repository);

        assertThat(page.getNumber()).isZero();
        assertThat(page.getContent()).containsExactlyElementsOf(range(0, 10));
        assertThat(repository.requestedPages()).containsExactly(0);
    }

    @ParameterizedTest
    @ValueSource (ints = {3, 9_999_999, 107_374_182})
    void pagePastTheEndIsReplacedByTheLastPage(int requested) {
        FakeRepository repository = new FakeRepository(25);

        Page<Integer> page = PageableUtils.findClampedPage(requested, 10, Sort.unsorted(), repository);

        assertThat(page.getNumber()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).containsExactlyElementsOf(range(20, 25));
        assertThat(repository.requestedPages()).containsExactly(requested, 2);
    }

    @Test
    void hugePageNeverAsksForAnOffsetBeyondAnInt() {
        FakeRepository repository = new FakeRepository(45);

        Page<Integer> page = PageableUtils.findClampedPage(Integer.MAX_VALUE, 20, Sort.unsorted(), repository);

        assertThat(repository.requested).allSatisfy(pageable ->
                assertThat(pageable.getOffset()).isLessThanOrEqualTo(Integer.MAX_VALUE));
        assertThat(page.getNumber()).isEqualTo(2);
        assertThat(page.getContent()).containsExactlyElementsOf(range(40, 45));
    }

    @Test
    void emptyResultIsAlwaysReportedAsTheFirstPageWithoutRedundantQuery() {
        FakeRepository repository = new FakeRepository(0);

        Page<Integer> page = PageableUtils.findClampedPage(9_999_999, 10, Sort.unsorted(), repository);

        assertThat(page.getNumber()).isZero();
        assertThat(page.getTotalPages()).isZero();
        assertThat(page.getContent()).isEmpty();
        assertThat(repository.requestedPages()).containsExactly(9_999_999);
    }

    @Test
    void theSortIsPassedToEveryQuery() {
        FakeRepository repository = new FakeRepository(5);
        Sort sort = PageableUtils.sortOf("price", "desc", SORTABLE);

        PageableUtils.findClampedPage(50, 2, sort, repository);

        assertThat(repository.requested).extracting(Pageable::getSort).containsOnly(sort);
    }

    private static List<Integer> range(int fromInclusive, int toExclusive) {
        return IntStream.range(fromInclusive, toExclusive).boxed().toList();
    }

    /** Behaves like a Spring Data repository over the values {@code 0..total-1} and records every request */
    private static final class FakeRepository implements Function<Pageable, Page<Integer>> {
        private final int total;
        private final List<Pageable> requested = new ArrayList<>();

        FakeRepository(int total) {
            this.total = total;
        }

        @Override
        public Page<Integer> apply(Pageable pageable) {
            requested.add(pageable);
            long from = pageable.getOffset();
            List<Integer> content = from >= total
                    ? List.of()
                    : range((int) from, (int) Math.min(from + pageable.getPageSize(), total));
            return new PageImpl<>(content, pageable, total);
        }

        List<Integer> requestedPages() {
            return requested.stream().map(Pageable::getPageNumber).toList();
        }
    }
}
