package com.droidknights.a2ui.catalog.hooks

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.catalog.A2uiAudioPlayerRenderer
import androidx.compose.material3.a2ui.catalog.A2uiImageRenderer
import androidx.compose.material3.a2ui.catalog.A2uiVideoRenderer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

object DefaultA2uiMediaRenderers {

    val imageRenderer: A2uiImageRenderer =
        A2uiImageRenderer { url, contentDescription, _, modifier, onError ->
            if (url.contains("broken") || url.contains("404")) {
                SideEffect {
                    onError(IllegalArgumentException("Simulated 404 image error for $url"))
                }
            }
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contentDescription ?: "이미지",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

    val videoRenderer: A2uiVideoRenderer =
        A2uiVideoRenderer { url, modifier, onError ->
            var isPlaying by remember(url) { mutableStateOf(false) }
            var progress by remember(url) { mutableFloatStateOf(0.35f) }
            if (url.contains("broken")) {
                SideEffect { onError(IllegalStateException("Video stream unreachable: $url")) }
            }
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "영상 플레이어",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                isPlaying = !isPlaying
                                if (isPlaying && progress < 0.9f) progress += 0.2f
                            },
                        ) {
                            Icon(
                                imageVector =
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPlaying) "일시정지" else "재생")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    )
                }
            }
        }

    val audioRenderer: A2uiAudioPlayerRenderer =
        A2uiAudioPlayerRenderer { url, contentDescription, modifier, onError ->
            var isPlaying by remember(url) { mutableStateOf(false) }
            if (url.contains("broken")) {
                SideEffect { onError(IllegalStateException("Audio decode failed: $url")) }
            }
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contentDescription ?: "오디오 플레이어",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = if (isPlaying) "재생 중" else "대기 중",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { isPlaying = !isPlaying }) {
                        Text(if (isPlaying) "정지" else "재생")
                    }
                }
            }
        }

    /**
     * Schemes the A2UI basic catalog implementation guide allows `openUrl` to open (`https`, `http`,
     * `mailto`, `tel`).
     */
    private val ALLOWED_URL_SCHEMES: Set<String> = setOf("https", "http", "mailto", "tel")

    /**
     * Validates [url] against the A2UI Basic Catalog V1 `openUrl` security contract
     * (`A2uiOpenUrlFunction.typedExecute`, AOSP CL `4346347`):
     * - Must be non-empty
     * - Must not contain ISO control characters (`\n`, `\t`, `\u0000`)
     * - Must have a non-empty scheme before the first `:`
     * - Scheme (case-insensitive) must be in [ALLOWED_URL_SCHEMES] (`https`, `http`, `mailto`, `tel`)
     *
     * @throws androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException if [url] is empty,
     *   contains control characters, has no scheme, or uses an unsupported scheme
     */
    fun validateOpenUrlScheme(url: String) {
        if (url.isEmpty()) {
            throw androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException(
                "Function openUrl was invoked with an empty URL"
            )
        }
        if (url.any { it.isISOControl() }) {
            throw androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException(
                "Function openUrl was invoked with a URL with control characters"
            )
        }
        // The scheme is the text before the first ':'. A value with no scheme (a relative path or
        // a bare host) is rejected: there is no base URL to resolve it against.
        val scheme = url.substringBefore(':', missingDelimiterValue = "")
        if (scheme.isEmpty()) {
            throw androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException(
                "Function openUrl was invoked with a URL that has no scheme"
            )
        }
        if (scheme.lowercase() !in ALLOWED_URL_SCHEMES) {
            throw androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException(
                "Function openUrl was invoked with the unsupported URL scheme " +
                    "'$scheme'. Allowed schemes: ${ALLOWED_URL_SCHEMES.joinToString()}"
            )
        }
    }

    /**
     * Creates an [A2uiBasicCatalogV1.Functions] contract implementation.
     *
     * - When [context] is provided (Android runtime / Compose UI), delegates all built-in functions
     *   (`formatNumber`, `formatCurrency`, `formatDate`, `pluralize`, etc.) to the official
     *   `A2uiBasicCatalogV1.Functions(context, localeProvider)` factory introduced in `1.0.0-SNAPSHOT`
     *   (commit `f3ed67ecaa9`), and wraps `openUrl` with scheme validation and optional [onOpenUrl] hook.
     * - When [context] is `null` (pure JVM unit tests), uses the default interface methods on
     *   [A2uiBasicCatalogV1.Functions] (`5098f93b2fc`) and invokes [onOpenUrl] after scheme validation.
     */
    fun createBasicFunctions(
        context: android.content.Context? = null,
        localeProvider: androidx.a2ui.model.catalog.functions.A2uiLocaleProvider =
            androidx.a2ui.model.catalog.functions.A2uiLocaleProvider.Default,
        onOpenUrl: ((String) -> Unit)? = null,
    ): A2uiBasicCatalogV1.Functions {
        return if (context != null) {
            val platformDefaults = A2uiBasicCatalogV1.Functions(context, localeProvider)
            object : A2uiBasicCatalogV1.Functions by platformDefaults {
                override fun openUrl(url: String, executionContext: A2uiExecutionContext) {
                    validateOpenUrlScheme(url)
                    if (onOpenUrl != null) {
                        onOpenUrl(url)
                    } else {
                        platformDefaults.openUrl(url, executionContext)
                    }
                }
            }
        } else {
            object : A2uiBasicCatalogV1.Functions {
                override fun openUrl(url: String, executionContext: A2uiExecutionContext) {
                    validateOpenUrlScheme(url)
                    onOpenUrl?.invoke(url)
                }
            }
        }
    }
}
