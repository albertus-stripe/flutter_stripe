package com.reactnativestripesdk

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.events.EventDispatcher
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.stripe.android.databinding.CardInputWidgetBinding
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputListener
import com.stripe.android.view.CardInputWidget

class StripeSdkCardView(context: Context, private val mEventDispatcher: EventDispatcher) : FrameLayout(context) {
  var mCardWidget: CardInputWidget
  val cardDetails: MutableMap<String, Any> = mutableMapOf("brand" to "", "last4" to "", "expiryMonth" to "", "expiryYear" to "", "postalCode" to "")
  var cardParams: PaymentMethodCreateParams.Card? = null

  init {
    mCardWidget = CardInputWidget(context);

    val binding = CardInputWidgetBinding.bind(mCardWidget)
    binding.container.isFocusable = true
    binding.container.isFocusableInTouchMode = true
    binding.container.requestFocus()

    addView(mCardWidget)
    setListeners()

    viewTreeObserver.addOnGlobalLayoutListener { requestLayout() }
  }

  fun setCardStyle(value: ReadableMap) {
    val binding = CardInputWidgetBinding.bind(mCardWidget)
    val borderWidth = getIntOrNull(value, "borderWidth")
    val backgroundColor = getIntOrNull(value, "backgroundColor")
    val borderColor = getIntOrNull(value, "borderColor")
    val borderRadius = getIntOrNull(value, "borderRadius") ?: 0
    val textColor = getIntOrNull(value, "textColor")
    val fontSize = getIntOrNull(value,"fontSize")
    val placeholderColor = getIntOrNull(value, "placeholderColor")
    val textErrorColor = getIntOrNull(value, "textErrorColor")

    textColor?.let {
      binding.cardNumberEditText.setTextColor(it)
      binding.cvcEditText.setTextColor(it)
      binding.expiryDateEditText.setTextColor(it)
      binding.postalCodeEditText.setTextColor(it)
    }
    textErrorColor?.let {
      binding.cardNumberEditText.setErrorColor(it)
      binding.cvcEditText.setErrorColor(it)
      binding.expiryDateEditText.setErrorColor(it)
      binding.postalCodeEditText.setErrorColor(it)
    }
    placeholderColor?.let {
      binding.cardNumberEditText.setHintTextColor(it)
      binding.cvcEditText.setHintTextColor(it)
      binding.expiryDateEditText.setHintTextColor(it)
      binding.postalCodeEditText.setHintTextColor(it)
    }
    fontSize?.let {
      binding.cardNumberEditText.textSize = it.toFloat()
      binding.cvcEditText.textSize = it.toFloat()
      binding.expiryDateEditText.textSize = it.toFloat()
      binding.postalCodeEditText.textSize = it.toFloat()
    }



    mCardWidget.setPadding(40, 0 ,40 ,0)
    mCardWidget.background = MaterialShapeDrawable(
      ShapeAppearanceModel()
        .toBuilder()
        .setAllCorners(CornerFamily.ROUNDED, (borderRadius * 2).toFloat())
        .build()
    ).also { shape ->
      shape.strokeWidth = 0.0f
      shape.strokeColor = ColorStateList.valueOf(Color.parseColor("#000000"))
      shape.fillColor = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
      borderWidth?.let {
        shape.strokeWidth = (it * 2).toFloat()
      }
      borderColor?.let {
        shape.strokeColor = ColorStateList.valueOf(it)
      }
      backgroundColor?.let {
        shape.fillColor = ColorStateList.valueOf(it)
      }
    }
  }

  fun setPlaceHolders(value: ReadableMap) {
    val binding = CardInputWidgetBinding.bind(mCardWidget)
    val numberPlaceholder = getValOr(value, "number", null)
    val expirationPlaceholder = getValOr(value, "expiration", null)
    val cvcPlaceholder = getValOr(value, "cvc", null)
    val postalCodePlaceholder = getValOr(value, "postalCode", null)

    numberPlaceholder?.let {
      binding.cardNumberEditText.hint = it
    }
    expirationPlaceholder?.let {
      binding.expiryDateEditText.hint = it
    }
    cvcPlaceholder?.let {
      mCardWidget.setCvcLabel(it)
    }
    postalCodePlaceholder?.let {
      binding.postalCodeEditText.hint = it
    }
  }

  fun setPostalCodeEnabled(isEnabled: Boolean) {
    mCardWidget.postalCodeEnabled = isEnabled
  }

  fun getValue(): MutableMap<String, Any> {
    return cardDetails
  }

  fun onCardChanged() {
    mCardWidget.paymentMethodCard?.let { cardParams = it }
    mEventDispatcher?.dispatchEvent(
      CardChangedEvent(id, cardDetails, mCardWidget.postalCodeEnabled, cardParams != null))
  }

  private fun setListeners() {
    mCardWidget.setCardInputListener(object : CardInputListener {
      override fun onCardComplete() {
        mCardWidget.cardParams?.let {
          cardDetails["brand"] = mapCardBrand(it.brand)
          cardDetails["last4"] = it.last4
        }
        onCardChanged()
      }
      override fun onExpirationComplete() {}
      override fun onCvcComplete() {
        mCardWidget.cardParams?.let {
          cardDetails["brand"] = mapCardBrand(it.brand)
          cardDetails["last4"] = it.last4
        }
        onCardChanged()
      }
      override fun onFocusChange(focusField: CardInputListener.FocusField) {
        if (mEventDispatcher != null) {
          mEventDispatcher?.dispatchEvent(
            CardFocusEvent(id, focusField.name))
        }
      }
    })

    mCardWidget.setExpiryDateTextWatcher(object : TextWatcher {
      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
      override fun afterTextChanged(p0: Editable?) {}
      override fun onTextChanged(var1: CharSequence?, var2: Int, var3: Int, var4: Int) {
        val splitted = var1.toString().split("/")
        cardDetails["expiryMonth"] = splitted[0]

        if (splitted.size == 2) {
          cardDetails["expiryYear"] = var1.toString().split("/")[1]
        }

        onCardChanged()
      }
    })

    mCardWidget.setPostalCodeTextWatcher(object : TextWatcher {
      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
      override fun afterTextChanged(p0: Editable?) {}
      override fun onTextChanged(var1: CharSequence?, var2: Int, var3: Int, var4: Int) {
        cardDetails["postalCode"] = var1.toString()
        onCardChanged()
      }
    })

  }

  override fun requestLayout() {
    super.requestLayout()
    post(mLayoutRunnable)
  }

  private val mLayoutRunnable = Runnable {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    layout(left, top, right, bottom)
  }
}

