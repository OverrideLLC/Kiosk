package app.kiosksync.designsystem

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visible keyboard focus ring (3 dp) for interactive components. It must be
 * placed before the component's own focusable/clickable modifier so it can
 * observe the focus target.
 */
@Composable
fun Modifier.focusRing(
    shape: Shape,
    width: Dp = 3.dp,
): Modifier = composed {
    val color = MaterialTheme.colorScheme.primary
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused }
        .then(
            if (focused) {
                Modifier.border(width, color, shape)
            } else {
                Modifier
            },
        )
}
