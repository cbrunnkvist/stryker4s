package stryker4s.childprocess.api

sealed trait Message extends Serializable

trait Request extends Message

trait Response extends Message {
  def message: String
}

/**
  * Stop the process
  */
case object PoisonPill extends Request
