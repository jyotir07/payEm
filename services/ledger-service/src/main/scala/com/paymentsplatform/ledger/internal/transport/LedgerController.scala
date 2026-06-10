package com.paymentsplatform.ledger.internal.transport

import com.paymentsplatform.ledger.internal.application._
import com.paymentsplatform.ledger.internal.domain.EntryType
import com.paymentsplatform.ledger.internal.transport.dto._
import io.javalin.Javalin
import io.javalin.http.Context

import java.util.UUID
import scala.jdk.CollectionConverters._

final class LedgerController(
    openAccount: OpenAccountUseCase,
    getBalance: GetBalanceUseCase,
    recordTransaction: RecordTransactionUseCase,
    getEntriesForPayment: GetEntriesForPaymentUseCase
) {

  def register(app: Javalin): Unit = {
    app.post("/accounts", ctx => createAccount(ctx))
    app.get("/accounts/{id}/balance", ctx => fetchBalance(ctx))
    app.post("/entries", ctx => recordEntries(ctx))
    app.get("/entries/{paymentId}", ctx => fetchEntriesByPayment(ctx))
  }

  private def createAccount(ctx: Context): Unit = {
    val body = ctx.bodyAsClass(classOf[CreateAccountRequest])
    if (body == null || body.name == null || body.name.trim.isEmpty)
      throw new IllegalArgumentException("name is required")
    if (body.currency == null || body.currency.trim.isEmpty)
      throw new IllegalArgumentException("currency is required")

    val account = openAccount.execute(body.name.trim, body.currency.trim)
    ctx.status(201).json(AccountResponse.from(account))
  }

  private def fetchBalance(ctx: Context): Unit = {
    val accountId = parseUuid(ctx.pathParam("id"), "id")
    val account = getBalance.loadAccount(accountId)
    ctx.status(200).json(BalanceResponse.from(account, account.balance))
  }

  private def recordEntries(ctx: Context): Unit = {
    val body = ctx.bodyAsClass(classOf[RecordEntriesRequest])
    if (body == null || body.paymentId == null)
      throw new IllegalArgumentException("paymentId is required")
    if (body.legs == null || body.legs.isEmpty)
      throw new IllegalArgumentException("legs must not be empty")

    val legs = body.legs.asScala.toList.map { dto =>
      if (dto.accountId == null) throw new IllegalArgumentException("leg.accountId is required")
      if (dto.entryType == null) throw new IllegalArgumentException("leg.entryType is required")
      if (dto.amount == null) throw new IllegalArgumentException("leg.amount is required")
      if (dto.currency == null) throw new IllegalArgumentException("leg.currency is required")
      val entryType =
        try EntryType.fromName(dto.entryType)
        catch { case _: IllegalArgumentException =>
          throw new InvalidEntryTypeException(dto.entryType)
        }
      RecordTransactionUseCase.Leg(
        accountId = dto.accountId,
        entryType = entryType,
        amount = BigDecimal(dto.amount),
        currency = dto.currency
      )
    }

    val result = recordTransaction.execute(RecordTransactionUseCase.Request(body.paymentId, legs))
    val status = if (result.isNew) 201 else 200
    ctx.status(status).json(TransactionResponse.from(body.paymentId, result.entries))
  }

  private def fetchEntriesByPayment(ctx: Context): Unit = {
    val paymentId = parseUuid(ctx.pathParam("paymentId"), "paymentId")
    val entries = getEntriesForPayment.execute(paymentId)
    ctx.status(200).json(TransactionResponse.from(paymentId, entries))
  }

  private def parseUuid(raw: String, paramName: String): UUID =
    try UUID.fromString(raw)
    catch { case _: IllegalArgumentException =>
      throw new InvalidPathParameterException(paramName, raw)
    }
}

final class InvalidPathParameterException(val paramName: String, val raw: String)
    extends RuntimeException(s"Invalid UUID for $paramName: $raw")

final class InvalidEntryTypeException(val raw: String)
    extends RuntimeException(s"Invalid entry type: $raw")
