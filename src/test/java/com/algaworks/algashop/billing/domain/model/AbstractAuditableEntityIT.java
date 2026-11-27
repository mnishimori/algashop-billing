package com.algaworks.algashop.billing.domain.model;

import com.algaworks.algashop.billing.IntegrationTest;
import com.algaworks.algashop.billing.application.invoice.management.AddressData;
import com.algaworks.algashop.billing.application.invoice.management.GenerateInvoiceInput;
import com.algaworks.algashop.billing.application.invoice.management.InvoiceManagementApplicationService;
import com.algaworks.algashop.billing.application.invoice.management.LineItemInput;
import com.algaworks.algashop.billing.application.invoice.management.PayerData;
import com.algaworks.algashop.billing.application.invoice.management.PaymentSettingsInput;
import com.algaworks.algashop.billing.domain.model.invoice.Invoice;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.algaworks.algashop.billing.domain.model.invoice.PaymentMethod;
import com.algaworks.algashop.billing.domain.model.invoice.payment.Payment;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@IntegrationTest
class AbstractAuditableEntityIT {

  @Autowired
  private InvoiceManagementApplicationService service;

  @Autowired
  private InvoiceRepository invoiceRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @MockitoBean
  private PaymentGatewayService paymentGatewayService;

  @Test
  void shouldFillAuditAttributesWhenEntityIsCreated() {
    OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

    UUID id = service.generate(input("order-audit-create"));
    invoiceRepository.flush();

    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    assertThat(invoice.getCreatedByUserId()).isNotNull();
    assertThat(invoice.getLastModifiedByUserId()).isNotNull();
    assertThat(invoice.getCreatedAt()).isNotNull().isAfterOrEqualTo(before);
    assertThat(invoice.getLastModifiedDate()).isNotNull().isAfterOrEqualTo(before);
    assertThat(invoice.getVersion()).isNotNegative();
  }

  @Test
  void shouldUpdateAuditAttributesWhenEntityIsModified() {
    UUID id = service.generate(input("order-audit-update"));
    invoiceRepository.flush();
    entityManager.clear();
    Invoice created = invoiceRepository.findById(id).orElseThrow();
    entityManager.clear();
    OffsetDateTime createdAt = created.getCreatedAt();
    OffsetDateTime lastModifiedDate = created.getLastModifiedDate();
    UUID createdBy = created.getCreatedByUserId();
    UUID lastModifiedBy = created.getLastModifiedByUserId();
    long version = created.getVersion();

    when(paymentGatewayService.capture(any(PaymentRequest.class)))
        .thenReturn(new Payment("gw-audit", id, PaymentMethod.GATEWAY_BALANCE, PaymentStatus.PAID));
    service.processPayment(id);
    invoiceRepository.flush();
    entityManager.clear();

    Invoice updated = invoiceRepository.findById(id).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(com.algaworks.algashop.billing.domain.model.invoice.InvoiceStatus.PAID);
    assertThat(updated.getVersion()).isGreaterThan(version);
    assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    assertThat(updated.getCreatedByUserId()).isEqualTo(createdBy);
    assertThat(updated.getLastModifiedDate()).isAfterOrEqualTo(lastModifiedDate);
    assertThat(updated.getLastModifiedByUserId()).isNotNull().isNotEqualTo(lastModifiedBy);
  }

  private GenerateInvoiceInput input(String orderId) {
    Set<LineItemInput> items = new LinkedHashSet<>();
    items.add(LineItemInput.builder().name("Notebook").amount(new BigDecimal("2500.00")).quantity(1).build());

    return GenerateInvoiceInput.builder()
        .orderId(orderId)
        .customerId(UUID.randomUUID())
        .paymentSettings(PaymentSettingsInput.builder().method(PaymentMethod.GATEWAY_BALANCE).build())
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
