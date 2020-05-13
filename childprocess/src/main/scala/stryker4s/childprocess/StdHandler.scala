package stryker4s.childprocess

import java.io.ObjectInputStream
import stryker4s.childprocess.api._

class StdHandler(messageHandler: MessageHandler) {
  def setupHandle(): Unit = {
    var continue = true;

    val inputStream = new ObjectInputStream(System.in)
    try {
      while (continue) {
        val obj = inputStream.readObject().asInstanceOf[Request]
        obj match {
          case PoisonPill => continue = false
          case other      => messageHandler.handle(other)
        }
      }
    } finally {
      inputStream.close()
    }
  }
}
