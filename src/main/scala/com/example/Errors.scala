package com.example

import cats.data.EitherT

// format: off
trait ServiceError extends Throwable

trait RequestError  extends ServiceError
trait DBError       extends ServiceError
trait ServerError   extends ServiceError
trait SecurityError extends ServiceError

case class ServiceUnavailable(code: String, title: String, message: String) extends Throwable(message), ServerError
case class Conflict(code: String, title: String, message: String)           extends Throwable(message), DBError
case class BadRequest(code: String, title: String, message: String)         extends Throwable(message), RequestError
case class NotFound(code: String, title: String, message: String)           extends Throwable(message), DBError
case class InternalServer(code: String, title: String, message: String)     extends Throwable(message), ServerError
case class Unauthorized(code: String, title: String, message: String)       extends Throwable(message), SecurityError
case class Forbidden(code: String, title: String, message: String)          extends Throwable(message), SecurityError

import cats.effect.*

type Result[A] = EitherT[IO, ServiceError, A]

// format: on

object ErrorsBuilder:

    def serviceUnavailableError(message: String): ServiceUnavailable = ServiceUnavailable("ECOD-503", "SERVICE-UNAVAILABLE", message)

    def conflictError(message: String): Conflict = Conflict("ECOD-409", "CONFLICT", message)

    def badRequestError(message: String): BadRequest = BadRequest("ECOD-400", "BAD-REQUEST", message)

    def notFoundError(message: String): NotFound = NotFound("ECOD-404", "NOT-FOUND", message)

    def internalServerError(message: String): InternalServer = InternalServer("ECOD-500", "INTERNAL-ERROR", message)

    def unauthorizedError(message: String): Unauthorized = Unauthorized("ECOD-401", "UNAUTHORIZED", message)

    def forbiddenError(message: String): Forbidden = Forbidden("ECOD-403", "FORBIDDEN", message)
