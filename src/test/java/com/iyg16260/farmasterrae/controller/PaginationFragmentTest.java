package com.iyg16260.farmasterrae.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders {@code fragments/pagination.html} on its own, with pages the services would never return, to check that
 * the fragment itself never builds the page list from an out-of-range page number.
 */
class PaginationFragmentTest {

    private final SpringTemplateEngine engine = new SpringTemplateEngine();

    PaginationFragmentTest() {
        ClassLoaderTemplateResolver fragments = new ClassLoaderTemplateResolver();
        fragments.setPrefix("templates/");
        fragments.setSuffix(".html");
        fragments.setTemplateMode(TemplateMode.HTML);
        fragments.setCharacterEncoding("UTF-8");
        fragments.setResolvablePatterns(Set.of("fragments/*"));
        fragments.setOrder(1);
        StringTemplateResolver inline = new StringTemplateResolver();
        inline.setTemplateMode(TemplateMode.HTML);
        inline.setOrder(2);
        engine.addTemplateResolver(fragments);
        engine.addTemplateResolver(inline);
    }

    @Test
    void outOfRangePageNumberRendersTheLastPagesInsteadOfAHugeRange() {
        // Page index 107374182 of a 100-element listing: 5 pages exist (0..4)
        Page<String> outOfRange = new PageImpl<>(List.of(), PageRequest.of(107_374_182, 20), 100);

        String html = render(outOfRange);

        assertThat(html).contains("Página 5 de 5")
                .contains("aria-current=\"page\">5</span>")
                .contains(">4</button>")
                .contains(">Anterior</button>")
                .doesNotContain(">Siguiente</button>")
                .doesNotContain(">…</span>")
                .doesNotContain("107374181")
                .doesNotContain("107374183");
        assertThat(pageInputs(html)).containsExactly("3", "3"); // "Anterior" and the button of page 4
        assertThat(html.length()).isLessThan(5_000);
    }

    @Test
    void middlePageShowsItsNeighboursAndBothDirections() {
        String html = render(new PageImpl<>(List.of("x"), PageRequest.of(4, 10), 100));

        assertThat(html).contains("Página 5 de 10")
                .contains(">Anterior</button>")
                .contains(">Siguiente</button>")
                .contains("aria-hidden=\"true\">…</span>");
        assertThat(pageInputs(html)).containsExactly("3", "3", "5", "6", "5");
    }

    @Test
    void singlePageWithResultsRendersNothing() {
        String html = render(new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 2));

        assertThat(html).doesNotContain("ft-pagination").doesNotContain("No hay");
    }

    @Test
    void emptyListingShowsTheEmptyMessage() {
        String html = render(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(html).contains("No hay productos disponibles para mostrar.").doesNotContain("No hay más");
        assertThat(html).doesNotContain("aria-label=\"Paginación\"");
    }

    @Test
    void missingPageShowsTheEmptyMessage() {
        assertThat(render(null)).contains("No hay productos disponibles para mostrar.");
    }

    private String render(Page<?> page) {
        Context context = new Context(Locale.forLanguageTag("es"));
        context.setVariable("pageData", page);
        return engine.process(
                "<div th:replace=\"~{fragments/pagination :: pagination(${pageData}, '/products', 'productos')}\"></div>",
                context);
    }

    private static List<String> pageInputs(String html) {
        return Pattern.compile("name=\"page\" value=\"(\\d+)\"").matcher(html).results()
                .map(match -> match.group(1))
                .toList();
    }
}
