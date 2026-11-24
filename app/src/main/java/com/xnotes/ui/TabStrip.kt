package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.util.DocumentKind
import com.xnotes.settings.OpenTab
import com.xnotes.settings.ToolbarPosition
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

private val STRIP_HEIGHT = 38.dp
private val TAB_MIN = 120.dp
private val TAB_MAX = 220.dp

/**
 * The open tabs over the editor, drawn like a browser's: the shown tab is cut from the surface just
 * below it (the docked toolbar, or the desk when the bar floats), the rest sit dim on the strip.
 * [primary] and [secondary] are the tabs on screen in the two panes, matched by [same]; while both are
 * tabs they are drawn as one split tab, its halves in the panes' order. [focusedSecondary] says which half has the focus.
 */
@Composable
internal fun TabStrip(
    tabs: List<OpenTab>,
    primary: String?,
    secondary: String?,
    focusedSecondary: Boolean,
    same: (String, String) -> Boolean,
    onSelect: (OpenTab) -> Unit,
    onClose: (OpenTab) -> Unit,
    onMoveFirst: (OpenTab) -> Unit,
    onSplit: (OpenTab) -> Unit,
    onCloseSplit: () -> Unit,
    onColor: (OpenTab, com.xnotes.core.model.Rgba?) -> Unit,
    onCloseOthers: () -> Unit,
) {
    val palette = LocalPalette.current
    val look = LocalToolbarLook.current
    val barBelow = !look.floating && look.position == ToolbarPosition.TOP
    val below = (if (barBelow) palette.panel else palette.bg).toComposeColor()
    val strip = (if (barBelow) palette.bg else palette.panel).toComposeColor()
    fun indexOf(uri: String?) = if (uri == null) -1 else tabs.indexOfFirst { same(it.uri, uri) }
    val left = indexOf(primary)
    val right = indexOf(secondary)
    val shown = if (focusedSecondary) right else left
    val merged = left >= 0 && right >= 0 && left != right
    // The split tab stands where the first of its two tabs did; the other is folded into it.
    val lead = if (merged) minOf(left, right) else shown
    val folded = if (merged) maxOf(left, right) else -1
    val onScreen = setOf(left, right) - setOf(-1)
    Row(
        Modifier.fillMaxWidth().height(STRIP_HEIGHT).background(strip),
        verticalAlignment = Alignment.Bottom,
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val width = (maxWidth / tabs.size.coerceAtLeast(1)).coerceIn(TAB_MIN, TAB_MAX)
            val scroll = rememberScrollState()
            val widthPx = with(LocalDensity.current) { width.roundToPx() }
            LaunchedEffect(lead, tabs.size) { if (lead >= 0) scroll.animateScrollTo((lead * widthPx - widthPx).coerceAtLeast(0)) }
            Row(Modifier.fillMaxHeight().horizontalScroll(scroll).padding(start = 6.dp), verticalAlignment = Alignment.Bottom) {
                var previous = -1
                tabs.forEachIndexed { i, tab ->
                    if (i == folded) return@forEachIndexed
                    // A hairline parts two dim tabs; none is drawn against the shown one, which has its own edge.
                    val parted = previous >= 0 && i != lead && previous != lead
                    previous = i
                    if (merged && i == lead) {
                        SplitTab(tabs[left], tabs[right], focusedSecondary, below, Modifier.width(width * SPLIT_WIDTH), onSelect, onCloseSplit)
                    } else {
                        Tab(tab, i == shown, parted, below, Modifier.width(width), { onSelect(tab) }, { onClose(tab) })
                    }
                }
            }
        }
        TabCounter(tabs, shown, onScreen, primary != null, onSelect, onClose, onMoveFirst, onSplit, onColor, onCloseOthers)
    }
}

@Composable
private fun Tab(
    tab: OpenTab,
    shown: Boolean,
    parted: Boolean,
    below: Color,
    modifier: Modifier,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalPalette.current
    val shape = MaterialTheme.shapes.small.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp))
    Box(modifier.height(STRIP_HEIGHT - 6.dp)) {
        if (parted) {
            Box(Modifier.align(Alignment.CenterStart).width(1.dp).height(16.dp).background(palette.border.toComposeColor()))
        }
        Row(
            Modifier
                .fillMaxHeight()
                .padding(horizontal = if (shown) 0.dp else 3.dp, vertical = if (shown) 0.dp else 3.dp)
                .clip(if (shown) shape else MaterialTheme.shapes.small)
                .background(if (shown) below else Color.Transparent)
                .clickable(role = Role.Tab, onClick = onSelect)
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dim = palette.textDim.toComposeColor()
            Icon(
                kindIcon(tab.name),
                contentDescription = null,
                tint = tab.color?.toComposeColor() ?: dim,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                DocumentKind.stripSuffix(tab.name),
                color = if (shown) palette.text.toComposeColor() else dim,
                fontSize = 13.sp,
                fontWeight = if (shown) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(24.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(XnotesIcons.close, contentDescription = stringResource(R.string.close_tab), tint = dim, modifier = Modifier.size(14.dp))
            }
        }
        tab.color?.let { c ->
            // The colour code runs along the tab's top edge, as a tab group's does.
            Box(
                Modifier.align(Alignment.TopCenter).padding(horizontal = if (shown) 10.dp else 13.dp)
                    .padding(top = if (shown) 0.dp else 3.dp).fillMaxWidth().height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)).background(c.toComposeColor()),
            )
        }
    }
}

