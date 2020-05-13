package stryker4s.childprocess

import stryker4s.childprocess.api.{Request, Response}

sealed trait Handler[A, B] {
  def handle(in: A): B
}

class MessageHandler extends Handler[Request, Response] {
  def handle(message: Request): Response = ???
}
