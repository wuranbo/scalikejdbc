package scalikejdbc.jsr310

import scala.language.implicitConversions
import scalikejdbc.WrappedResultSet

object Implicits extends Implicits

trait Implicits {

  @deprecated("use WrappedResultSet", "3.0.0")
  implicit def fromWrappedResultSetToJSR310WrappedResultSet(rs: WrappedResultSet): JSR310WrappedResultSet =
    new JSR310WrappedResultSet(rs)

}
