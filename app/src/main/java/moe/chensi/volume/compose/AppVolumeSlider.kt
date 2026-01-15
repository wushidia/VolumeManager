package moe.chensi.volume.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import moe.chensi.volume.data.App

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppVolumeSlider(
    app: App, menuVisible: Boolean, onChange: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackSlider(
            modifier = Modifier.weight(1f),
            cornerRadius = 20.dp,
            value = app.volume,
            onValueChange = { value ->
                app.volume = value
                onChange?.invoke()
            }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp, 8.dp)
            ) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = "App icon",
                    modifier = Modifier.width(32.dp),
                    contentScale = ContentScale.FillWidth
                )

                Text(text = app.name)
            }
        }

        if (menuVisible) {
            ToggleButton(
                checked = app.hidden,
                checkedIcon = Icons.Default.Visibility,
                checkedDescription = "Unhide app",
                uncheckedIcon = Icons.Default.VisibilityOff,
                uncheckedDescription = "Hide app"
            ) {
                app.hidden = it
            }

            ToggleButton(
                checked = app.disableVolumeButtons,
                checkedIcon = Icons.AutoMirrored.Filled.VolumeUp,
                checkedDescription = "Enable volume buttons",
                uncheckedIcon = Icons.AutoMirrored.Filled.VolumeOff,
                uncheckedDescription = "Disable volume buttons"
            ) {
                app.disableVolumeButtons = it
            }
        }
    }
}