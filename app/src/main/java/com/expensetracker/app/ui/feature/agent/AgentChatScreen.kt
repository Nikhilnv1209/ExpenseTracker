package com.expensetracker.app.ui.feature.agent

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.ai.AiSettingsStore
import com.expensetracker.app.ai.AgentMessageRole
import com.expensetracker.app.ai.AgentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(
    onBack: () -> Unit,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settingsStore = remember { AiSettingsStore(context) }
    var settingsKey by remember { mutableStateOf(0) }
    val isEnabled = remember(settingsKey) { settingsStore.isConfigured() }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Agent") },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showSettings = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "AI settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (!isEnabled) {
                    Text(
                        text = "AI Agent is not enabled. Tap the settings icon to add your API key.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about your spending...") },
                        enabled = isEnabled,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (input.isNotBlank()) {
                                    viewModel.sendMessage(input)
                                    input = ""
                                    scope.launch { listState.animateScrollToItem(messages.size) }
                                }
                            },
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF7C3AED),
                        ),
                        shape = RoundedCornerShape(24.dp),
                        textStyle = TextStyle.Default.copy(fontSize = 15.sp),
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF7C3AED), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (input.isNotBlank()) {
                                        viewModel.sendMessage(input)
                                        input = ""
                                        scope.launch { listState.animateScrollToItem(messages.size) }
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "↑",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            items(messages) { msg ->
                MessageBubble(msg)
            }

            if (isLoading) {
                item { TypingIndicator() }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showSettings) {
        AiSettingsDialog(
            onDismiss = { showSettings = false },
            onSaved = { settingsKey++ },
        )
    }
}

@Composable
private fun MessageBubble(msg: com.expensetracker.app.ai.AgentMessage) {
    val isUser = msg.role == AgentMessageRole.USER
    val bg = if (isUser) Color(0xFF7C3AED) else Color(0xFF2A2738)
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        if (isUser) {
            Text(
                text = msg.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 280.dp)
                    .background(bg, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 300.dp),
            ) {
                msg.reasoning?.let { reasoning ->
                    if (reasoning.isNotBlank()) {
                        ReasoningSection(reasoning, isStreaming = msg.text.isBlank())
                    }
                }

                if (msg.text.isNotBlank()) {
                    com.mikepenz.markdown.m3.Markdown(
                        content = msg.text,
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        colors = com.mikepenz.markdown.m3.markdownColor(
                            text = Color.White,
                            codeText = Color(0xFFE0B0FF),
                            codeBackground = Color(0xFF1A1825),
                            dividerColor = Color.White.copy(alpha = 0.15f),
                        ),
                        typography = com.mikepenz.markdown.m3.markdownTypography(
                            paragraph = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Color.White),
                            h1 = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White),
                            h2 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White),
                            h3 = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                            bullet = TextStyle(fontSize = 14.sp, color = Color.White),
                            code = TextStyle(fontSize = 13.sp, color = Color(0xFFE0B0FF), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasoningSection(reasoning: String, isStreaming: Boolean) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A2E))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isStreaming) "Thought process" else "Thought process",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7C3AED).copy(alpha = 0.7f),
            )
            if (isStreaming) {
                Spacer(Modifier.width(4.dp))
                val infiniteTransition = rememberInfiniteTransition(label = "think_dot")
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "dotAlpha",
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = Color(0xFF7C3AED).copy(alpha = dotAlpha),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (expanded) "▼" else "▶",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.3f),
            )
        }

        Spacer(Modifier.height(4.dp))

        if (expanded) {
            Text(
                text = reasoning,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color.White.copy(alpha = 0.5f),
            )
        } else {
            val previewText = reasoning.takeLast(100).let {
                val newlineIdx = it.indexOf('\n')
                if (newlineIdx >= 0 && it.length > 30) it.substring(newlineIdx + 1) else it
            }.ifBlank { reasoning.take(100) }

            Text(
                text = previewText.ifBlank { "..." },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.6f),
                            Color(0xFF7C3AED).copy(alpha = 0.15f),
                        ),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse",
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmer",
    )

    Row(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2A2738), Color(0xFF1F1B2E)),
                ),
                shape = RoundedCornerShape(18.dp),
            )
            .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.15f * pulse), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = pulse),
                            Color(0xFF5B21B6).copy(alpha = pulse * 0.5f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White.copy(alpha = pulse * 0.9f),
                modifier = Modifier.size(13.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { i ->
                val dotProgress = ((shimmer + i * 0.33f) % 1f)
                val dotAlpha = when {
                    dotProgress < 0.2f -> dotProgress / 0.2f
                    dotProgress < 0.6f -> 1f
                    else -> 1f - (dotProgress - 0.6f) / 0.4f
                }
                Text(
                    text = ".",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED).copy(alpha = dotAlpha * 0.8f),
                )
            }
        }
    }
}

@Composable
private fun AiSettingsDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember { AiSettingsStore(context) }

    var enabled by remember { mutableStateOf(store.isEnabled) }
    var baseUrl by remember { mutableStateOf(store.baseUrl) }
    var model by remember { mutableStateOf(store.model) }
    var apiKey by remember { mutableStateOf(store.apiKey) }
    var showKey by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Text(
                text = "AI Agent Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable AI Agent",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF7C3AED),
                        checkedTrackColor = Color(0xFF7C3AED).copy(alpha = 0.5f),
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            AiTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = "Base URL",
                placeholder = "https://api.openai.com/v1/",
            )

            Spacer(Modifier.height(12.dp))

            AiTextField(
                value = model,
                onValueChange = { model = it },
                label = "Model",
                placeholder = "gpt-4o-mini",
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        text = if (showKey) "Hide" else "Show",
                        fontSize = 12.sp,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showKey = !showKey },
                            )
                            .padding(horizontal = 12.dp),
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    cursorColor = Color(0xFF7C3AED),
                ),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            store.isEnabled = enabled
                            store.baseUrl = baseUrl
                            store.model = model
                            store.apiKey = apiKey
                            onSaved()
                            onDismiss()
                        },
                    ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }
            }
        }
    }
}

@Composable
private fun AiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF7C3AED),
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            cursorColor = Color(0xFF7C3AED),
        ),
        shape = RoundedCornerShape(12.dp),
    )
}
