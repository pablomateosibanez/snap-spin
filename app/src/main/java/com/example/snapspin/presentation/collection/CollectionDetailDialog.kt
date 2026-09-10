package com.example.snapspin.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.snapspin.R
import com.example.snapspin.domain.model.CollectionItem

/**
 * Ficha del disco registrado: portada grande y los datos que no caben en la lista, entre ellos
 * el año de la edición concreta que se tiene (que puede no ser el de salida del álbum).
 */
@Composable
fun CollectionDetailDialog(item: CollectionItem, onDismiss: () -> Unit) {
    val unknown = stringResource(R.string.detail_unknown)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(item.artist, style = MaterialTheme.typography.titleMedium)
                Text(item.title, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item.coverUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailLine(
                        R.string.detail_album_year,
                        item.albumYear?.toString() ?: item.editionYear?.toString() ?: unknown,
                    )
                    DetailLine(
                        R.string.detail_edition_year,
                        item.editionYear?.toString() ?: unknown,
                    )
                    if (item.genres.isNotEmpty()) {
                        DetailLine(R.string.detail_genres, item.genres.joinToString(" · "))
                    }
                    if (item.styles.isNotEmpty()) {
                        DetailLine(R.string.detail_styles, item.styles.joinToString(", "))
                    }
                    item.labelLine()?.let { DetailLine(R.string.detail_label, it) }
                    if (item.formats.isNotEmpty()) {
                        DetailLine(R.string.detail_format, item.formats.joinToString(", "))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun DetailLine(labelRes: Int, value: String) {
    Text(
        text = stringResource(labelRes, value),
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun CollectionItem.labelLine(): String? {
    val label = labels.firstOrNull() ?: return catalogNumber
    return catalogNumber?.let { "$label · $it" } ?: label
}
