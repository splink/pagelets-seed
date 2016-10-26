package models

import play.api.libs.json.Json

object Size {
  implicit val format = Json.format[Size]
}

case class Size(width: Int, height: Int)

object Image {
  implicit val format = Json.format[Image]
}

case class Image(src: String, size: Size, alt: String, title: Option[String] = None)
