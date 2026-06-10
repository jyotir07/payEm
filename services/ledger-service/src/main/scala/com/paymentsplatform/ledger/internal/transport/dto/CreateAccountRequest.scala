package com.paymentsplatform.ledger.internal.transport.dto

import scala.beans.BeanProperty

// Jackson populates these via setters, so we expose mutable fields with @BeanProperty.
final class CreateAccountRequest {
  @BeanProperty var name: String = _
  @BeanProperty var currency: String = _
}
