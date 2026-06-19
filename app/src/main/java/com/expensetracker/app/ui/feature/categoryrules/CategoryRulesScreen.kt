package com.expensetracker.app.ui.feature.categoryrules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.data.local.CategoryRuleEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.ui.components.GlassCard
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRulesScreen(
    onBack: () -> Unit,
    viewModel: CategoryRulesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingRule by remember { mutableStateOf<CategoryRuleEntity?>(null) }

    editingRule?.let { rule ->
        CategoryRuleEditorDialog(
            rule = rule,
            onDismiss = { editingRule = null },
            onSelect = { category ->
                viewModel.updateCategory(rule.title, category)
                editingRule = null
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TopAppBar(
            title = { Text("Category Rules") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        if (uiState.rules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No category rules yet",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(uiState.rules) { rule ->
                    CategoryRuleItem(
                        rule = rule,
                        onEdit = { editingRule = rule },
                        onDelete = { viewModel.deleteRule(rule.title) },
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CategoryRuleItem(
    rule: CategoryRuleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = Category.fromDisplayName(rule.category)
    val catColor = categoryColor(category)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tint = catColor,
        tintAlpha = 0.06f,
        borderAlpha = 0.08f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(catColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = category.displayName,
                    tint = catColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = category.displayName,
                    fontSize = 12.sp,
                    color = catColor,
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = com.expensetracker.app.ui.feature.home.categoryIcon(category),
                    contentDescription = "Change category",
                    tint = catColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete rule",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryRuleEditorDialog(
    rule: CategoryRuleEntity,
    onDismiss: () -> Unit,
    onSelect: (Category) -> Unit,
) {
    var selected by remember { mutableStateOf(Category.fromDisplayName(rule.category)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text("Save", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Change category", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = rule.title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(8.dp))
                Category.entries.forEach { cat ->
                    val catColor = categoryColor(cat)
                    val isSelected = cat == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = cat }
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    color = if (isSelected) catColor else catColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = categoryIcon(cat),
                                contentDescription = cat.displayName,
                                tint = if (isSelected) Color.White else catColor,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = cat.displayName,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
    )
}
