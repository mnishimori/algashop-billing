package com.algaworks.algashop.billing.domain.model.invoice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PayerTest {

  @Test
  void shouldCreatePayerWhenAllFieldsAreValid() {
    Address address = validAddress();

    Payer payer = validBuilder().address(address).build();

    assertThat(payer.getFullName()).isEqualTo("John Doe");
    assertThat(payer.getDocument()).isEqualTo("123.456.789-00");
    assertThat(payer.getPhone()).isEqualTo("11999999999");
    assertThat(payer.getEmail()).isEqualTo("john@example.com");
    assertThat(payer.getAddress()).isEqualTo(address);
  }

  @Test
  void shouldBeEqualWhenAllFieldsAreEqual() {
    assertThat(validBuilder().build()).isEqualTo(validBuilder().build());
  }

  @Test
  void shouldThrowNullPointerExceptionWhenRequiredFieldIsNull() {
    assertThatNullPointerException().isThrownBy(() -> validBuilder().fullName(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().document(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().phone(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().email(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().address(null).build());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenRequiredFieldIsBlank(String blankValue) {
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().fullName(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().document(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().phone(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().email(blankValue).build());
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenEmailIsInvalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().email("invalid-email").build());
  }

  private Payer.PayerBuilder validBuilder() {
    return Payer.builder()
        .fullName("John Doe")
        .document("123.456.789-00")
        .phone("11999999999")
        .email("john@example.com")
        .address(validAddress());
  }

  private Address validAddress() {
    return Address.builder()
        .street("Rua das Flores")
        .number("100")
        .neighborhood("Centro")
        .city("Sao Paulo")
        .state("SP")
        .zipCode("01000-000")
        .build();
  }
}
