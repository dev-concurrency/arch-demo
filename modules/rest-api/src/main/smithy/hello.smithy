$version: "2"

namespace hello

use utils#authToken
use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [Hello, HealthCheck],
}

@http(method: "GET", uri: "/hello/{name}", code: 200)
@authToken(roles: ["admin"])
operation Hello {
  input: Person,
  output: Greeting
  errors: [
      BadRequestError
      UnauthorizedError
      ForbiddenError
      ConflictError
      InternalServerError
  ]
}

@http(method: "GET", uri: "/health", code: 200)
operation HealthCheck {
  input: Unit,
  output: Unit
}

structure Person {
  @httpLabel
  @required
  name: String,

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}

@mixin
structure ErrorMixin {
  @required
  code: String
  @required
  title: String
  @required
  description: String
}

@httpError(400)
@error("client")
structure BadRequestError with [ErrorMixin]{}

@httpError(401)
@error("client")
structure UnauthorizedError with [ErrorMixin]{}

@httpError(403)
@error("client")
structure ForbiddenError with [ErrorMixin]{}

@httpError(404)
@error("client")
structure NotFoundError with [ErrorMixin]{}

@httpError(406)
@error("client")
structure NotAcceptableError with [ErrorMixin]{}

@httpError(408)
@error("client")
structure RequestTimeoutError with [ErrorMixin]{}

@httpError(409)
@error("client")
structure ConflictError with [ErrorMixin]{}

@httpError(413)
@error("client")
structure ContentTooLargeError with [ErrorMixin]{}

@httpError(416)
@error("client")
structure RangeNotSatisfiableError with [ErrorMixin]{}

@httpError(422)
@error("client")
structure UnprocessableContentError with [ErrorMixin]{}

@httpError(423)
@error("client")
structure LockedError with [ErrorMixin]{}

@httpError(451)
@error("client")
structure UnavailableForLegalReasonsError with [ErrorMixin]{}

@httpError(500)
@error("server")
structure InternalServerError with [ErrorMixin]{}

@httpError(501)
@error("server")
structure NotImplementedError with [ErrorMixin]{}

@httpError(503)
@error("server")
structure ServiceUnavailableError with [ErrorMixin]{}

@httpError(507)
@error("server")
structure InsufficientStorageError with [ErrorMixin]{}


@simpleRestJson
service WalletRestService {
  version: "1.0.0"
  operations: [
      WalletCreate
      WalletDelete
      AddCredit
      AddDebit
      GetBalance
  ]
}

@http(
    method: "POST"
    uri: "/wallet"
    code: 201
)
// rpc createWallet(RequestId) returns (Response) {}
operation WalletCreate{
    input: RequestId
    output: Response
    errors: [
        BadRequestError
        UnauthorizedError
        ForbiddenError
        ConflictError
        InternalServerError
    ]
}

structure WalletDeleteInput {
    @httpLabel
    @required
    id: String
}
@http(
    method: "DELETE"
    uri: "/wallet/{id}"
    code: 201
)
// rpc deleteWallet(RequestId) returns (Response) {}
operation WalletDelete{
    input: WalletDeleteInput
    output: Response
    errors: [
        BadRequestError
        UnauthorizedError
        ForbiddenError
        ConflictError
        InternalServerError
    ]
}

structure AddCreditInput {
    @httpLabel
    @required
    id: String

    @required
    amount: Long
}
@http(
    method: "POST"
    uri: "/wallet/{id}/credit"
    code: 200
)
// rpc addCredit (CreditRequest) returns (Response) {}
operation AddCredit{
    input: AddCreditInput
    output: Response
    errors: [
        BadRequestError
        UnauthorizedError
        ForbiddenError
        ConflictError
        InternalServerError
    ]
}
structure AddDebitInput {
    @httpLabel
    @required
    id: String

    @required
    amount: Long
}
@http(
    method: "POST"
    uri: "/wallet/{id}/debit"
    code: 200
)
// rpc addDebit(DebitRequest) returns (Response) {}
operation AddDebit{
    input: AddDebitInput
    output: Response
    errors: [
        BadRequestError
        UnauthorizedError
        ForbiddenError
        ConflictError
        InternalServerError
    ]
}

structure GetBalanceInput {
    @httpLabel
    @required
    id: String
}
@http(method: "GET", uri: "/wallet/{id}/balance", code: 200)
// rpc getBalance(RequestId) returns (BalanceResponse) {}
operation GetBalance {
    input: GetBalanceInput
    output: BalanceResponse
    errors: [
        BadRequestError
        UnauthorizedError
        ForbiddenError
        ConflictError
        InternalServerError
    ]
}
structure CreditRequest{
    id: String
    amount: Long
}

structure DebitRequest{
    id: String
    amount: Long
}

structure Request{
}

structure RequestId{
    id: String
}

structure Response{
   message: String
}

structure BalanceResponse{
  balance : Long
}
