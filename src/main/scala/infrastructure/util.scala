package infrastructure
package util

import cats.data.EitherT

case class ResultError(code: TransportError, message: String) extends infrastructure.CborSerializable

enum TransportError extends infrastructure.CborSerializable:
    case NotFound, BadRequest, InternalServerError, Unauthorized, Forbidden, Unknown, Maintenance
