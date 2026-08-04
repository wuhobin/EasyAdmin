package com.nexora.monitor.domain.form;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineSessionQueryFormTest {

    @Test
    void rejectsOversizedSearchesAndInvalidPaging() {
        OnlineSessionQueryForm form = new OnlineSessionQueryForm();
        form.setKeyword("x".repeat(101));
        form.setIp("1".repeat(46));
        form.setPageNum(0);
        form.setPageSize(101);

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(form))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(
                            "keyword", "ip", "pageNum", "pageSize");
        }
    }

    @Test
    void validatesSearchLengthAfterTrimming() {
        OnlineSessionQueryForm form = new OnlineSessionQueryForm();
        form.setKeyword("  " + "x".repeat(100) + "  ");
        form.setIp("  " + "1".repeat(45) + "  ");

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(form)).isEmpty();
        }
        assertThat(form.getKeyword()).hasSize(100);
        assertThat(form.getIp()).hasSize(45);
    }
}
