package com.algaworks.algashop.billing.domain.model.invoice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AddressTest {

  @Test
  void shouldCreateAddressWhenAllFieldsAreValid() {
    Address address = validBuilder().build();

    assertThat(address.getStreet()).isEqualTo("Rua das Flores");
    assertThat(address.getNumber()).isEqualTo("100");
    assertThat(address.getComplement()).isEqualTo("Apto 10");
    assertThat(address.getNeighborhood()).isEqualTo("Centro");
    assertThat(address.getCity()).isEqualTo("Sao Paulo");
    assertThat(address.getState()).isEqualTo("SP");
    assertThat(address.getZipCode()).isEqualTo("01000-000");
  }

  @Test
  void shouldCreateAddressWhenComplementIsNull() {
    Address address = validBuilder().complement(null).build();

    assertThat(address.getComplement()).isNull();
  }

  @Test
  void shouldBeEqualWhenAllFieldsAreEqual() {
    assertThat(validBuilder().build()).isEqualTo(validBuilder().build());
  }

  @Test
  void shouldNotBeEqualWhenFieldsDiffer() {
    assertThat(validBuilder().build()).isNotEqualTo(validBuilder().city("Campinas").build());
  }

  @Test
  void shouldThrowNullPointerExceptionWhenRequiredFieldIsNull() {
    assertThatNullPointerException().isThrownBy(() -> validBuilder().street(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().number(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().neighborhood(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().city(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().state(null).build());
    assertThatNullPointerException().isThrownBy(() -> validBuilder().zipCode(null).build());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenRequiredFieldIsBlank(String blankValue) {
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().street(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().number(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().neighborhood(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().city(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().state(blankValue).build());
    assertThatIllegalArgumentException().isThrownBy(() -> validBuilder().zipCode(blankValue).build());
  }

  private Address.AddressBuilder validBuilder() {
    return Address.builder()
        .street("Rua das Flores")
        .number("100")
        .complement("Apto 10")
        .neighborhood("Centro")
        .city("Sao Paulo")
        .state("SP")
        .zipCode("01000-000");
  }
}
