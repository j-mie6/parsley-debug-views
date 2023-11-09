package parsley.debugger.frontend

import parsley.debugger.{DebugTree, ParseAttempt}

// Small type class for quickly converting objects into JSON.
trait ToJson[-T] extends (T => ujson.Value) {
  def apply(obj: T): ujson.Value
}

object ToJson {
  // Used instances for ToJson.
  object Implicits {
    implicit val stringToJSON: ToJson[String] = ujson.Str(_)

    implicit def optionToJSON[J: ToJson]: ToJson[Option[J]] = {
      case Some(x) => x.toJson
      case None    => ujson.Str("N/A")
    }

    implicit val intToJSON: ToJson[Int] = ujson.Num(_)

    implicit val posToJSON: ToJson[(Int, Int)] = { case (l, c) =>
      ujson.Obj(
        "line"   -> l.toJson,
        "column" -> c.toJson
      )
    }

    implicit def mapToJson[V: ToJson]: ToJson[Map[String, V]] = (map: Map[String, V]) =>
      ujson.Obj.from(map.iterator.map { case (name, v) => (name, v.toJson) })

    implicit val paToJSON: ToJson[ParseAttempt] = {
      case ParseAttempt(rawInput, fromOffset, toOffset, fromPos, toPos, success, result) =>
        ujson.Obj(
          "input"    -> (if (fromOffset == toOffset) ujson.Null else rawInput.slice(fromOffset, toOffset).toJson),
          "position" -> ujson.Obj(
            "from" -> fromPos.toJson,
            "to"   -> toPos.toJson
          ),
          "success"  -> (if (success) "Yes" else "No").toJson,
          "result"   -> result.toString.toJson
        )
    }

    // DebugTree.fullInput comes later.
    implicit lazy val dtToJSON: ToJson[DebugTree] = (dt: DebugTree) =>
      ujson.Obj(
        "name"     -> (if (dt.parserName != dt.internalName) s"${dt.parserName} (${dt.internalName})"
                   else dt.internalName).toJson,
        "result"   -> dt.parseResults.toJson,
        "children" -> implicitly[ToJson[Map[String, DebugTree]]](mapToJson(dtToJSON)).apply(dt.nodeChildren)
      )

    implicit class ToJsonOps[J: ToJson](x: J) {
      def toJson: ujson.Value = implicitly[ToJson[J]].apply(x)
    }
  }
}
