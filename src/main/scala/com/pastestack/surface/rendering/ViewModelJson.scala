package com.pastestack.surface.rendering

trait ViewModelJson[A] {
  def encode(model: A): String
}
