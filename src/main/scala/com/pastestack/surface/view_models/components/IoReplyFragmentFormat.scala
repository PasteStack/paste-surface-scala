package com.pastestack.surface.view_models.components

enum IoReplyFragmentFormat(val wireName: String) {
  case Html extends IoReplyFragmentFormat("html")
  case Json extends IoReplyFragmentFormat("json")
}
