package components
package main

import distage.Injector
import distage.plugins.PluginConfig
import distage.plugins.PluginLoader

import infrastructure.wallet.WalletContainer as obj

extension [A](a: A)
    def |>[B](f: (A) => B): B = f(a)
    def |[B](f: (A) => B): B = a.pipe(f)

def runDemo =
    val pluginConfig = PluginConfig.cached(packagesEnabled = Seq("components.entities"))
    val appModules = PluginLoader().load(pluginConfig)
    val module = appModules.result.merge

    val app = Injector().produceGet[obj.EntityConfig](module).unsafeGet()
    app.typeKey.name |> println

@main
def run = runDemo
