package com.yourcompany.glowparty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yourcompany.glowparty.ui.theme.Glow
import kotlin.math.roundToInt

/** Screen header that sits below the status bar (edge-to-edge safe). */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Glow.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Glow.SurfaceHi)
                .border(1.dp, Glow.Line, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Text("←", color = Glow.Ink) }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Glow.Ink)
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = Glow.Dim,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Glow.Surface)
            .border(1.dp, Glow.Line, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
fun RowDivider() = HorizontalDivider(color = Glow.Line, thickness = 1.dp)

@Composable
fun SliderRow(label: String, value: Int, range: IntRange, valueText: String, onChange: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Glow.Ink, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = Glow.Dim)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(thumbColor = Glow.Pink, activeTrackColor = Glow.Pink),
        )
    }
}

@Composable
fun SwitchRow(
    label: String,
    sub: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (enabled) Glow.Ink else Glow.Dim)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = Glow.Dim)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Glow.Pink, checkedThumbColor = Color.White),
        )
    }
}

@Composable
fun LinkRow(label: String, sub: String? = null, dot: Color = Glow.Dim, trailing: String = "›", onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Glow.Ink)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = Glow.Dim)
        }
        Text(trailing, color = Glow.Dim, style = MaterialTheme.typography.bodyLarge)
    }
}
