/*
 * Copyright 2013 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros._

import scalikejdbc.interpolation.SQLSyntax

/**
 * Macros for dynamic fields validation
 */
object SQLInterpolationMacro {

  def selectDynamic[E: c.WeakTypeTag](c: Context)(name: c.Expr[String]): c.Expr[SQLSyntax] = {
    import c.universe._

    for {
      _name <- name.tree match {
        case Literal(Constant(value: String)) => Some(value)
        case _ => None
      }
      // primary constructor args of type E
      expectedNames: Set[String] = c.weakTypeOf[E].declarations.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor =>
          m.paramss.flatMap { _.map(_.name.encoded.trim) }(collection.breakOut): Set[String]
      }.getOrElse(Set.empty)
      if !expectedNames.isEmpty && !expectedNames.contains(_name)
    } {
      c.error(c.enclosingPosition, s"${c.weakTypeOf[E]}#${_name} not found. Expected fields are ${expectedNames.mkString("#", ", #", "")}.")
    }

    c.Expr[SQLSyntax](Apply(Select(c.prefix.tree, newTermName("field")), List(name.tree)))
  }

}

