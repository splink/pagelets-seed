package service

trait PlayPort {
  lazy val port = Option(System.getProperty("http.port")).map(Integer.parseInt).getOrElse(9000)
}
