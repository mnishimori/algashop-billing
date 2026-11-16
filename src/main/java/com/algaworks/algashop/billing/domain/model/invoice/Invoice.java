package com.algaworks.algashop.billing.domain.model.invoice;

import com.algaworks.algashop.billing.domain.model.IdGenerator;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

  @EqualsAndHashCode.Include
  private UUID id;
  private String orderId;
  private UUID customerId;
  private OffsetDateTime issuedAt;
  private OffsetDateTime paidAt;
  private OffsetDateTime canceledAt;
  private OffsetDateTime expiresAt;
  private BigDecimal totalAmount;
  private InvoiceStatus status;
  private PaymentSettings paymentSettings;
  private Set<LineItem> items = new HashSet<>();
  private Payer payer;
  private String cancelReason;

  public static Invoice issue(String orderId, UUID customerId, Payer payer, Set<LineItem> items) {
    var totalAmount = items.stream().map(LineItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new Invoice(IdGenerator.generateTimeBasedUUID(), orderId, customerId, OffsetDateTime.now(), null, null,
        OffsetDateTime.now().plusDays(3), totalAmount, InvoiceStatus.UNPAID, null, items, payer, null);
  }

  public Set<LineItem> getItems() {
    return Collections.unmodifiableSet(items);
  }

  public void markAsPaid() {
    this.paidAt = OffsetDateTime.now();
    this.status = InvoiceStatus.PAID;
  }

  public void cancel() {
    this.canceledAt = OffsetDateTime.now();
    this.status = InvoiceStatus.CANCELED;
  }

  public void assignPaymentGatewwayCode(String code) {

  }

  public void changePaymentSettings(PaymentMethod method, UUID creditCard) {
    var paymentSettings = PaymentSettings.brandNew(method, creditCard);
    this.setPaymentSettings(paymentSettings);
  }
}
