package com.questnotes.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Квадратный флажок чеклиста (только отрисовка, нажатие обрабатывает родитель). */
@Composable
fun CheckMark(checked: Boolean, size: Dp) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(if (checked) KeepMuted else Color.Transparent)
            .border(1.5.dp, KeepMuted, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = KeepBg,
                modifier = Modifier.size(size * 0.8f),
            )
        }
    }
}

/** Текстовое поле без рамки и подчёркивания, с подсказкой. */
@Composable
fun PlainField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    singleLine: Boolean = false,
    onNext: (() -> Unit)? = null,
) {
    Box(modifier) {
        if (value.isEmpty()) {
            Text(hint, style = style.copy(color = KeepMuted))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = style,
            cursorBrush = SolidColor(KeepAccent),
            singleLine = singleLine,
            keyboardOptions = if (onNext != null) KeyboardOptions(imeAction = ImeAction.Next) else KeyboardOptions.Default,
            keyboardActions = if (onNext != null) KeyboardActions(onNext = { onNext() }) else KeyboardActions.Default,
            modifier = fieldModifier.fillMaxWidth(),
        )
    }
}
