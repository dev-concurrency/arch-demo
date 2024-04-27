package infrastructure
package util


case class ResultError(code: TransportError, message: String) extends infrastructure.CborSerializable

enum TransportError extends infrastructure.CborSerializable:
    case NotFound, BadRequest, InternalServerError, Unauthorized, Forbidden, Unknown, Maintenance
