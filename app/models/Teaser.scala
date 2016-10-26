package models

import play.api.libs.json.Json

object Teaser {
  implicit val format = Json.format[Teaser]
}

case class Teaser(title: String, text: String, image: Image)
