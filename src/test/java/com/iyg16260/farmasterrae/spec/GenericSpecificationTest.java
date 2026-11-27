package com.iyg16260.farmasterrae.spec;

import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GenericSpecificationTest {

    @Test
    void likeWildcardsAndTheEscapeCharacterAreEscaped() {
        assertThat(GenericSpecification.escapeLikeWildcards("100%")).isEqualTo("100\\%");
        assertThat(GenericSpecification.escapeLikeWildcards("first_aid")).isEqualTo("first\\_aid");
        assertThat(GenericSpecification.escapeLikeWildcards("C:\\temp")).isEqualTo("C:\\\\temp");
        assertThat(GenericSpecification.escapeLikeWildcards("%_\\")).isEqualTo("\\%\\_\\\\");
    }

    @Test
    void textWithoutWildcardsIsLeftAsIs() {
        assertThat(GenericSpecification.escapeLikeWildcards("ibuprofeno 600 mg")).isEqualTo("ibuprofeno 600 mg");
        assertThat(GenericSpecification.escapeLikeWildcards("")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource (strings = {" ", "   ", "\t", " \n "})
    void blankKeywordsMeanNoSearch(String keyword) {
        assertThat(GenericSpecification.normalizeKeyword(keyword)).isNull();
    }

    @Test
    void keywordsAreTrimmed() {
        assertThat(GenericSpecification.normalizeKeyword("  ibuprofeno ")).isEqualTo("ibuprofeno");
        assertThat(GenericSpecification.normalizeKeyword("vitamina c")).isEqualTo("vitamina c");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource (strings = {" ", "   "})
    void blankKeywordsMatchNoEnumValue(String keyword) {
        // A space used to match every multi-word label ("Cuidado Personal", "Tarjeta de crédito"...)
        assertThat(Category.findByValueContainsIgnoreCase(keyword)).isEmpty();
        assertThat(PaymentMethod.findByValueContainsIgnoreCase(keyword)).isEmpty();
        assertThat(SaleStatus.findByValueContainsIgnoreCase(keyword)).isEmpty();
    }

    @Test
    void enumLabelsAreMatchedIgnoringCaseAndSurroundingSpaces() {
        assertThat(PaymentMethod.findByValueContainsIgnoreCase(" PAYPAL ")).containsExactly(PaymentMethod.PAYPAL);
        assertThat(Category.findByValueContainsIgnoreCase("cuidado"))
                .containsExactlyInAnyOrder(Category.CARE, Category.BEAUTY);
        assertThat(SaleStatus.findByValueContainsIgnoreCase("Pendiente")).containsExactly(SaleStatus.PENDING);
    }

    @Test
    void blankSearchesAreUnrestrictedSpecifications() {
        // No search means no predicate at all (Specification.unrestricted() returns a null predicate)
        assertThat(GenericSpecification.likeIgnoreCase(new String[]{"name"}, "   ")
                .toPredicate(null, null, null)).isNull();
        assertThat(GenericSpecification.likeIgnoreCase(new String[0], "x")
                .toPredicate(null, null, null)).isNull();
        assertThat(GenericSpecification.equal("category", null)
                .toPredicate(null, null, null)).isNull();
    }
}
