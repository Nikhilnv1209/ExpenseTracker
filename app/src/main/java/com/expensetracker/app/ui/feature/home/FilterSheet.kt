package com.expensetracker.app.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val chipSelected = Color(0xFF7C3AED)
private val chipUnselected = Color.White.copy(alpha = 0.08f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filter: TransactionFilter,
    bankSuggestions: List<String>,
    defaultPeriod: FilterPeriod = FilterPeriod.THIS_MONTH,
    onApply: (TransactionFilter) -> Unit,
    onReset: () -> Unit,
) {
    var search by remember { mutableStateOf(filter.search) }
    var selectedType by remember { mutableStateOf(filter.type) }
    var selectedBank by remember { mutableStateOf(filter.bank) }
    var selectedPeriod by remember { mutableStateOf(filter.period) }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Filters",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row {
                Text(
                    text = "Reset",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7C3AED),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            search = ""
                            selectedType = null
                            selectedBank = null
                            selectedPeriod = defaultPeriod
                            onReset()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        onApply(TransactionFilter(search, selectedType, selectedBank, selectedPeriod))
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(chipSelected, CircleShape),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Apply", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name, alias, or bank") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                cursorColor = Color(0xFF7C3AED),
            ),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(20.dp))

        Text("Period", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPeriod.entries.forEach { period ->
                val selected = period == selectedPeriod
                FilterChip(label = period.label, selected = selected) {
                    selectedPeriod = if (selected) defaultPeriod else period
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All" to null, "Income" to "INCOME", "Expense" to "EXPENSE").forEach { (label, type) ->
                val selected = selectedType == type
                FilterChip(label = label, selected = selected) {
                    selectedType = type
                }
            }
        }

        if (bankSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))

            Text("Bank", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(label = "All", selected = selectedBank == null) {
                    selectedBank = null
                }
                bankSuggestions.forEach { bank ->
                    val selected = selectedBank == bank
                    FilterChip(label = bank, selected = selected) {
                        selectedBank = if (selected) null else bank
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private val FilterPeriod.label: String
    get() = when (this) {
        FilterPeriod.THIS_WEEK -> "This Week"
        FilterPeriod.THIS_MONTH -> "This Month"
        FilterPeriod.LAST_MONTH -> "Last Month"
        FilterPeriod.ALL_TIME -> "All Time"
    }

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) chipSelected else chipUnselected,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}
