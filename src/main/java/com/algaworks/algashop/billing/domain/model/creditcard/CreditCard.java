package com.algaworks.algashop.billing.domain.model.creditcard;

import com.algaworks.algashop.billing.domain.model.IdGenerator;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class CreditCard {

  @Id
  @EqualsAndHashCode.Include
  private UUID id;
  private OffsetDateTime createdAt;
  private UUID customerId;
  private String lastNumbers;
  private String brand;
  private Integer expMonth;
  private Integer expYear;
  private String gatewayCode;

  public static CreditCard brandNew(UUID customerId, String lastNumbers, String brand, Integer expMonth,
      Integer expYear, String gatewayCode) {
    Objects.requireNonNull(customerId);
    Objects.requireNonNull(expMonth);
    Objects.requireNonNull(expYear);
    if (StringUtils.isBlank(lastNumbers) || StringUtils.isBlank(brand) || StringUtils.isBlank(gatewayCode)) {
      throw new IllegalArgumentException();
    }
    return new CreditCard(IdGenerator.generateTimeBasedUUID(), OffsetDateTime.now(), customerId, lastNumbers, brand,
        expMonth, expYear, gatewayCode);
  }

  public void setGatewayCode(String gatewayCode) {
    if (StringUtils.isBlank(gatewayCode)) {
      throw new IllegalArgumentException();
    }
    this.gatewayCode = gatewayCode;
  }
}
