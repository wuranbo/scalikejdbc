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

import scalikejdbc.interpolation.SQLSyntax

private[scalikejdbc] object LastParameter

private object SQLInterpolationString {

  private val cache = new scala.collection.concurrent.TrieMap[String, SQLSyntax]()

}

/**
 * SQLInterpolation definition
 */
class SQLInterpolationString(val s: StringContext) extends AnyVal {

  def sql[A](params: Any*) = {
    val syntax = sqls(params: _*)
    SQL[A](syntax.value).bind(syntax.parameters: _*)
  }

  def sqls(params: Any*): SQLSyntax = {
    def create = {
      val query: String = s.parts.zipAll(params, "", LastParameter).foldLeft("") {
        case (query, (previousQueryPart, param)) => query + previousQueryPart + getPlaceholders(param)
      }
      SQLSyntax(query, params.flatMap(toSeq))
    }

    if (s.parts.size == 1 && params.isEmpty) {
      SQLInterpolationString.cache.getOrElseUpdate(s.parts.head, create)
    } else {
      create
    }
  }

  private def getPlaceholders(param: Any): String = param match {
    case _: String => "?"
    case t: Traversable[_] => t.map(_ => "?").mkString(", ") // e.g. in clause
    case LastParameter => ""
    case SQLSyntax(s, _) => s
    case _ => "?"
  }

  private def toSeq(param: Any): Traversable[Any] = param match {
    case s: String => Seq(s)
    case t: Traversable[_] => t
    case SQLSyntax(_, params) => params
    case n => Seq(n)
  }

}

