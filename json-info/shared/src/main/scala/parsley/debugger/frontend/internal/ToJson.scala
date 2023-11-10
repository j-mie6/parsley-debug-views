package parsley.debugger.frontend.internal

import parsley.debugger.{DebugTree, ParseAttempt}

// Small type class for quickly converting objects into JSON.
private[frontend] trait ToJson[-T] extends (T => ujson.Value) {
  def apply(obj: T): ujson.Value
}

private[frontend] object ToJson {
  // Used instances for ToJson.
  object Implicits {
    implicit val stringToJSON: ToJson[String] = ujson.Str(_)

    implicit def optionToJSON[J: ToJson]: ToJson[Option[J]] = {
      case Some(x) => x.toJson
      case None    => ujson.Null
    }

    implicit val intToJSON: ToJson[Int] = (x: Int) => ujson.Num(x.toDouble)

    implicit val posToJSON: ToJson[(Int, Int)] = { case (l, c) =>
      ujson.Obj(
        "line"   -> l.toJson,
        "column" -> c.toJson
      )
    }

    implicit def mapToJson[V: ToJson]: ToJson[Map[String, V]] = (map: Map[String, V]) =>
      ujson.Arr.from(map)(_._2.toJson)

    implicit val paToJSON: ToJson[ParseAttempt] = {
      case ParseAttempt(rawInput, fromOffset, toOffset, fromPos, toPos, success, result) =>
        ujson.Obj(
          "input"    -> (if (fromOffset == toOffset) ujson.Null else rawInput.slice(fromOffset, toOffset + 1).toJson),
          "position" -> ujson.Obj(
            "from" -> fromPos.toJson,
            "to"   -> toPos.toJson
          ),
          "success"  -> (if (success) "Yes" else "No").toJson,
          "result"   -> (result match {
            case Some(r) => r.toString.toJson
            case None    => ujson.Null
          })
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
