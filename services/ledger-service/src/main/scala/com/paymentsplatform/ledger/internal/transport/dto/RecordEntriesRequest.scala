package com.paymentsplatform.ledger.internal.transport.dto

import java.util.UUID
import scala.beans.BeanProperty

final class RecordEntriesRequest {
  @BeanProperty var paymentId: UUID = _
  @BeanProperty var legs: java.util.List[LegDto] = new java.util.ArrayList[LegDto]()
}

final class LegDto {
  @BeanProperty var accountId: UUID = _
  @BeanProperty var entryType: String = _
  @BeanProperty var amount: java.math.BigDecimal = _
  @BeanProperty var currency: String = _
}
