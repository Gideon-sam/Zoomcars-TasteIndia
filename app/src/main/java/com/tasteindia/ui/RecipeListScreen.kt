package com.tasteindia.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.tasteindia.domain.*
import com.tasteindia.ui.theme.Forest
import com.tasteindia.ui.theme.Saffron

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    state: ListUiState,
    contentPadding: PaddingValues,
    title: String,
    subtitle: String,
    onQuery: (String) -> Unit,
    onApply: (String?, String?, Boolean, SortOrder) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onDismissNotice: () -> Unit,
    onToggle: (MealSummary) -> Unit,
    onOpen: (String) -> Unit,
) {
    SystemBarAppearance(darkStatusIcons = false)
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    if (showFilters) FilterSheet(state, onDismiss = { showFilters = false }, onApply = { c, i, f, s ->
        onApply(c, i, f, s); showFilters = false
    }, onClear = { onClear(); showFilters = false })

    Column(Modifier.fillMaxSize().padding(bottom = contentPadding.calculateBottomPadding())) {
        Surface(color = Forest) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("TASTE INDIA", style = MaterialTheme.typography.labelLarge, color = Saffron,
                    fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.headlineLarge, color = Color.White,
                    fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(.78f))
                Spacer(Modifier.height(18.dp))
                SearchBar(state.filters.query, onQuery, state.filters.activeCount, { showFilters = true })
            }
        }

        AnimatedVisibility(state.notice != null) {
            NoticeBanner(state.notice.orEmpty(), state.source?.name == "REVIEW_FIXTURE", onRetry, onDismissNotice)
        }

        when {
            state.loading -> LoadingList()
            state.error != null && state.allMeals.isEmpty() -> ErrorState(state.error, onRetry)
            else -> {
                ResultHeader(state, onClear)
                AppliedFilters(state.filters, onQuery, onApply)
                if (state.meals.isEmpty()) EmptyState(state.filters.activeCount > 0, onClear)
                else LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.meals, key = { it.id }) { meal ->
                        RecipeRow(meal, state.details[meal.id], meal.id in state.favourites, { onToggle(meal) }, { onOpen(meal.id) })
                    }
                    if (state.refreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = "Refreshing recipes" }) }
                    item {
                        Text("Recipes and images from TheMealDB", Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppliedFilters(filters: RecipeFilters, onQuery: (String) -> Unit, onApply: (String?, String?, Boolean, SortOrder) -> Unit) {
    if (filters.activeCount == 0) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 10.dp).semantics { contentDescription = "Applied filters" },
    ) {
        if (filters.query.isNotBlank()) item { InputChip(true, { onQuery("") }, { Text("“${filters.query}”") }, trailingIcon = { Icon(Icons.Outlined.Close, "Remove search", Modifier.size(16.dp)) }) }
        filters.category?.let { value -> item { InputChip(true, { onApply(null, filters.ingredient, filters.favouritesOnly, filters.sort) }, { Text(value) }, trailingIcon = { Icon(Icons.Outlined.Close, "Remove category", Modifier.size(16.dp)) }) } }
        filters.ingredient?.let { value -> item { InputChip(true, { onApply(filters.category, null, filters.favouritesOnly, filters.sort) }, { Text(value) }, trailingIcon = { Icon(Icons.Outlined.Close, "Remove ingredient", Modifier.size(16.dp)) }) } }
        if (filters.favouritesOnly) item { InputChip(true, { onApply(filters.category, filters.ingredient, false, filters.sort) }, { Text("Favourites") }, leadingIcon = { Icon(Icons.Rounded.Favorite, null, Modifier.size(16.dp)) }) }
        if (filters.sort == SortOrder.ZA) item { InputChip(true, { onApply(filters.category, filters.ingredient, filters.favouritesOnly, SortOrder.AZ) }, { Text("Z–A") }, trailingIcon = { Icon(Icons.Outlined.Close, "Reset sort", Modifier.size(16.dp)) }) }
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, activeCount: Int, onFilters: () -> Unit) {
    val focus = LocalFocusManager.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query, onValueChange = onQuery, modifier = Modifier.weight(1f), singleLine = true,
            placeholder = { Text("Search Indian recipes") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, "Clear search") } },
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        BadgedBox(badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }) {
            FilledTonalIconButton(onClick = onFilters, modifier = Modifier.size(54.dp)) {
                Icon(Icons.Outlined.Tune, "Open filters")
            }
        }
    }
}

@Composable
private fun ResultHeader(state: ListUiState, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${state.meals.size} ${if (state.meals.size == 1) "recipe" else "recipes"}", fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.weight(1f))
        if (state.filters.activeCount > 0) TextButton(onClick = onClear) { Icon(Icons.Outlined.ClearAll, null); Spacer(Modifier.width(4.dp)); Text("Clear all") }
    }
}

