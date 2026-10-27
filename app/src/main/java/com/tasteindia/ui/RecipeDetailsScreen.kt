package com.tasteindia.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasteindia.domain.Recipe
import com.tasteindia.domain.safeWebUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsScreen(state: DetailsUiState, onBack: () -> Unit, onRetry: () -> Unit, onToggle: () -> Unit) {
    SystemBarAppearance(darkStatusIcons = !isSystemInDarkTheme())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    if (state.recipe != null) IconToggleButton(checked = state.favourite, onCheckedChange = { onToggle() }, modifier = Modifier.semantics {
                        contentDescription = if (state.favourite) "Remove from favourites" else "Add to favourites"
                    }) { Icon(if (state.favourite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (state.favourite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current) }
                },
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding).semantics { contentDescription = "Loading recipe details" }, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.CloudOff, null, Modifier.size(54.dp)); Spacer(Modifier.height(12.dp)); Text("Recipe unavailable", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(state.error, Modifier.padding(vertical = 10.dp)); Button(onClick = onRetry) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Try again") } }
            }
            state.recipe != null -> DetailsContent(state.recipe, padding)
        }
    }
}

@Composable
private fun DetailsContent(recipe: Recipe, padding: PaddingValues) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { value ->
        safeWebUrl(value)?.let { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.recoverCatching { e -> if (e is ActivityNotFoundException) Unit else throw e } }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(310.dp)) {
                MealImage(recipe.id, recipe.image, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.72f)), startY = 100f)))
                Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                    recipe.category?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }
                    Text(recipe.name, style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    recipe.area?.let { Text("From $it", color = Color.White.copy(.86f), style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        if (recipe.tags.isNotEmpty()) item {
            androidx.compose.foundation.lazy.LazyRow(Modifier.padding(top = 18.dp), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recipe.tags.size) { index -> Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) { Text(recipe.tags[index], Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) } }
            }
        }
        if (recipe.ingredients.isNotEmpty()) {
            item { SectionTitle("Ingredients", "Everything to gather") }
            itemsIndexed(recipe.ingredients) { index, ingredient ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                    Text(ingredient.name, Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
                    if (ingredient.measure.isNotBlank()) Text(ingredient.measure, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        recipe.instructions?.let { instructions ->
            item {
                SectionTitle("Method", "Take it one step at a time")
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    instructions.split(Regex("\\n\\s*\\n|\\r?\\n")).map(String::trim).filter(String::isNotEmpty).forEachIndexed { index, paragraph ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Text("${index + 1}.", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(30.dp))
                            Text(paragraph, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        if (recipe.extras.isNotEmpty()) item {
            SectionTitle("More details", "Additional information from TheMealDB")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recipe.extras.forEach { (label, value) ->
                    if (safeWebUrl(value) != null) LinkButton(label, Icons.AutoMirrored.Outlined.OpenInNew) { openUrl(value) }
                    else Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(.42f), fontWeight = FontWeight.SemiBold); Text(value, Modifier.weight(.58f), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if (recipe.source != null || recipe.video != null) item {
            SectionTitle("Keep exploring", "Open trusted links in your browser")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recipe.source?.let { LinkButton("Read the original recipe", Icons.AutoMirrored.Outlined.Article) { openUrl(it) } }
                recipe.video?.let { LinkButton("Watch the cooking video", Icons.Outlined.PlayCircle) { openUrl(it) } }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 26.dp))
            Text("Recipe data and images provided by TheMealDB", Modifier.fillMaxWidth().padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) = Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

@Composable private fun LinkButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) { Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(label, Modifier.weight(1f)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp)) }
}
