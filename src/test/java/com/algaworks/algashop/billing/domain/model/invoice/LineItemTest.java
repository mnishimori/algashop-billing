package com.algaworks.algashop.billing.domain.model.invoice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LineItemTest {

  @Test
  void shouldCreateLineItemWhenAllFieldsAreValid() {
    LineItem lineItem = LineItem.builder()
        .number(1)
        .name("Notebook")
        .amount(new BigDecimal("2500.00"))
        .build();

    assertThat(lineItem.getNumber()).isEqualTo(1);
    assertThat(lineItem.getName()).isEqualTo("Notebook");
    assertThat(lineItem.getAmount()).isEqualByComparingTo("2500.00");
  }

  @Test
  void shouldBeEqualWhenAllFieldsAreEqual() {
    LineItem first = LineItem.builder().number(1).name("Notebook").amount(new BigDecimal("2500.00")).build();
    LineItem second = LineItem.builder().number(1).name("Notebook").amount(new BigDecimal("2500.00")).build();

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  void shouldThrowNullPointerExceptionWhenNumberIsNull() {
    assertThatNullPointerException().isThrownBy(() -> LineItem.builder()
        .number(null).name("Notebook").amount(BigDecimal.TEN).build());
  }

  @Test
  void shouldThrowNullPointerExceptionWhenAmountIsNull() {
    assertThatNullPointerException().isThrownBy(() -> LineItem.builder()
        .number(1).name("Notebook").amount(null).build());
  }

  @Test
  void shouldThrowNullPointerExceptionWhenNameIsNull() {
    assertThatNullPointerException().isThrownBy(() -> LineItem.builder()
        .number(1).name(null).amount(BigDecimal.TEN).build());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenNameIsBlank(String blankName) {
    assertThatIllegalArgumentException().isThrownBy(() -> LineItem.builder()
        .number(1).name(blankName).amount(BigDecimal.TEN).build());
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenAmountIsZero() {
    assertThatIllegalArgumentException().isThrownBy(() -> LineItem.builder()
        .number(1).name("Notebook").amount(BigDecimal.ZERO).build());
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenAmountIsNegative() {
    assertThatIllegalArgumentException().isThrownBy(() -> LineItem.builder()
        .number(1).name("Notebook").amount(new BigDecimal("-0.01")).build());
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenNumberIsZero() {
    assertThatIllegalArgumentException().isThrownBy(() -> LineItem.builder()
        .number(0).name("Notebook").amount(BigDecimal.TEN).build());
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenNumberIsNegative() {
    assertThatIllegalArgumentException().isThrownBy(() -> LineItem.builder()
        .number(-1).name("Notebook").amount(BigDecimal.TEN).build());
  }
}
