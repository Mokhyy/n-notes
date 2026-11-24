package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.util.DocumentKind
import com.xnotes.settings.ExplorerSortKey
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lists the notes saved as templates under [root], searchable by name and sortable; a tap picks one. */
@Composable
internal fun NoteTemplateDialog(editor: Editor, root: String, onDismiss: () -> Unit, onPick: (BrowseEntry) -> Unit) {
    val palette = LocalPalette.current
    val templates by produceState<List<BrowseEntry>?>(null, root) {
        value = withContext(Dispatchers.IO) { editor.noteTemplateEntries(root) }
    }
    var query by remember { mutableStateOf("") }
    var sortKey by remember { mutableStateOf(ExplorerSortKey.NAME) }
    var descending by remember { mutableStateOf(false) }
    val keys = listOf(ExplorerSortKey.NAME, ExplorerSortKey.MODIFIED, ExplorerSortKey.CREATED)
    SheetDialog(
        title = stringResource(R.string.use_template),
        onDismiss = onDismiss,
        gap = 8.dp,
        foot = { DialogButtons { DialogButton(stringResource(R.string.cancel), onClick = onDismiss) } },
    ) {
        DialogField(query, { query = it }, placeholder = stringResource(R.string.search))
        PanelSegmented(keys.size, keys.indexOf(sortKey), { i ->
            val k = keys[i]
            if (k == sortKey) descending = !descending else { sortKey = k; descending = k != ExplorerSortKey.NAME }
        }, sunken = false) { i, on ->
            val ink = (if (on) palette.selectionForeground else palette.textDim).toComposeColor()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sortLabel(keys[i]), color = ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (on) Icon(if (descending) XnotesIcons.arrowDown else XnotesIcons.arrowUp, null, tint = ink, modifier = Modifier.size(14.dp))
            }
        }
        val list = templates
            ?.filter { it.name.contains(query.trim(), ignoreCase = true) }
            ?.sortedWith(explorerComparator(sortKey, descending) { it.created })
        when {
            list == null -> EmptyNote(stringResource(R.string.loading))
            list.isEmpty() -> EmptyNote(stringResource(R.string.note_templates_empty))
            else -> list.forEach { t ->
                // Each template on a card of its own, as the new note screen's choices are.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(panelColors().card)
                        .clickable(role = Role.Button) { onPick(t) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(if (DocumentKind.ofName(t.name) == DocumentKind.CANVAS) XnotesIcons.canvas else XnotesIcons.file, null, tint = palette.textDim.toComposeColor(), modifier = Modifier.size(20.dp))
                    Text(DocumentKind.stripSuffix(t.name), color = palette.text.toComposeColor(), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
