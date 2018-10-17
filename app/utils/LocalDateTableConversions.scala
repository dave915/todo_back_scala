package utils

import java.sql.Timestamp
import java.time.LocalDateTime

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._

/**
  * @author dave.th
  * @date 16/10/2018
  */
object LocalDateTableConversions {
  implicit val localDateTimeToTimestamp: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    { Timestamp.valueOf } ,
    { ts => ts.toLocalDateTime }
  )
}
