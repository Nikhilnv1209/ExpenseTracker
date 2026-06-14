package com.expensetracker.app.ui.feature.aliases

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.data.local.AliasEntity
import com.expensetracker.app.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliasesScreen(
    onBack: () -> Unit,
    viewModel: AliasesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingAlias by remember { mutableStateOf<AliasEntity?>(null) }
    var editText by remember { mutableStateOf("") }

    editingAlias?.let { alias ->
        AlertDialog(
            onDismissRequest = { editingAlias = null },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = editText.trim()
                    if (trimmed.isBlank()) {
                        viewModel.deleteAlias(alias.originalTitle)
                    } else {
                        viewModel.updateAlias(alias.originalTitle, trimmed)
                    }
                    editingAlias = null
                }) { Text("Save", color = Color(0xFF7C3AED)) }
            },
            dismissButton = {
                TextButton(onClick = { editingAlias = null }) { Text("Cancel") }
            },
            title = { Text("Edit Alias") },
            text = {
                Column {
                    Text(
                        text = "Original: ${alias.originalTitle}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            cursorColor = Color(0xFF7C3AED),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TopAppBar(
            title = { Text("Manage Aliases") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        if (uiState.aliases.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No aliases set",
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

                items(uiState.aliases) { alias ->
                    AliasItem(
                        alias = alias,
                        onEdit = {
                            editText = alias.alias
                            editingAlias = alias
                        },
                        onDelete = { viewModel.deleteAlias(alias.originalTitle) },
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AliasItem(
    alias: AliasEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tint = Color(0xFF7C3AED),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alias.alias,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = alias.originalTitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textDecoration = TextDecoration.LineThrough,
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.width(20.dp).height(20.dp),
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.width(20.dp).height(20.dp),
                )
            }
        }
    }
}
