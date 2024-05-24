package components
package examples

extension [A](a: A)
    def |>[B](f: (A) => B): B = f(a)
    def |[B](f: (A) => B): B = a.pipe(f)

// https://izumi.7mind.io/distage/basics.html#set-bindings

import distage.ModuleDef

final case class CommandHandler
  (
    handle: PartialFunction[String, String])

val additionHandler = CommandHandler {
  case s"$x + $y" => s"${x.toInt + y.toInt}"
}

object AdditionModule extends ModuleDef {

  many[CommandHandler]
    .add(additionHandler)

}

val subtractionHandler = CommandHandler {
  case s"$x - $y" => s"${x.toInt - y.toInt}"
}

object SubtractionModule extends ModuleDef {

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

object AppModule extends ModuleDef {
  // include all the previous module definitions
  include(AdditionModule)
  include(SubtractionModule)

  // add a help handler
  many[CommandHandler].add(CommandHandler {
    case "help" => "Please input an arithmetic expression!"
  })

  // bind App
  make[App].from[App.Impl]
}

// wire the graph and get the app
def runDemo1 =
    val app = Injector().produceGet[App](AppModule).unsafeGet()
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

import distage.plugins.{ PluginConfig, PluginDef, PluginLoader }

trait Service1 {
  def m1(): Unit
  def m2(): Unit
}

trait Trait1 {
  def m1(): Unit
}

trait Trait2 {
  def m2(): Unit
}

object Service1 {

  trait Impl
    (
      t1: Trait1,
      t2: Trait2) extends Service1 {
    def m1(): Unit = t1.m1()
    def m2(): Unit = t2.m2()
  }

}

trait S1Impl1 extends Trait1 {
  def m1(): Unit = println("Echoing m1...")
}

object defaultTrait1 extends Trait1 {
  def m1(): Unit = println("m1 default...")
}

trait S1Impl2 extends Trait2 {
  def m2(): Unit = println("Echoing m2...")
}

object defaultTrait2 extends Trait2 {
  def m2(): Unit = println("m2 default...")
}

def runDemo2 =
    def module =
      new ModuleDef {
        make[Trait1].fromTrait[S1Impl1]
        make[Trait2].fromTrait[S1Impl2]
        make[Service1].fromTrait[Service1.Impl]
      }
    Injector().produceRun(module) {
      (
        t1: Trait1,
        t2: Trait2,
        s1: Service1) =>
        {
          // t1.m1()
          // t2.m2()
          s1.m1()
          s1.m2()
        }
    }

object modulePlugin extends PluginDef {
  make[Trait1].from(defaultTrait1)
  make[Trait2].from(defaultTrait2)
  make[Service1].fromTrait[Service1.Impl]
}

def runDemo3 =
    val pluginConfig = PluginConfig.cached(packagesEnabled = Seq("components.examples"))
    val appModules = PluginLoader().load(pluginConfig)
    val module = appModules.result.merge

    Injector().produceRun(module) {
      (
        t1: Trait1,
        t2: Trait2,
        s1: Service1) =>
        {
          s1.m1()
          s1.m2()
        }
    }

@main
def run = runDemo3
