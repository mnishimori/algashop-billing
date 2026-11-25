package com.algaworks.algashop.billing.application.invoice.management;

import com.algaworks.algashop.billing.domain.model.DomainException;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.algaworks.algashop.billing.domain.model.invoice.Invoice;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.algaworks.algashop.billing.domain.model.invoice.InvoicingService;
import com.algaworks.algashop.billing.domain.model.invoice.LineItem;
import com.algaworks.algashop.billing.domain.model.invoice.Payer;
import com.algaworks.algashop.billing.domain.model.invoice.PaymentMethod;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceManagementApplicationServiceTest {

  @Mock
  private PaymentGatewayService paymentGatewayService;

  @Mock
  private InvoicingService invoicingService;

  @Mock
  private InvoiceRepository invoiceRepository;

  @Mock
  private CreditCardRepository creditCardRepository;

  @InjectMocks
  private InvoiceManagementApplicationService service;

  @Test
  @SuppressWarnings("unchecked")
  void shouldGenerateInvoiceWithCreditCardAndReturnId() {
    UUID creditCardId = UUID.randomUUID();
    GenerateInvoiceInput input = input(PaymentMethod.CREDIT_CARD, creditCardId);
    when(creditCardRepository.existsById(creditCardId)).thenReturn(true);
    when(invoicingService.issue(eq("order-1"), eq(input.getCustomerId()), any(Payer.class), any(Set.class)))
        .thenReturn(issuedInvoice(input.getCustomerId()));

    UUID id = service.generate(input);

    ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceRepository).saveAndFlush(invoiceCaptor.capture());
    Invoice saved = invoiceCaptor.getValue();
    assertThat(id).isEqualTo(saved.getId());
    assertThat(saved.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(saved.getPaymentSettings().getCreditCardId()).isEqualTo(creditCardId);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGenerateInvoiceWithGatewayBalanceWithoutCheckingCreditCard() {
    GenerateInvoiceInput input = input(PaymentMethod.GATEWAY_BALANCE, null);
    when(invoicingService.issue(eq("order-1"), eq(input.getCustomerId()), any(Payer.class), any(Set.class)))
        .thenReturn(issuedInvoice(input.getCustomerId()));

    UUID id = service.generate(input);

    verifyNoInteractions(creditCardRepository);
    ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceRepository).saveAndFlush(invoiceCaptor.capture());
    assertThat(id).isEqualTo(invoiceCaptor.getValue().getId());
    assertThat(invoiceCaptor.getValue().getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldConvertPayerAndItemsWhenIssuing() {
    GenerateInvoiceInput input = input(PaymentMethod.GATEWAY_BALANCE, null);
    when(invoicingService.issue(eq("order-1"), eq(input.getCustomerId()), any(Payer.class), any(Set.class)))
        .thenReturn(issuedInvoice(input.getCustomerId()));

    service.generate(input);

    ArgumentCaptor<Payer> payerCaptor = ArgumentCaptor.forClass(Payer.class);
    ArgumentCaptor<Set<LineItem>> itemsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(invoicingService).issue(eq("order-1"), eq(input.getCustomerId()), payerCaptor.capture(),
        itemsCaptor.capture());

    Payer payer = payerCaptor.getValue();
    assertThat(payer.getFullName()).isEqualTo("John Doe");
    assertThat(payer.getEmail()).isEqualTo("john@example.com");
    assertThat(payer.getDocument()).isEqualTo("123.456.789-00");
    assertThat(payer.getPhone()).isEqualTo("11999999999");
    assertThat(payer.getAddress().getStreet()).isEqualTo("Rua das Flores");
    assertThat(payer.getAddress().getCity()).isEqualTo("Sao Paulo");
    assertThat(payer.getAddress().getZipCode()).isEqualTo("01000-000");

    List<LineItem> items = List.copyOf(itemsCaptor.getValue());
    assertThat(items).hasSize(2);
    assertThat(items).extracting(LineItem::getNumber).containsExactly(1, 2);
    assertThat(items).extracting(LineItem::getName).containsExactly("Notebook", "Mouse");
    assertThat(items).extracting(LineItem::getAmount)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(new BigDecimal("2500.00"), new BigDecimal("150.50"));
  }

  @Test
  void shouldThrowCreditCardNotFoundExceptionWhenCreditCardDoesNotExist() {
    UUID creditCardId = UUID.randomUUID();
    GenerateInvoiceInput input = input(PaymentMethod.CREDIT_CARD, creditCardId);
    when(creditCardRepository.existsById(creditCardId)).thenReturn(false);

    assertThatThrownBy(() -> service.generate(input)).isInstanceOf(CreditCardNotFoundException.class);

    verifyNoInteractions(invoicingService);
    verify(invoiceRepository, never()).saveAndFlush(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldPropagateDomainExceptionWhenInvoiceAlreadyExistsForOrder() {
    GenerateInvoiceInput input = input(PaymentMethod.GATEWAY_BALANCE, null);
    when(invoicingService.issue(eq("order-1"), eq(input.getCustomerId()), any(Payer.class), any(Set.class)))
        .thenThrow(new DomainException("Invoice already exists for order order-1"));

    assertThatThrownBy(() -> service.generate(input))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("already exists");

    verify(invoiceRepository, never()).saveAndFlush(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldThrowNullPointerExceptionWhenCreditCardMethodHasNoCreditCardId() {
    GenerateInvoiceInput input = input(PaymentMethod.CREDIT_CARD, null);
    when(invoicingService.issue(eq("order-1"), eq(input.getCustomerId()), any(Payer.class), any(Set.class)))
        .thenReturn(issuedInvoice(input.getCustomerId()));

    assertThatThrownBy(() -> service.generate(input)).isInstanceOf(NullPointerException.class);

    verify(invoiceRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldThrowNullPointerExceptionWhenPaymentSettingsIsNull() {
    GenerateInvoiceInput input = input(PaymentMethod.GATEWAY_BALANCE, null);
    input.setPaymentSettings(null);

    assertThatThrownBy(() -> service.generate(input)).isInstanceOf(NullPointerException.class);

    verifyNoInteractions(invoicingService, invoiceRepository);
  }

  @Test
  void shouldThrowNullPointerExceptionWhenPayerIsNull() {
    GenerateInvoiceInput input = input(PaymentMethod.GATEWAY_BALANCE, null);
    input.setPayer(null);

    assertThatThrownBy(() -> service.generate(input)).isInstanceOf(NullPointerException.class);

    verifyNoInteractions(invoicingService, invoiceRepository);
  }

  private Invoice issuedInvoice(UUID customerId) {
    return Invoice.issue("order-1", customerId, payer(), Set.of(
        LineItem.builder().number(1).name("Notebook").amount(new BigDecimal("100.00")).build()));
  }

  private Payer payer() {
    return Payer.builder()
        .fullName("John Doe")
        .document("123.456.789-00")
        .phone("11999999999")
        .email("john@example.com")
        .address(com.algaworks.algashop.billing.domain.model.invoice.Address.builder()
            .street("Rua das Flores")
            .number("100")
            .neighborhood("Centro")
            .city("Sao Paulo")
            .state("SP")
            .zipCode("01000-000")
            .build())
        .build();
  }

  private GenerateInvoiceInput input(PaymentMethod method, UUID creditCardId) {
    Set<LineItemInput> items = new LinkedHashSet<>();
    items.add(LineItemInput.builder().name("Notebook").amount(new BigDecimal("2500.00")).quantity(1).build());
    items.add(LineItemInput.builder().name("Mouse").amount(new BigDecimal("150.50")).quantity(2).build());

    return GenerateInvoiceInput.builder()
        .orderId("order-1")
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
