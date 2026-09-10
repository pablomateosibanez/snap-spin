package com.example.snapspin.presentation.collection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.snapspin.R
import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.model.CollectionOrder

/**
 * Listado de los discos ya registrados: buscador por grupo o título y ordenación por orden de
 * alta o alfabética. Cada fila muestra portada, grupo, título y año.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(viewModel: CollectionViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onBack)

    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.collection_title))
                        if (state.total > 0) {
                            Text(
                                text = if (state.isFiltered) {
                                    stringResource(
                                        R.string.collection_count_filtered,
                                        state.visible.size,
                                        state.total,
                                    )
                                } else {
                                    stringResource(R.string.collection_count, state.total)
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                label = { Text(stringResource(R.string.collection_search_hint)) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.collection_clear_search),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            OrderChips(
                selected = state.order,
                onSelect = viewModel::onOrderChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.loading && state.visible.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.error != null -> ErrorState(
                        message = state.error.orEmpty(),
                        onRetry = viewModel::refresh,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.visible.isEmpty() -> Text(
                        text = stringResource(
                            if (state.isFiltered) {
                                R.string.collection_no_results
                            } else {
                                R.string.collection_empty
                            }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                    )

                    else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(state.visible, key = { it.instanceId }) { item ->
                            CollectionRow(
                                item = item,
                                onClick = { viewModel.openDetail(item) },
                                onDelete = { viewModel.askToDelete(item) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        state.selected?.let { item ->
            CollectionDetailDialog(item = item, onDismiss = viewModel::closeDetail)
        }

        state.pendingDeletion?.let { item ->
            DeleteConfirmationDialog(
                item = item,
                deleting = state.deleting,
                onConfirm = viewModel::confirmDeletion,
                onDismiss = viewModel::cancelDeletion,
            )
        }
    }
}

/** Confirmación antes de borrar: el alta se recupera escaneando otra vez, pero mejor preguntar. */
@Composable
private fun DeleteConfirmationDialog(
    item: CollectionItem,
    deleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text(stringResource(R.string.collection_delete_title)) },
        text = {
            Text(stringResource(R.string.collection_delete_body, item.title, item.artist))
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !deleting) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !deleting) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun OrderChips(
    selected: CollectionOrder,
    onSelect: (CollectionOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CollectionOrder.entries.forEach { order ->
            FilterChip(
                selected = order == selected,
                onClick = { onSelect(order) },
                label = { Text(stringResource(order.labelRes())) },
            )
        }
    }
}

@Composable
private fun CollectionRow(item: CollectionItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.artist,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle()?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.collection_delete),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

/** Año de salida del álbum y género, que es lo que identifica el disco de un vistazo. */
private fun CollectionItem.subtitle(): String? = listOfNotNull(
    displayYear?.toString(),
    genres.joinToString(" · ").takeIf { it.isNotBlank() },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

private fun CollectionOrder.labelRes(): Int = when (this) {
    CollectionOrder.RECENT_FIRST -> R.string.order_recent
    CollectionOrder.OLDEST_FIRST -> R.string.order_oldest
    CollectionOrder.ARTIST -> R.string.order_artist
    CollectionOrder.TITLE -> R.string.order_title
}
