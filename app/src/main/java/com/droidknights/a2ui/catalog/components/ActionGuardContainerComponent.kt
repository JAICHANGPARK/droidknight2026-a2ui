package com.droidknights.a2ui.catalog.components

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.ProvideActionInterceptor
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.schema.DomainCatalogSchemas

object ActionGuardContainerComponent : DomainCatalogSchemas.ActionGuardContainer {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        guardedActionName: String,
        guardTitle: String,
        modifier: Modifier,
    ) {
        var pendingPayload by remember { mutableStateOf<Map<String, Any?>?>(null) }

        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProvideActionInterceptor(
                onIntercept = { actionPayload ->
                    val eventMap = actionPayload["event"] as? Map<*, *>
                    val eventName = eventMap?.get("name") as? String
                    if (eventName == guardedActionName) {
                        pendingPayload = actionPayload
                        true
                    } else {
                        false
                    }
                }
            ) {
                val childState = observeA2uiComponentState(childId)
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator(Modifier.size(20.dp))
                    is A2uiComponentState.Error ->
                        Text(
                            childState.exception.message.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }

            AnimatedContent(targetState = pendingPayload, label = "LocalGuardModal") { intercepted ->
                if (intercepted != null) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = guardTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Text(
                                text = "'$guardedActionName' 실행 전 로컬 승인이 필요합니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { pendingPayload = null }) {
                                    Text("취소")
                                }
                                Button(
                                    onClick = {
                                        val approved = intercepted
                                        pendingPayload = null
                                        dispatchAction(approved)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("승인 후 실행")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
