package components
package examples

import distage.plugins.PluginDef

// package new_components_2a:

//   trait S1Impl2a extends Trait2{
//     def m2(): Unit = println("Echoing m2 a...")
//   }

//   object module2Plugin extends PluginDef {
//       modify[Trait2]{ _ =>
//               new S1Impl2a{}
//       }
//   }

package new_components_2b:

    trait S1Impl2b extends Trait2 {
      def m2(): Unit = println("Echoing m2 b...")
    }

    object module2Plugin2 extends PluginDef {

      modify[Trait2] {
        _ =>
          new S1Impl2b {}
      }

    }

package new_components_1a:

    trait S1Impl1a extends Trait1 {
      def m1(): Unit = println("Echoing m1 a...")
    }

    object module2Plugin1 extends PluginDef {

      modify[Trait1] {
        _ =>
          new S1Impl1a {}
      }

    }

// package new_components_1b:

//   trait S1Impl1b extends Trait1{
//     def m1(): Unit = println("Echoing m1 b...")
//   }

//   object module2Plugin1 extends PluginDef {
//       modify[Trait1]{ _ =>
//               new S1Impl1b{}
//       }
//   }
