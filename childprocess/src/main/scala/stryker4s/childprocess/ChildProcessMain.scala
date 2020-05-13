package stryker4s.childprocess

object ChildProcessMain {
  def main(args: Array[String]): Unit = {
    val messageHandler = new MessageHandler()
    val stdHandler = new StdHandler(messageHandler)

    stdHandler.setupHandle()
  }
}
