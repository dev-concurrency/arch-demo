package infrastructure
package util

case class ResultError(code: TransportError, message: String) extends infrastructure.CborSerializable
// , infrastructure.ProtoSerializable

// enum TransportError extends infrastructure.CborSerializable, infrastructure.ProtoSerializable:
enum TransportError                                           extends infrastructure.CborSerializable:
    case NotFound, BadRequest, InternalServerError, Unauthorized, Forbidden, Unknown, Maintenance

enum EffectType extends infrastructure.CborSerializable:
    case Stop, None
