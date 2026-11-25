package com.algaworks.algashop.billing.application.invoice.management;

import com.algaworks.algashop.billing.IntegrationTest;
import com.algaworks.algashop.billing.domain.model.DomainException;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCard;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.algaworks.algashop.billing.domain.model.invoice.Invoice;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.algaworks.algashop.billing.domain.model.invoice.PaymentMethod;
import com.algaworks.algashop.billing.domain.model.invoice.payment.Payment;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@IntegrationTest
class InvoiceManagementApplicationServiceIT {

  @Autowired
  private InvoiceManagementApplicationService service;

  @Autowired
  private InvoiceRepository invoiceRepository;

  @Autowired
  private CreditCardRepository creditCardRepository;

  @MockitoBean
  private PaymentGatewayService paymentGatewayService;

  @Test
  void shouldGenerateAndPersistInvoiceWithGatewayBalance() {
    GenerateInvoiceInput input = input("order-gb", PaymentMethod.GATEWAY_BALANCE, null);

    UUID id = service.generate(input);

    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    assertThat(invoice.getOrderId()).isEqualTo("order-gb");
    assertThat(invoice.getCustomerId()).isEqualTo(input.getCustomerId());
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("2650.50");
    assertThat(invoice.getItems()).hasSize(2);
    assertThat(invoice.getPayer().getFullName()).isEqualTo("John Doe");
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
  }

  @Test
  void shouldGenerateAndPersistInvoiceWithExistingCreditCard() {
    CreditCard card = creditCardRepository.saveAndFlush(
        CreditCard.brandNew(UUID.randomUUID(), "1234", "Visa", 12, 2030, "gw-card"));

    UUID id = service.generate(input("order-cc", PaymentMethod.CREDIT_CARD, card.getId()));

    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isEqualTo(card.getId());
  }

  @Test
  void shouldThrowCreditCardNotFoundExceptionWhenCreditCardDoesNotExist() {
    GenerateInvoiceInput input = input("order-nocc", PaymentMethod.CREDIT_CARD, UUID.randomUUID());

    assertThatThrownBy(() -> service.generate(input)).isInstanceOf(CreditCardNotFoundException.class);

    assertThat(invoiceRepository.existsByOrderId("order-nocc")).isFalse();
  }

  @Test
  void shouldThrowDomainExceptionWhenInvoiceAlreadyExistsForOrder() {
    service.generate(input("order-dup", PaymentMethod.GATEWAY_BALANCE, null));

    assertThatThrownBy(() -> service.generate(input("order-dup", PaymentMethod.GATEWAY_BALANCE, null)))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void shouldMarkInvoiceAsPaidWhenPaymentIsPaid() {
    UUID id = service.generate(input("order-paid", PaymentMethod.GATEWAY_BALANCE, null));
    when(paymentGatewayService.capture(any(PaymentRequest.class)))
        .thenReturn(new Payment("gw-1", id, PaymentMethod.GATEWAY_BALANCE, PaymentStatus.PAID));

    service.processPayment(id);

    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    assertThat(invoice.getPaidAt()).isNotNull();
    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("gw-1");
  }

  @Test
  void shouldCancelInvoiceWhenPaymentFailed() {
    UUID id = service.generate(input("order-failed", PaymentMethod.GATEWAY_BALANCE, null));
    when(paymentGatewayService.capture(any(PaymentRequest.class)))
        .thenReturn(new Payment("gw-2", id, PaymentMethod.GATEWAY_BALANCE, PaymentStatus.FAILED));

    service.processPayment(id);

    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
    assertThat(invoice.getCancelReason()).isEqualTo("Payment failed");
  }

  @Test
  void shouldKeepInvoiceUnpaidWhenPaymentIsPending() {
    UUID id = service.generate(input("order-pending", PaymentMethod.GATEWAY_BALANCE, null));
    when(paymentGatewayService.capture(any(PaymentRequest.class)))
        .thenReturn(new Payment("gw-3", id, PaymentMethod.GATEWAY_BALANCE, PaymentStatus.PENDING));

    service.processPayment(id);

    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("gw-3");
  }

  @Test
  void shouldCancelInvoiceWhenGatewayCaptureThrowsException() {
    UUID id = service.generate(input("order-error", PaymentMethod.GATEWAY_BALANCE, null));
    when(paymentGatewayService.capture(any(PaymentRequest.class))).thenThrow(new RuntimeException("gateway down"));

    service.processPayment(id);

    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
    assertThat(invoice.getCancelReason()).isEqualTo("Payment capture failed");
    verify(paymentGatewayService).capture(any(PaymentRequest.class));
  }

  @Test
  void shouldThrowInvoiceNotFoundExceptionWhenProcessingUnknownInvoice() {
    assertThatThrownBy(() -> service.processPayment(UUID.randomUUID()))
        .isInstanceOf(InvoiceNotFoundException.class);
  }

  private GenerateInvoiceInput input(String orderId, PaymentMethod method, UUID creditCardId) {
    Set<LineItemInput> items = new LinkedHashSet<>();
    items.add(LineItemInput.builder().name("Notebook").amount(new BigDecimal("2500.00")).quantity(1).build());
    items.add(LineItemInput.builder().name("Mouse").amount(new BigDecimal("150.50")).quantity(2).build());

    return GenerateInvoiceInput.builder()
        .orderId(orderId)
        .customerId(UUID.randomUUID())
        .paymentSettings(PaymentSettingsInput.builder().method(method).creditCardId(creditCardId).build())
        .payer(PayerData.builder()
            .fullName("John Doe")
            .document("123.456.789-00")
            .email("john@example.com")
            .phone("11999999999")
            .address(AddressData.builder()
                .street("Rua das Flores")
                .number("100")
                .neighborhood("Centro")
                .city("Sao Paulo")
                .state("SP")
                .zipCode("01000-000")
                .build())
            .build())
        .items(items)
        .build();
  }
}
