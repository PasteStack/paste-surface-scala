package com.pastestack.surface.rendering

import com.pastestack.surface.view_models.components.IoReplyFragmentTarget

object IoReplyFragmentEnvelope {

  def success(
    ioReplyFragments: Seq[(String, IoReplyFragmentMarkup)],
    variables: Seq[(String, String)],
    time: Double
  ): String = render(200, ioReplyFragments, variables, time)

  /** Render the same fragment outcome at its actual HTTP status, including errors. */
  def render(
    status: Int,
    ioReplyFragments: Seq[(String, IoReplyFragmentMarkup)],
    variables: Seq[(String, String)],
    time: Double,
    title: Option[String] = None
  ): String = {
    require(status >= 200 && status <= 599, "fragment response status must be 200 to 599")
    require(status != 204 && status != 205 && status != 304, "fragment response status must permit a body")
    require(title != null && title.forall(_ != null), "fragment title must not be null")
    require(time.isFinite, "envelope time must be finite")
    val fragmentNames = ioReplyFragments.map(_._1)
    ioReplyFragments.foreach { case (name, ioReplyFragment) =>
      require(
        IoReplyFragmentTarget.isValidName(name),
        s"IO reply fragment name must be a valid target name, got: $name"
      )
      require(ioReplyFragment != null, s"IO reply fragment markup for $name must be set")
    }
    require(
      fragmentNames.distinct.size == fragmentNames.size,
      s"IO reply fragment names must be distinct, duplicate: ${fragmentNames.diff(fragmentNames.distinct).mkString(",")}"
    )
    val variableNames = variables.map(_._1)
    variables.foreach { case (name, value) =>
      require(
        isValidVariableName(name),
        s"IO reply fragment variable name must be nonblank without whitespace, ',' or '=', got: $name"
      )
      require(value != null, s"IO reply fragment variable $name must be set")
    }
    require(
      variableNames.distinct.size == variableNames.size,
      s"IO reply fragment variable names must be distinct, duplicate: ${variableNames.diff(variableNames.distinct).mkString(",")}"
    )
    val ioReplyFragmentEntries = ioReplyFragments.map { case (name, ioReplyFragment) =>
      val fields = Seq(
        Some(s"\"markup\":${string(ioReplyFragment.markup.body)}"),
        Option.when(ioReplyFragment.js.nonEmpty)(s"\"js\":${stringList(ioReplyFragment.js)}"),
        Option.when(ioReplyFragment.css.nonEmpty)(s"\"css\":${stringList(ioReplyFragment.css)}")
      ).flatten.mkString(",")
      s"${string(name)}:{$fields}"
    }
    val variableEntries = variables.map { case (name, value) =>
      s"${string(name)}:${string(value)}"
    }
    val titleField = title.fold("")(value => s""","title":${string(value)}""")
    s"""{"meta":{"code":$status,"message":${string(reasonPhrase(status))},"time":${number(time)}},"data":{"fragments":{${ioReplyFragmentEntries.mkString(",")}},"variables":{${variableEntries.mkString(",")}}$titleField}}"""
  }

  def failure(
    status: Int,
    error: Option[IoReplyFragmentEnvelopeError],
    time: Double
  ): String = {
    require(time.isFinite, "envelope time must be finite")
    require(status >= 400 && status <= 599, s"IO reply fragment envelope failure status must be 400 to 599, got: $status")
    val errorFields = error match {
      case Some(envelopeError) =>
        s""","error_type":${string(envelopeError.errorType.wireName)},"error_code":${envelopeError.errorType.code},"error_detail":${string(envelopeError.detail)}"""
      case None => ""
    }
    s"""{"meta":{"code":$status,"message":${string(reasonPhrase(status))},"time":${number(time)}$errorFields},"data":{}}"""
  }

  def reasonPhrase(status: Int): String = reasonPhrases.getOrElse(status, "")

  private def isValidVariableName(name: String): Boolean =
    Option(name).exists(n => n.nonEmpty && !n.exists(c => c.isWhitespace || Character.isSpaceChar(c) || c == '\uFEFF' || c == ',' || c == '='))

  private def number(value: Double): String =
    new java.math.BigDecimal(java.lang.Double.toString(value)).toPlainString

  private def stringList(values: Seq[String]): String =
    values.map(string).mkString("[", ",", "]")

  private def string(value: String): String =
    "\"" + value.flatMap {
      case '"' => "\\\""
      case '\\' => "\\\\"
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case '<' => "\\u003c"
      case '>' => "\\u003e"
      case '&' => "\\u0026"
      case '\u2028' => "\\u2028"
      case '\u2029' => "\\u2029"
      case c if c < ' ' => f"\\u${c.toInt}%04x"
      case c => c.toString
    } + "\""

  private val reasonPhrases: Map[Int, String] = Map(
    100 -> "Continue",
    101 -> "Switching Protocols",
    102 -> "Processing",
    103 -> "Early Hints",
    200 -> "OK",
    201 -> "Created",
    202 -> "Accepted",
    203 -> "Non-Authoritative Information",
    204 -> "No Content",
    205 -> "Reset Content",
    206 -> "Partial Content",
    207 -> "Multi-Status",
    208 -> "Already Reported",
    226 -> "IM Used",
    300 -> "Multiple Choices",
    301 -> "Moved Permanently",
    302 -> "Found",
    303 -> "See Other",
    304 -> "Not Modified",
    305 -> "Use Proxy",
    307 -> "Temporary Redirect",
    308 -> "Permanent Redirect",
    400 -> "Bad Request",
    401 -> "Unauthorized",
    402 -> "Payment Required",
    403 -> "Forbidden",
    404 -> "Not Found",
    405 -> "Method Not Allowed",
    406 -> "Not Acceptable",
    407 -> "Proxy Authentication Required",
    408 -> "Request Timeout",
    409 -> "Conflict",
    410 -> "Gone",
    411 -> "Length Required",
    412 -> "Precondition Failed",
    413 -> "Content Too Large",
    414 -> "URI Too Long",
    415 -> "Unsupported Media Type",
    416 -> "Range Not Satisfiable",
    417 -> "Expectation Failed",
    421 -> "Misdirected Request",
    422 -> "Unprocessable Content",
    423 -> "Locked",
    424 -> "Failed Dependency",
    425 -> "Too Early",
    426 -> "Upgrade Required",
    428 -> "Precondition Required",
    429 -> "Too Many Requests",
    431 -> "Request Header Fields Too Large",
    451 -> "Unavailable For Legal Reasons",
    500 -> "Internal Server Error",
    501 -> "Not Implemented",
    502 -> "Bad Gateway",
    503 -> "Service Unavailable",
    504 -> "Gateway Timeout",
    505 -> "HTTP Version Not Supported",
    506 -> "Variant Also Negotiates",
    507 -> "Insufficient Storage",
    508 -> "Loop Detected",
    510 -> "Not Extended",
    511 -> "Network Authentication Required"
  )
}
