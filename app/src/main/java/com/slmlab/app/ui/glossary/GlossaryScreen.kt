@file:OptIn(ExperimentalMaterial3Api::class)

package com.slmlab.app.ui.glossary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.GlossaryTerm
import com.slmlab.app.ui.components.EmptyState
import com.slmlab.app.ui.components.PopCard
import com.slmlab.app.ui.components.PopTag
import com.slmlab.app.ui.components.PopTopBar
import com.slmlab.app.ui.theme.LocalPop

@Composable
fun GlossaryScreen() {
    val vm: GlossaryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlossaryViewModel(AppContainer.repo) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { PopTopBar(title = "Glossary", overline = "Words you will meet") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Search terms") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
            )

            val error = state.error
            when {
                state.loading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                error != null -> EmptyState(
                    message = error,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    actionLabel = "Retry",
                    onAction = { vm.retry() },
                )

                state.groups.isEmpty() -> EmptyState(
                    message = "No terms match.",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    state.groups.forEach { group ->
                        item(key = "letter-${group.letter}") {
                            Row(modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)) {
                                PopTag(text = group.letter.toString(), fill = LocalPop.current.pink)
                            }
                        }
                        items(group.terms) { term ->
                            TermRow(term)
                        }
                    }
                    item(key = "end") {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TermRow(term: GlossaryTerm) {
    PopCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        radius = 16.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = term.term,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = term.definition,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
