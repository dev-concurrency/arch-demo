package components
package examples
package plugins

import distage.plugins.PluginConfig
import distage.plugins.PluginDef
import distage.plugins.PluginLoader

final case class CommandHandler
  (
    handle: PartialFunction[String, String])

object AdditionModule extends PluginDef {

  val additionHandler = CommandHandler {
    case s"$x + $y" => s"${x.toInt + y.toInt}"
  }

  many[CommandHandler]
    .add(additionHandler)

}

object SubtractionModule extends PluginDef {

  val subtractionHandler = CommandHandler {
    case s"$x - $y" => s"${x.toInt - y.toInt}"
  }

  many[CommandHandler]
    .add(subtractionHandler)

}

import distage.Injector

trait App {
  def interpret(input: String): String
}

object App {

  final class Impl
    (
      handlers: Set[CommandHandler]) extends App {

    override def interpret(input: String): String = {
      handlers.map(_.handle).reduce(_ orElse _).lift(input) match {
        case Some(answer) => s"ANSWER: $answer"
        case None         => "?"
      }
    }

  }

}

object AppModule extends PluginDef {

  many[CommandHandler].add(CommandHandler {
    case "help" => "Please input an arithmetic expression!"
  })

  make[App].from[App.Impl]
}

def runDemo1 =
    val pluginConfig = PluginConfig.cached(packagesEnabled = Seq("components.examples.plugins"))
    val appModules = PluginLoader().load(pluginConfig)
    val module = appModules.result.merge

    val app = Injector().produceGet[App](module).unsafeGet()

    app.interpret("1 + 5")
    |> println
    // res27: String = "ANSWER: 6"
    app.interpret("7 - 11")
    |> println
    // res28: String = "ANSWER: -4"
    app.interpret("1 / 3")
    |> println
    // res29: String = "?"
    app.interpret("help")
    |> println
    // res30: String = "ANSWER: Please input an arithmetic expression!"

@main
def run = runDemo1
