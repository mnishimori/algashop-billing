package com.algaworks.algashop.billing.domain.model.invoice;

import com.algaworks.algashop.billing.domain.model.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {

  @Test
  void shouldIssueInvoiceWithUnpaidStatusAndTotalAmount() {
    Set<LineItem> items = Set.of(
        lineItem(1, "Notebook", "2500.00"),
        lineItem(2, "Mouse", "150.50")
    );
    UUID customerId = UUID.randomUUID();

    OffsetDateTime before = OffsetDateTime.now();
    Invoice invoice = Invoice.issue("order-1", customerId, validPayer(), items);
    OffsetDateTime after = OffsetDateTime.now();

    assertThat(invoice.getId()).isNotNull();
    assertThat(invoice.getOrderId()).isEqualTo("order-1");
    assertThat(invoice.getCustomerId()).isEqualTo(customerId);
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("2650.50");
    assertThat(invoice.getItems()).containsExactlyInAnyOrderElementsOf(items);
    assertThat(invoice.getPayer()).isEqualTo(validPayer());
    assertThat(invoice.getPaymentSettings()).isNull();
    assertThat(invoice.getPaidAt()).isNull();
    assertThat(invoice.getCanceledAt()).isNull();
    assertThat(invoice.getCancelReason()).isNull();
    assertThat(invoice.getIssuedAt()).isBetween(before, after);
    assertThat(invoice.getExpiresAt()).isBetween(before.plusDays(3), after.plusDays(3));
  }

  @Test
  void shouldThrowNullPointerExceptionWhenIssuingWithNullCustomerId() {
    Set<LineItem> items = Set.of(lineItem(1, "Notebook", "10.00"));

    assertThatNullPointerException().isThrownBy(() -> Invoice.issue("order-1", null, validPayer(), items));
  }

  @Test
  void shouldThrowNullPointerExceptionWhenIssuingWithNullPayer() {
    Set<LineItem> items = Set.of(lineItem(1, "Notebook", "10.00"));

    assertThatNullPointerException().isThrownBy(() -> Invoice.issue("order-1", UUID.randomUUID(), null, items));
  }

  @Test
  void shouldThrowNullPointerExceptionWhenIssuingWithNullItems() {
    assertThatNullPointerException()
        .isThrownBy(() -> Invoice.issue("order-1", UUID.randomUUID(), validPayer(), null));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void shouldThrowIllegalArgumentExceptionWhenOrderIdIsBlank(String orderId) {
    Set<LineItem> items = Set.of(lineItem(1, "Notebook", "10.00"));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> Invoice.issue(orderId, UUID.randomUUID(), validPayer(), items));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenItemsIsEmpty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Invoice.issue("order-1", UUID.randomUUID(), validPayer(), new HashSet<>()));
  }

  @Test
  void shouldReturnUnmodifiableItems() {
    Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), validPayer(),
        Set.of(lineItem(1, "Notebook", "10.00")));

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> invoice.getItems().add(lineItem(2, "Mouse", "5.00")));
  }

  @Test
  void shouldReportUnpaidStatus() {
    Invoice invoice = issuedInvoice();

    assertThat(invoice.isUnpaid()).isTrue();
    assertThat(invoice.isPaid()).isFalse();
    assertThat(invoice.isCanceled()).isFalse();
  }

  @Test
  void shouldMarkUnpaidInvoiceAsPaid() {
    Invoice invoice = issuedInvoice();

    OffsetDateTime before = OffsetDateTime.now();
    invoice.markAsPaid();
    OffsetDateTime after = OffsetDateTime.now();

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    assertThat(invoice.isPaid()).isTrue();
    assertThat(invoice.isUnpaid()).isFalse();
    assertThat(invoice.getPaidAt()).isBetween(before, after);
  }

  @Test
  void shouldThrowDomainExceptionWhenMarkingPaidInvoiceAsPaid() {
    Invoice invoice = issuedInvoice();
    invoice.markAsPaid();

    assertThatThrownBy(invoice::markAsPaid)
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be marked as paid");
  }

  @Test
  void shouldThrowDomainExceptionWhenMarkingCanceledInvoiceAsPaid() {
    Invoice invoice = issuedInvoice();
    invoice.cancel("reason");

    assertThatThrownBy(invoice::markAsPaid)
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be marked as paid");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
  }

  @Test
  void shouldCancelInvoiceWithReason() {
    Invoice invoice = issuedInvoice();

    OffsetDateTime before = OffsetDateTime.now();
    invoice.cancel("customer request");
    OffsetDateTime after = OffsetDateTime.now();

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
    assertThat(invoice.isCanceled()).isTrue();
    assertThat(invoice.getCancelReason()).isEqualTo("customer request");
    assertThat(invoice.getCanceledAt()).isBetween(before, after);
  }

  @Test
  void shouldCancelPaidInvoice() {
    Invoice invoice = issuedInvoice();
    invoice.markAsPaid();

    invoice.cancel("chargeback");

    assertThat(invoice.isCanceled()).isTrue();
    assertThat(invoice.getCancelReason()).isEqualTo("chargeback");
  }

  @Test
  void shouldThrowDomainExceptionWhenCancelingAlreadyCanceledInvoice() {
    Invoice invoice = issuedInvoice();
    invoice.cancel("first");

    assertThatThrownBy(() -> invoice.cancel("second"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("is already canceled");
    assertThat(invoice.getCancelReason()).isEqualTo("first");
  }

  @Test
  void shouldAssignPaymentGatewayCodeWhenUnpaid() {
    Invoice invoice = issuedInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);

    invoice.assignPaymentGatewayCode("GW-123");

    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("GW-123");
  }

  @Test
  void shouldThrowDomainExceptionWhenAssigningGatewayCodeToPaidInvoice() {
    Invoice invoice = issuedInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
    invoice.markAsPaid();

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("GW-123"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
  }

  @Test
  void shouldThrowDomainExceptionWhenAssigningGatewayCodeToCanceledInvoice() {
    Invoice invoice = issuedInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
    invoice.cancel("reason");

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("GW-123"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
  }

  @Test
  void shouldThrowNullPointerExceptionWhenAssigningGatewayCodeWithoutPaymentSettings() {
    Invoice invoice = issuedInvoice();

    assertThatNullPointerException().isThrownBy(() -> invoice.assignPaymentGatewayCode("GW-123"));
  }

  @Test
  void shouldThrowDomainExceptionWhenGatewayCodeAlreadyAssignedThroughInvoice() {
    Invoice invoice = issuedInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
    invoice.assignPaymentGatewayCode("GW-123");

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("GW-456"))
        .isInstanceOf(DomainException.class)
        .hasMessage("Gateway code already assigned");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldThrowIllegalArgumentExceptionWhenAssigningBlankGatewayCodeThroughInvoice(String blankCode) {
    Invoice invoice = issuedInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);

    assertThatIllegalArgumentException().isThrownBy(() -> invoice.assignPaymentGatewayCode(blankCode));
  }

  @Test
  void shouldThrowNullPointerExceptionWhenChangingToCreditCardWithoutCreditCardId() {
    Invoice invoice = issuedInvoice();

    assertThatNullPointerException()
        .isThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, null));
    assertThat(invoice.getPaymentSettings()).isNull();
  }

  @Test
  void shouldThrowNullPointerExceptionWhenPaymentMethodIsNull() {
    Invoice invoice = issuedInvoice();

    assertThatNullPointerException()
        .isThrownBy(() -> invoice.changePaymentSettings(null, UUID.randomUUID()));
    assertThat(invoice.getPaymentSettings()).isNull();
  }

  @Test
  void shouldChangePaymentSettingsWhenUnpaid() {
    Invoice invoice = issuedInvoice();
    UUID creditCardId = UUID.randomUUID();

    invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, creditCardId);

    assertThat(invoice.getPaymentSettings()).isNotNull();
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isEqualTo(creditCardId);
  }

  @Test
  void shouldReplacePaymentSettingsWhenChangedTwice() {
    Invoice invoice = issuedInvoice();
    invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, UUID.randomUUID());
    UUID firstSettingsId = invoice.getPaymentSettings().getId();

    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);

    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(invoice.getPaymentSettings().getId()).isNotEqualTo(firstSettingsId);
  }

  @Test
  void shouldThrowDomainExceptionWhenChangingPaymentSettingsOfPaidInvoice() {
    Invoice invoice = issuedInvoice();
    invoice.markAsPaid();

    assertThatThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
    assertThat(invoice.getPaymentSettings()).isNull();
  }

  @Test
  void shouldThrowDomainExceptionWhenChangingPaymentSettingsOfCanceledInvoice() {
    Invoice invoice = issuedInvoice();
    invoice.cancel("reason");

    assertThatThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
  }

  @Test
  void shouldBeEqualWhenIdsAreEqual() {
    Invoice invoice = issuedInvoice();
    Invoice sameInvoice = new Invoice(invoice.getId(), "other", UUID.randomUUID(), null, null, null, null,
        BigDecimal.ONE, InvoiceStatus.PAID, null, new HashSet<>(), null, null);

    assertThat(invoice).isEqualTo(sameInvoice);
    assertThat(invoice).hasSameHashCodeAs(sameInvoice);
  }

  @Test
  void shouldNotBeEqualWhenIdsDiffer() {
    assertThat(issuedInvoice()).isNotEqualTo(issuedInvoice());
  }

  private Invoice issuedInvoice() {
    return Invoice.issue("order-1", UUID.randomUUID(), validPayer(), Set.of(lineItem(1, "Notebook", "100.00")));
  }

  private LineItem lineItem(Integer number, String name, String amount) {
    return LineItem.builder().number(number).name(name).amount(new BigDecimal(amount)).build();
  }

  private Payer validPayer() {
    return Payer.builder()
        .fullName("John Doe")
        .document("123.456.789-00")
        .phone("11999999999")
        .email("john@example.com")
        .address(Address.builder()
            .street("Rua das Flores")
            .number("100")
            .neighborhood("Centro")
            .city("Sao Paulo")
            .state("SP")
            .zipCode("01000-000")
            .build())
        .build();
  }
}
