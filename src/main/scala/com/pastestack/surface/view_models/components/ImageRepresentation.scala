// Copyright (c) 2017–present Tomshley LLC
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.pastestack.surface.view_models.components

// One resolved representation of an image: a URL plus its intrinsic
// dimensions. Upgrade rungs and progressive-resolution descriptors are
// this value — never a frame or another nested structure.
final case class ImageRepresentation(
  src: String,
  width: Int,
  height: Int
) {
  require(ImageRepresentation.usableSrc(src), "ImageRepresentation src must not be blank or carry control characters")
  require(width > 0, "ImageRepresentation width must be positive")
  require(height > 0, "ImageRepresentation height must be positive")

  private[surface] def jsonMap: java.util.Map[String, Object] = {
    // Insertion-ordered: the serialized key order is part of the
    // rendered bytes and must not vary between JVM runs.
    val map = new java.util.LinkedHashMap[String, Object]()
    map.put("src", src)
    map.put("width", Int.box(width))
    map.put("height", Int.box(height))
    map
  }
}

object ImageRepresentation {
  // The special schemes whose host-bearing URLs are folded, each with the
  // port it omits. file: is special too, but its host and drive-letter
  // rules are its own, so a file: src compares as written.
  private val DefaultPorts = Map("http" -> 80, "https" -> 443, "ws" -> 80, "wss" -> 443, "ftp" -> 21)
  private val SchemePrefix = "(?s)([A-Za-z][A-Za-z0-9+.-]*):(.*)".r

  // The base-independent identity two srcs share when the browser
  // resolves them to one URL, for the "one resource, one intrinsic size"
  // checks. It folds what the WHATWG URL parser folds against any http(s)
  // document base: surrounding spaces and controls and every tab or
  // newline are dropped; dot segments, "%2e" spellings included, are
  // removed one segment at a time as the parser's path state does, so
  // "x//../f1.webp" is "x/f1.webp", never "f1.webp", and "%2e/f1.webp" is
  // "f1.webp"; a ".." at a path's root is a no-op, so "/../f1.webp" is
  // "/f1.webp"; and an http, https, ws, wss or ftp URL compares with its
  // scheme and host lower-cased and its default port dropped. A relative
  // src resolves against the base's directory, which this check does not
  // know, so a ".." that climbs into it is kept. Everything else is left
  // as written: a backslash, a third slash before the host, another
  // scheme, a host the browser would decode or map, a path character it
  // would percent-encode. Two identities can still name one URL, but one
  // identity names one URL to the standard parser; an engine that keeps a
  // "%2e" segment as written resolves such a pair apart, and an engine
  // that follows the standard still refuses the catalog. The Element,
  // which resolves against the real document base, stays the final check.
  private[surface] def sourceIdentity(src: String): String = {
    val text = src.filterNot(c => c == '\t' || c == '\n' || c == '\r')
      .dropWhile(_ <= ' ').reverse.dropWhile(_ <= ' ').reverse
    foldedIdentity(text).getOrElse(text)
  }

  private def foldedIdentity(text: String): Option[String] =
    if (text.isEmpty || text.contains('\\') || text.startsWith("?") || text.startsWith("#")) None
    else text match {
      case SchemePrefix(scheme, rest) =>
        val name = asciiLower(scheme)
        DefaultPorts.get(name).flatMap(port => authorityIdentity(rest, Some(port))).map(name + ":" + _)
      case _ if text.startsWith("//") => authorityIdentity(text, None)
      case _ if text.startsWith("/") =>
        val (path, suffix) = splitSuffix(text.substring(1))
        val (_, kept) = resolveSegments(path)
        // The parser's own guard: a host-less path that starts with an
        // empty segment is written "/.//", never read as "//host".
        val guard = if (kept.size > 1 && kept.head.isEmpty) "/." else ""
        Some(guard + "/" + kept.mkString("/") + suffix)
      case _ =>
        val (path, suffix) = splitSuffix(text)
        val (climbs, kept) = resolveSegments(path)
        // "./" keeps a first segment that is empty or holds a colon from
        // reading as a path-absolute src, a query alone, or a scheme.
        val prefix =
          if (climbs > 0) "../" * climbs
          else if (kept.head.isEmpty || kept.head.contains(':')) "./"
          else ""
        Some(prefix + kept.mkString("/") + suffix)
    }