/** How much wider than a tab the split tab is: room for two names, not two whole tabs. */
private const val SPLIT_WIDTH = 1.7f

/** The two tabs a split screen shows, as one tab with a half for each pane; a half takes that pane's focus, the x ends the split. */
@Composable
private fun SplitTab(
    first: OpenTab,
    second: OpenTab,
    focusedSecond: Boolean,
    below: Color,
    modifier: Modifier,
    onSelect: (OpenTab) -> Unit,
    onCloseSplit: () -> Unit,
) {
    val palette = LocalPalette.current
    val shape = MaterialTheme.shapes.small.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp))
    val dim = palette.textDim.toComposeColor()
    Row(
        modifier.height(STRIP_HEIGHT - 6.dp).clip(shape).background(below).padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        @Composable
        fun half(tab: OpenTab, focused: Boolean) {
            Box(Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab) { onSelect(tab) }) {
                Row(Modifier.fillMaxHeight().padding(start = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(kindIcon(tab.name), contentDescription = null, tint = tab.color?.toComposeColor() ?: dim, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        DocumentKind.stripSuffix(tab.name),
                        color = if (focused) palette.text.toComposeColor() else dim,
                        fontSize = 13.sp,
                        fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                tab.color?.let { c ->
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(horizontal = 10.dp).fillMaxWidth().height(3.dp)
                            .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)).background(c.toComposeColor()),
                    )
                }
            }
        }
        half(first, !focusedSecond)
        Box(Modifier.width(1.dp).height(16.dp).background(palette.border.toComposeColor()))
        half(second, focusedSecond)
        Box(
            Modifier.size(24.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onCloseSplit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(XnotesIcons.close, contentDescription = stringResource(R.string.close_split), tint = dim, modifier = Modifier.size(14.dp))
        }
    }
}

private fun kindIcon(name: String): ImageVector = when (DocumentKind.ofName(name)) {
    DocumentKind.CANVAS -> XnotesIcons.canvas
    DocumentKind.PDF -> XnotesIcons.pdfFile
    else -> XnotesIcons.note
}

/** The number of tabs in a box, opening the list of them all with what can be done to each. */
@Composable
private fun TabCounter(
    tabs: List<OpenTab>,
    shown: Int,
    onScreen: Set<Int>,
    canSplit: Boolean,
    onSelect: (OpenTab) -> Unit,
    onClose: (OpenTab) -> Unit,
    onMoveFirst: (OpenTab) -> Unit,
    onSplit: (OpenTab) -> Unit,
    onColor: (OpenTab, com.xnotes.core.model.Rgba?) -> Unit,
    onCloseOthers: () -> Unit,
) {
    val palette = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    // The tab whose own actions the list shows in place of the tabs, and whether that is its colours.
    var acting by remember { mutableStateOf<String?>(null) }
    var colours by remember { mutableStateOf(false) }
    LaunchedEffect(open) { if (!open) { acting = null; colours = false } }
    val text = palette.text.toComposeColor()
    Box(Modifier.fillMaxHeight().padding(end = 6.dp), contentAlignment = Alignment.Center) {
        Tooltip(stringResource(R.string.all_tabs)) {
            Row(
                Modifier.height(30.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button) { open = true }.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(
                    Modifier.widthIn(min = 20.dp).height(20.dp).border(1.5.dp, text, MaterialTheme.shapes.extraSmall).padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // The font's own padding and line gap would sit the digits low in the box.
                    Text(
                        if (tabs.size > 99) "\u221E" else tabs.size.toString(),
                        color = text,
                        fontSize = if (tabs.size > 99) 14.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
                            ),
                        ),
                    )
                }
                Icon(XnotesIcons.chevronDown, contentDescription = null, tint = palette.textDim.toComposeColor(), modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val tab = acting?.let { uri -> tabs.firstOrNull { it.uri == uri } }
            when {
                tab != null && colours -> ColorCodeMenuContent({ colours = false }) { c -> open = false; onColor(tab, c) }
                tab != null -> {
                    MenuBack(DocumentKind.stripSuffix(tab.name)) { acting = null }
                    MenuItem(stringResource(R.string.move_to_first), XnotesIcons.arrowUp, enabled = tabs.firstOrNull() != tab) { open = false; onMoveFirst(tab) }
                    MenuItem(stringResource(R.string.open_in_split), XnotesIcons.split, enabled = canSplit && tabs.indexOf(tab) !in onScreen) { open = false; onSplit(tab) }
                    MenuItem(stringResource(R.string.colour_code), XnotesIcons.palette, more = true) { colours = true }
                    MenuItem(stringResource(R.string.close_tab), XnotesIcons.close, danger = true) { open = false; onClose(tab) }
                }
                else -> {
                    MenuItem(stringResource(R.string.close_all_tabs), XnotesIcons.close) { open = false; onCloseOthers() }
                    MenuDivider()
                    tabs.forEachIndexed { i, t ->
                        DropdownMenuItem(
                            text = { Text(DocumentKind.stripSuffix(t.name), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { open = false; onSelect(t) },
                            selected = i == shown || i in onScreen,
                            leadingIcon = {
                                Icon(kindIcon(t.name), contentDescription = null, tint = t.color?.toComposeColor() ?: androidx.compose.material3.LocalContentColor.current, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape).clickable(role = Role.Button) { acting = t.uri },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(XnotesIcons.more, contentDescription = stringResource(R.string.tab_options), modifier = Modifier.size(18.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
