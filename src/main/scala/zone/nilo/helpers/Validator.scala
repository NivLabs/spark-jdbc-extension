package zone.nilo.helpers

import java.util.UUID

object Validator {
  def isValidUUID(s: String): Boolean = {
    try {
      UUID.fromString(s)
      true
    } catch {
      case _: IllegalArgumentException => false
    }
  }
}