  // "//", a nonempty host and an optional port, then the path. A third
  // slash is left as written, since what it means depends on the base.
  private def authorityIdentity(rest: String, defaultPort: Option[Int]): Option[String] =
    if (!rest.startsWith("//") || rest.startsWith("///")) None
    else {
      val afterSlashes = rest.substring(2)
      val end = afterSlashes.indexWhere(c => c == '/' || c == '?' || c == '#')
      val (authority, remainder) = afterSlashes.splitAt(if (end < 0) afterSlashes.length else end)
      val userInfo = authority.substring(0, authority.lastIndexOf('@') + 1)
      val hostPort = authority.substring(userInfo.length)
      val hostEnd =
        if (hostPort.startsWith("[")) hostPort.indexOf(']') + 1
        else if (hostPort.contains(':')) hostPort.indexOf(':')
        else hostPort.length
      val host = asciiLower(hostPort.substring(0, hostEnd))
      val portDigits = hostPort.substring(hostEnd).stripPrefix(":")
      val validPort = hostPort.length == hostEnd ||
        (hostPort.charAt(hostEnd) == ':' && portDigits.forall(c => c >= '0' && c <= '9') &&
          portDigits.dropWhile(_ == '0').length <= 5)
      lazy val port = if (portDigits.isEmpty) None else Some(portDigits.toInt)
      if (host.isEmpty || !validPort || port.exists(_ > 65535)) None
      else {
        val (path, suffix) = splitSuffix(remainder)
        val (_, kept) = resolveSegments(path.stripPrefix("/"))
        val shownPort = port.filterNot(p => defaultPort.contains(p)).fold("")(":" + _)
        Some("//" + userInfo + host + shownPort + "/" + kept.mkString("/") + suffix)
      }
    }

  // The path ends at the first "?" or "#"; the query and fragment are
  // kept as written.
  private def splitSuffix(text: String): (String, String) = {
    val end = text.indexWhere(c => c == '?' || c == '#')
    if (end < 0) (text, "") else text.splitAt(end)
  }

  // The parser's path state over "/"-separated segments: a ".." drops the
  // segment before it, or counts a climb above the start when there is
  // none, and a dot segment that ends the path leaves an empty last
  // segment, so the result is never empty.
  private def resolveSegments(path: String): (Int, Vector[String]) = {
    val segments = path.split("/", -1).toVector
    segments.zipWithIndex.foldLeft((0, Vector.empty[String])) { case ((climbs, kept), (segment, index)) =>
      val last = index == segments.size - 1
      asciiLower(segment) match {
        case ".." | ".%2e" | "%2e." | "%2e%2e" =>
          val (nextClimbs, rest) = if (kept.isEmpty) (climbs + 1, kept) else (climbs, kept.init)
          (nextClimbs, if (last) rest :+ "" else rest)
        case "." | "%2e" => (climbs, if (last) kept :+ "" else kept)
        case _ => (climbs, kept :+ segment)
      }
    }
  }

  private def asciiLower(text: String): String =
    text.map(c => if (c >= 'A' && c <= 'Z') (c + 32).toChar else c)

  // The Element's rule for a src: a string that is non-empty after
  // JavaScript's trim, whose whitespace set is wider than Java's. A C0
  // control is refused too — the HTML attribute and the JSON block would
  // carry it differently, so one src would resolve to two URLs.
  private[surface] def usableSrc(src: String): Boolean =
    Option(src).exists(value =>
      !value.exists(_ < ' ') &&
        value.exists(c => !Character.isWhitespace(c) && !Character.isSpaceChar(c) && c != '﻿')
    )
}
