/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.reactnativestripesdk
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event

internal class CardFocusEvent constructor(viewTag: Int, private val focusField: String?) : Event<CardFocusEvent>(viewTag) {
  override fun getEventName(): String {
    return EVENT_NAME
  }

  override fun serializeEventData(): WritableMap {
    val eventData = Arguments.createMap()
    eventData.putString("focusedField", focusField)

    return eventData
  }

  companion object {
    const val EVENT_NAME = "topFocusChange"
  }

}
