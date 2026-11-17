package com.algaworks.algashop.billing.domain.model.creditcard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CreditCardTest {

  @Test
  void shouldCreateBrandNewCreditCardWhenAllFieldsAreValid() {
    UUID customerId = UUID.randomUUID();

    OffsetDateTime before = OffsetDateTime.now();
    CreditCard creditCard = CreditCard.brandNew(customerId, "1234", "VISA", 12, 2030, "GW-CARD-1");
    OffsetDateTime after = OffsetDateTime.now();

    assertThat(creditCard.getId()).isNotNull();
    assertThat(creditCard.getCustomerId()).isEqualTo(customerId);
    assertThat(creditCard.getLastNumbers()).isEqualTo("1234");
    assertThat(creditCard.getBrand()).isEqualTo("VISA");
    assertThat(creditCard.getExpMonth()).isEqualTo(12);
    assertThat(creditCard.getExpYear()).isEqualTo(2030);
    assertThat(creditCard.getGatewayCode()).isEqualTo("GW-CARD-1");
    assertThat(creditCard.getCreatedAt()).isBetween(before, after);
  }

  @Test
  void shouldThrowNullPointerExceptionWhenCustomerIdIsNull() {
    assertThatNullPointerException()
        .isThrownBy(() -> CreditCard.brandNew(null, "1234", "VISA", 12, 2030, "GW-CARD-1"));
  }

  @Test
  void shouldThrowNullPointerExceptionWhenExpMonthIsNull() {
    assertThatNullPointerException()
        .isThrownBy(() -> CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", null, 2030, "GW-CARD-1"));
  }

  @Test
  void shouldThrowNullPointerExceptionWhenExpYearIsNull() {
    assertThatNullPointerException()
        .isThrownBy(() -> CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, null, "GW-CARD-1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenLastNumbersIsBlank(String blankValue) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CreditCard.brandNew(UUID.randomUUID(), blankValue, "VISA", 12, 2030, "GW-CARD-1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenBrandIsBlank(String blankValue) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CreditCard.brandNew(UUID.randomUUID(), "1234", blankValue, 12, 2030, "GW-CARD-1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenGatewayCodeIsBlankOnCreation(String blankValue) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, blankValue));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenGatewayCodeIsNullOnCreation() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, null));
  }

  @Test
  void shouldSetGatewayCodeWhenValid() {
    CreditCard creditCard = CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, "GW-CARD-1");

    creditCard.setGatewayCode("GW-CARD-2");

    assertThat(creditCard.getGatewayCode()).isEqualTo("GW-CARD-2");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenSettingBlankGatewayCode(String blankValue) {
    CreditCard creditCard = CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, "GW-CARD-1");

    assertThatIllegalArgumentException().isThrownBy(() -> creditCard.setGatewayCode(blankValue));
    assertThat(creditCard.getGatewayCode()).isEqualTo("GW-CARD-1");
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSettingNullGatewayCode() {
    CreditCard creditCard = CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, "GW-CARD-1");

    assertThatIllegalArgumentException().isThrownBy(() -> creditCard.setGatewayCode(null));
    assertThat(creditCard.getGatewayCode()).isEqualTo("GW-CARD-1");
  }

  @Test
  void shouldBeEqualWhenIdsAreEqual() {
    CreditCard creditCard = CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, "GW-CARD-1");
    CreditCard sameCard = new CreditCard(creditCard.getId(), null, null, null, null, null, null, null);

    assertThat(creditCard).isEqualTo(sameCard);
    assertThat(creditCard).hasSameHashCodeAs(sameCard);
  }

  @Test
  void shouldNotBeEqualWhenIdsDiffer() {
    CreditCard first = CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, "GW-CARD-1");
    CreditCard second = CreditCard.brandNew(UUID.randomUUID(), "1234", "VISA", 12, 2030, "GW-CARD-1");

    assertThat(first).isNotEqualTo(second);
  }
}
