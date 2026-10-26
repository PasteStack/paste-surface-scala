package com.pastestack.surface.view_models.components

/** Presentation policy; scroll remains native in both modes. */
enum SequencePlayback {
  case Direct
  case Queued(framesPerSecond: Int = 30)
}
