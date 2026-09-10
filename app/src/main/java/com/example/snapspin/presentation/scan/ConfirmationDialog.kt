package com.example.snapspin.presentation.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.snapspin.R
import com.example.snapspin.domain.model.EditionChoiceReason
import com.example.snapspin.domain.model.RegistrationProposal
import com.example.snapspin.domain.model.ReleaseDetails
import java.text.NumberFormat
import java.util.Currency

/**
 * Modal de confirmación: es el único paso manual del registro.
 *
 * Muestra el grupo y el título que se van a registrar y, cuando el disco ya está en la colección,
 * cambia las acciones por descartar / sobrescribir.
 */
@Composable
fun ConfirmationDialog(
    proposal: RegistrationProposal,
    onConfirm: () -> Unit,
    onOverwrite: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    val release = proposal.release
    val duplicate = proposal.alreadyInCollection

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (duplicate) R.string.dialog_duplicate_title else R.string.dialog_confirm_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    release.coverImageUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Column {
                        Text(
                            text = release.artist,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = release.title,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }

                release.subtitle()?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = proposal.editionReason(),
                    style = MaterialTheme.typography.bodySmall,
                )

                release.priceLabel()?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    text = stringResource(R.string.folder_label, proposal.folder.name),
                    style = MaterialTheme.typography.bodySmall,
                )

                if (duplicate) {
                    Text(
                        text = stringResource(
                            R.string.dialog_duplicate_body,
                            proposal.existingInstances.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = if (duplicate) onOverwrite else onConfirm) {
                Text(
                    stringResource(
                        if (duplicate) R.string.action_overwrite else R.string.action_register
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = if (duplicate) onDiscard else onDismiss) {
                Text(
                    stringResource(
                        if (duplicate) R.string.action_discard else R.string.action_cancel
                    )
                )
            }
        },
    )
}

@Composable
private fun RegistrationProposal.editionReason(): String = when (edition.reason) {
    EditionChoiceReason.MEDIAN_PRICE ->
        stringResource(R.string.edition_reason_median, edition.editionsEvaluated)
    EditionChoiceReason.MOST_COMMON -> stringResource(R.string.edition_reason_common)
    EditionChoiceReason.ONLY_MATCH -> stringResource(R.string.edition_reason_only)
}

/** Línea con año, país y formato de la edición elegida. */
private fun ReleaseDetails.subtitle(): String? = listOfNotNull(
    year?.toString(),
    country,
    formats.take(3).takeIf { it.isNotEmpty() }?.joinToString(", "),
    labels.firstOrNull()?.let { label -> catalogNumber?.let { "$label · $it" } ?: label },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

@Composable
private fun ReleaseDetails.priceLabel(): String? {
    val price = lowestPrice ?: return null
    return stringResource(R.string.edition_price, formatCurrency(price, currency), copiesForSale)
}

private fun formatCurrency(amount: Double, currencyCode: String?): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        currencyCode?.let { currency = Currency.getInstance(it) }
    }.format(amount)
}.getOrElse { String.format("%.2f", amount) }
