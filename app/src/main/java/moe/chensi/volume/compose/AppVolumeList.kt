package moe.chensi.volume.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.chensi.volume.data.App
import kotlin.collections.sortedWith

fun LazyListScope.group(header: String, apps: List<App>, onChange: (() -> Unit)? = null) {
    if (apps.isNotEmpty()) {
        item {
            Text(
                text = header,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
        }

        items(
            items = apps.sortedWith(App.comparator), key = { app -> app.packageName }) { app ->
            AppVolumeSlider(app, true, onChange)
        }
    }
}

@Composable
fun AppVolumeList(
    apps: MutableCollection<App>,
    showAll: Boolean,
    onChange: (() -> Unit)? = null,
    content: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content?.invoke(this)

        if (!showAll) {
            items(items = apps.filter { app -> !app.hidden && app.players.isNotEmpty() }
                .sortedWith(App.comparator), key = { app -> app.packageName }) { app ->
                AppVolumeSlider(app, false, onChange)
            }
            return@LazyColumn
        }

        val activePlayers = mutableListOf<App>()
        val inactivePlayers = mutableListOf<App>()
        val hiddenPlayers = mutableListOf<App>()
        val otherApps = mutableListOf<App>()

        for (app in apps) {
            if (app.isPlayer) {
                if (!app.hidden) {
                    if (app.players.isNotEmpty()) {
                        activePlayers.add(app)
                    } else {
                        inactivePlayers.add(app)
                    }
                } else {
                    hiddenPlayers.add(app)
                }
            } else {
                otherApps.add(app)
            }
        }

        group("Active", activePlayers, onChange)
        group("Inactive", inactivePlayers, onChange)
        group("Hidden", hiddenPlayers, onChange)
        group("Non-players", otherApps, onChange)
    }
}
