/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend.internal

import io.circe.*
import io.circe.syntax.*

import CJson.Output

import parsley.debugger.{DebugTree, ParseAttempt}

// Small type class for quickly converting objects into JSON.
// Specialised for a Parsley debugger frontend.
private[frontend] trait CJson[-V] extends (V => Output) {
  def apply(value: V): Output
}

private[frontend] object CJson {
  type Output = Json

  // Quick extension for converting to JSON.
  implicit class CJsonOps[-V: CJson](x: V) {
    def toJSON: Output = implicitly[CJson[V]].apply(x)
  }

  // Instances for CJson
  implicit val stringToJSON: CJson[String] = (s: String) => s.asJson
  implicit val intToJSON: CJson[Int]       = (i: Int) => i.asJson

  implicit def optionToJSON[V: CJson]: CJson[Option[V]] = {
    case Some(v) => v.toJSON
    case None    => "[N/A]".toJSON
  }

  implicit val posToJSON: CJson[(Int, Int)] = { case (l, c) =>
    JsonObject(
      "line"   -> l.toJSON,
      "column" -> c.toJSON
    ).asJson
  }

  // This is assuming Map.map keeps the linked property of a given map.
  implicit def mapToJson[V: CJson]: CJson[Map[String, V]] = (map: Map[String, V]) =>
    JsonObject.fromMap(map.map { case (k, v) => (k, v.toJSON) }).asJson

  implicit val paToJSON: CJson[ParseAttempt] = { case ParseAttempt(ri, fo, to, fp, tp, sc, res) =>
    JsonObject(
      "input"    -> (if (fo == to) Json.Null else ri.toJSON),
      "position" -> JsonObject(
        "from" -> fp.toJSON,
        "to"   -> tp.toJSON
      ).asJson,
      "success"  -> (if (sc) "Yes" else "No").toJSON,
      "result"   -> (res match {
        case Some(r) => r.toString.toJSON
        case None    => Json.Null
      })
    ).asJson
  }

  // DebugTree.fullInput comes later.
  implicit lazy val dtToJSON: CJson[DebugTree] = (dt: DebugTree) =>
    JsonObject(
      "name"     -> (
        if (dt.parserName != dt.internalName)
          s"${dt.parserName} (${dt.internalName}${if (dt.childNumber.isDefined) s" (${dt.childNumber.get})" else ""})"
        else
          s"${dt.internalName}${if (dt.childNumber.isDefined) s" (${dt.childNumber.get})" else ""}"
      ).toJSON,
      "attempt"  -> dt.parseResults.toJSON,
      "children" -> Json.arr(dt.nodeChildren.map { case (_, t) => dtToJSON(t) }.toVector*)
    ).asJson
}
