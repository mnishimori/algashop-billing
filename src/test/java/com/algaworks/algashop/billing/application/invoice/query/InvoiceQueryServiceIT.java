package com.algaworks.algashop.billing.application.invoice.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algaworks.algashop.billing.IntegrationTest;
import com.algaworks.algashop.billing.application.invoice.management.AddressData;
import com.algaworks.algashop.billing.application.invoice.management.GenerateInvoiceInput;
import com.algaworks.algashop.billing.application.invoice.management.InvoiceManagementApplicationService;
import com.algaworks.algashop.billing.application.invoice.management.LineItemInput;
import com.algaworks.algashop.billing.application.invoice.management.PayerData;
import com.algaworks.algashop.billing.application.invoice.management.PaymentSettingsInput;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.algaworks.algashop.billing.domain.model.invoice.PaymentMethod;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
class InvoiceQueryServiceIT {

  @Autowired
  private InvoiceQueryService queryService;

  @Autowired
  private InvoiceManagementApplicationService managementService;

  @MockitoBean
  private PaymentGatewayService paymentGatewayService;

  @Test
  void shouldFindInvoiceByOrderId() {
    GenerateInvoiceInput input = input("order-query-1");
    UUID id = managementService.generate(input);

    InvoiceOutput output = queryService.findByOrderId("order-query-1");

    assertThat(output.getId()).isEqualTo(id);
    assertThat(output.getOrderId()).isEqualTo("order-query-1");
    assertThat(output.getCustomerId()).isEqualTo(input.getCustomerId());
    assertThat(output.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(output.getTotalAmount()).isEqualByComparingTo("2650.50");
    assertThat(output.getPayer().getFullName()).isEqualTo("John Doe");
    assertThat(output.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
  }

  @Test
  void shouldThrowInvoiceNotFoundExceptionWhenOrderIdDoesNotExist() {
    assertThatThrownBy(() -> queryService.findByOrderId("unknown-order"))
        .isInstanceOf(InvoiceNotFoundException.class);
  }

  private GenerateInvoiceInput input(String orderId) {
    Set<LineItemInput> items = new LinkedHashSet<>();
    items.add(LineItemInput.builder().name("Notebook").amount(new BigDecimal("2500.00")).quantity(1).build());
    items.add(LineItemInput.builder().name("Mouse").amount(new BigDecimal("150.50")).quantity(2).build());

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
