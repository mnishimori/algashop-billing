package com.algaworks.algashop.billing.domain.model.invoice;

import com.algaworks.algashop.billing.domain.model.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSettingsTest {

  @Test
  void shouldCreateCreditCardPaymentSettings() {
    UUID creditCardId = UUID.randomUUID();

    PaymentSettings paymentSettings = PaymentSettings.brandNew(PaymentMethod.CREDIT_CARD, creditCardId);

    assertThat(paymentSettings.getId()).isNotNull();
    assertThat(paymentSettings.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(paymentSettings.getCreditCardId()).isEqualTo(creditCardId);
    assertThat(paymentSettings.getGatewayCode()).isNull();
  }

  @Test
  void shouldCreateGatewayBalancePaymentSettingsWithoutCreditCard() {
    PaymentSettings paymentSettings = PaymentSettings.brandNew(PaymentMethod.GATEWAY_BALANCE, null);

    assertThat(paymentSettings.getId()).isNotNull();
    assertThat(paymentSettings.getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(paymentSettings.getCreditCardId()).isNull();
    assertThat(paymentSettings.getGatewayCode()).isNull();
  }

  @Test
  void shouldThrowNullPointerExceptionWhenMethodIsNull() {
    assertThatNullPointerException().isThrownBy(() -> PaymentSettings.brandNew(null, UUID.randomUUID()));
  }

  @Test
  void shouldStoreCreditCardIdEvenWhenMethodIsGatewayBalance() {
    UUID creditCardId = UUID.randomUUID();

    PaymentSettings paymentSettings = PaymentSettings.brandNew(PaymentMethod.GATEWAY_BALANCE, creditCardId);

    assertThat(paymentSettings.getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(paymentSettings.getCreditCardId()).isEqualTo(creditCardId);
  }

  @Test
  void shouldThrowNullPointerExceptionWhenCreditCardMethodHasNoCreditCardId() {
    assertThatNullPointerException().isThrownBy(() -> PaymentSettings.brandNew(PaymentMethod.CREDIT_CARD, null));
  }

  @Test
  void shouldAssignGatewayCodeWhenNotAssignedYet() {
    PaymentSettings paymentSettings = PaymentSettings.brandNew(PaymentMethod.GATEWAY_BALANCE, null);

    paymentSettings.assignGatewayCode("GW-123");

    assertThat(paymentSettings.getGatewayCode()).isEqualTo("GW-123");
  }

  @Test
  void shouldThrowDomainExceptionWhenGatewayCodeIsAlreadyAssigned() {
    PaymentSettings paymentSettings = PaymentSettings.brandNew(PaymentMethod.GATEWAY_BALANCE, null);
    paymentSettings.assignGatewayCode("GW-123");

    assertThatThrownBy(() -> paymentSettings.assignGatewayCode("GW-456"))
        .isInstanceOf(DomainException.class)
        .hasMessage("Gateway code already assigned");
    assertThat(paymentSettings.getGatewayCode()).isEqualTo("GW-123");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenGatewayCodeIsBlank(String blankCode) {
    PaymentSettings paymentSettings = PaymentSettings.brandNew(PaymentMethod.GATEWAY_BALANCE, null);

    assertThatIllegalArgumentException().isThrownBy(() -> paymentSettings.assignGatewayCode(blankCode));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenGatewayCodeIsNull() {
    PaymentSettings paymentSettings = PaymentSettings.brandNew(PaymentMethod.GATEWAY_BALANCE, null);

    assertThatIllegalArgumentException().isThrownBy(() -> paymentSettings.assignGatewayCode(null));
  }
}