@Composable
private fun RecipeRow(meal: MealSummary, recipe: Recipe?, favourite: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    ElevatedCard(onClick = onOpen, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 132.dp)) {
            MealImage(meal.id, meal.image, Modifier.width(132.dp).fillMaxHeight().heightIn(min = 132.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp, top = 15.dp, bottom = 15.dp)) {
                recipe?.category?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) }
                Text(meal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                recipe?.mainIngredient?.let { Text("Main ingredient · $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                Spacer(Modifier.weight(1f))
                Text("View recipe  →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            IconToggleButton(checked = favourite, onCheckedChange = { onToggle() }, modifier = Modifier.padding(4.dp).semantics {
                contentDescription = if (favourite) "Remove ${meal.name} from favourites" else "Add ${meal.name} to favourites"
            }) { Icon(if (favourite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (favourite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current) }
        }
    }
}

@Composable
fun MealImage(id: String, url: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(model = url, contentDescription = null, modifier = modifier.clip(RoundedCornerShape(0.dp)), contentScale = ContentScale.Crop) {
        when (painter.state) {
            is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            is coil.compose.AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) { CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.Center), strokeWidth = 2.dp) }
            else -> SubcomposeAsyncImage(model = "file:///android_asset/fixtures/images/$id.jpg", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) {
                if (painter.state is coil.compose.AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else ImagePlaceholder(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Restaurant, "Recipe image unavailable", Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable private fun LoadingList() = Column(Modifier.fillMaxWidth().padding(20.dp).semantics { contentDescription = "Loading Indian recipes" }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    repeat(4) { Surface(Modifier.fillMaxWidth().height(132.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(22.dp)) {} }
}

@Composable private fun ErrorState(message: String?, retry: () -> Unit) = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.CloudOff, null, Modifier.size(54.dp)); Spacer(Modifier.height(12.dp)); Text("Couldn’t load the menu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(message.orEmpty(), Modifier.padding(vertical = 8.dp)); Button(onClick = retry) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Try again") } }
}

@Composable private fun EmptyState(filtered: Boolean, clear: () -> Unit) = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.SearchOff, null, Modifier.size(54.dp)); Spacer(Modifier.height(12.dp)); Text(if (filtered) "No recipes match" else "Nothing saved yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(if (filtered) "Try removing a filter or changing your search." else "Tap the heart on a recipe to keep it here.", Modifier.padding(vertical = 8.dp)); if (filtered) OutlinedButton(onClick = clear) { Text("Clear all filters") } }
}

@Composable private fun NoticeBanner(message: String, fixture: Boolean, retry: () -> Unit, dismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) { Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (fixture) Icons.Outlined.OfflineBolt else Icons.Outlined.Info, null); Text(message, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodySmall); TextButton(onClick = retry) { Text("Retry") }; IconButton(onClick = dismiss) { Icon(Icons.Outlined.Close, "Dismiss message") } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(state: ListUiState, onDismiss: () -> Unit, onApply: (String?, String?, Boolean, SortOrder) -> Unit, onClear: () -> Unit) {
    var category by remember(state.filters.category) { mutableStateOf(state.filters.category) }
    var ingredient by remember(state.filters.ingredient) { mutableStateOf(state.filters.ingredient) }
    var favouritesOnly by remember(state.filters.favouritesOnly) { mutableStateOf(state.filters.favouritesOnly) }
    var sort by remember(state.filters.sort) { mutableStateOf(state.filters.sort) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("Refine your menu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Filters combine to narrow the Indian collection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterGroup("CATEGORY", state.categories, category) { category = if (category == it) null else it }
            FilterGroup("MAIN INGREDIENT", state.ingredients, ingredient) { ingredient = if (ingredient == it) null else it }
            Text("SORT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SortOrder.entries.forEachIndexed { index, order ->
                    SegmentedButton(selected = sort == order, onClick = { sort = order }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(if (order == SortOrder.AZ) "Name A–Z" else "Name Z–A") }
                }
            }
            if (!state.favouritesDestination) Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Favourites only", fontWeight = FontWeight.Bold); Text("Show recipes you saved", style = MaterialTheme.typography.bodySmall) }; Switch(favouritesOnly, { favouritesOnly = it }) }
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear all") }; Button(onClick = { onApply(category, ingredient, favouritesOnly, sort) }, modifier = Modifier.weight(1f)) { Text("Show recipes") } }
        }
    }
}

@Composable private fun FilterGroup(label: String, choices: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
    if (choices.isEmpty()) Text("Choices appear after recipe details load.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(choices) { choice -> FilterChip(selected = selected == choice, onClick = { onSelect(choice) }, label = { Text(choice) }, leadingIcon = if (selected == choice) {{ Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }} else null) } }
}
