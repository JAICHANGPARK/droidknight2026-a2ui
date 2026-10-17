package com.droidknights.a2ui.common

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * DroidKnights 2026 — A2UI Spec 1.0 (v1.0) to AndroidX A2UI (v0.9.1 Engine) Bridge Adapter.
 *
 * ## Architecture Rationale
 * - **The Protocol (Open Source A2UI)** is standardizing on **v1.0 Candidate** (`@path`, `@call`, inline `createSurface`, multi-catalog mixing).
 * - **The Android Library (`androidx.a2ui`)** currently implements **v0.9.1** (`SUPPORTED_VERSIONS = ["v0.9", "v0.9.1"]`).
 *
 * Instead of discarding official `androidx.a2ui` to write a redundant custom engine from scratch,
 * this Bridge operates as a lightweight client-side desugaring layer:
 *
 * 1. **Version Normalization**: Maps `"version": "v1.0"` to `"v0.9.1"` so the AndroidX engine accepts it.
 * 2. **Catalog ID Aliasing**: Resolves `v1_0` Basic Catalog URI (`.../specification/v1_0/catalogs/basic/catalog.json`) to `A2uiBasicCatalogV1.CatalogId` (`.../v0_9/...`).
 * 3. **Directive Desugaring**: Maps Spec 1.0 reserved prefixes (`"@path"` -> `"path"`, `"@call"` -> `"call"`, `"@@..."` -> `"@..."`).
 * 4. **Inline Surface Decomposition**: Decomposes a single Spec 1.0 inline `createSurface { components, dataModel }` message
 *    into the 3 sequential messages (`createSurface` -> `updateDataModel` -> `updateComponents`) expected by AndroidX A2UI.
 *
 * Once Google updates `androidx.a2ui` to natively support Spec 1.0 in a future alpha/beta,
 * this adapter can be cleanly bypassed or removed without rewriting any agent payloads or UI components.
 */
object A2uiSpec10Bridge {

    const val SPEC_1_0_VERSION = "v1.0"
    const val ANDROIDX_TARGET_VERSION = "v0.9.1"

    const val SPEC_1_0_BASIC_CATALOG_ID =
        "https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Transforms a single Spec 1.0 (or v0.9.1) JSONL line into one or more AndroidX-compatible JSONL lines.
     */
    fun transform(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return listOf(trimmed)

        val rootElement = try {
            json.parseToJsonElement(trimmed).jsonObject
        } catch (_: Exception) {
            return listOf(trimmed)
        }

        // Check if message is Spec 1.0
        val version = rootElement["version"]?.jsonPrimitive?.content
        if (version != SPEC_1_0_VERSION) {
            // Passthrough for already v0.9 / v0.9.1 messages
            return listOf(trimmed)
        }

        // Case 1: createSurface with potential inline components / dataModel
        val createSurfaceObj = rootElement["createSurface"]?.jsonObject
        if (createSurfaceObj != null) {
            return decomposeCreateSurface(createSurfaceObj)
        }

        // Case 2: updateComponents, updateDataModel, deleteSurface, etc.
        val desugaredRoot = buildJsonObject {
            put("version", JsonPrimitive(ANDROIDX_TARGET_VERSION))
            for ((k, v) in rootElement) {
                if (k != "version") {
                    put(k, desugarElement(v))
                }
            }
        }
        return listOf(desugaredRoot.toString())
    }

    /**
     * Decomposes Spec 1.0 inline createSurface:
     * 1. createSurface (surfaceId, catalogId, sendDataModel)
     * 2. updateDataModel (if dataModel present)
     * 3. updateComponents (if components present)
     */
    private fun decomposeCreateSurface(createSurfaceObj: JsonObject): List<String> {
        val surfaceId = createSurfaceObj["surfaceId"]?.jsonPrimitive?.content
            ?: return listOf(createSurfaceObj.toString())

        val rawCatalogId = createSurfaceObj["catalogId"]?.jsonPrimitive?.content
        val catalogId = when (rawCatalogId) {
            null, SPEC_1_0_BASIC_CATALOG_ID -> A2uiBasicCatalogV1.CatalogId
            else -> rawCatalogId
        }

        val sendDataModel = createSurfaceObj["sendDataModel"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
        val inlineDataModel = createSurfaceObj["dataModel"]?.jsonObject
        val inlineComponents = createSurfaceObj["components"]?.jsonArray

        val result = mutableListOf<String>()

        // 1. AndroidX createSurface message
        val baseCreateSurface = buildJsonObject {
            put("version", JsonPrimitive(ANDROIDX_TARGET_VERSION))
            put("createSurface", buildJsonObject {
                put("surfaceId", JsonPrimitive(surfaceId))
                put("catalogId", JsonPrimitive(catalogId))
                put("sendDataModel", JsonPrimitive(sendDataModel))
            })
        }
        result.add(baseCreateSurface.toString())

        // 2. updateDataModel (if present in inline createSurface)
        if (inlineDataModel != null && inlineDataModel.isNotEmpty()) {
            val updateDataMsg = buildJsonObject {
                put("version", JsonPrimitive(ANDROIDX_TARGET_VERSION))
                put("updateDataModel", buildJsonObject {
                    put("surfaceId", JsonPrimitive(surfaceId))
                    put("path", JsonPrimitive("/"))
                    put("value", desugarElement(inlineDataModel))
                })
            }
            result.add(updateDataMsg.toString())
        }

        // 3. updateComponents (if present in inline createSurface)
        if (inlineComponents != null && inlineComponents.isNotEmpty()) {
            val updateCompMsg = buildJsonObject {
                put("version", JsonPrimitive(ANDROIDX_TARGET_VERSION))
                put("updateComponents", buildJsonObject {
                    put("surfaceId", JsonPrimitive(surfaceId))
                    put("components", desugarElement(inlineComponents))
                })
            }
            result.add(updateCompMsg.toString())
        }

        return result
    }

    /**
     * Recursively desugars JSON tree:
     * - Keys: `@path` -> `path`, `@call` -> `call`, `@@...` -> `@...`
     * - Values: maps `SPEC_1_0_BASIC_CATALOG_ID` to `A2uiBasicCatalogV1.CatalogId`
     */
    fun desugarElement(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> {
                buildJsonObject {
                    for ((key, value) in element) {
                        val targetKey = when {
                            key == "@path" -> "path"
                            key == "@call" -> "call"
                            key.startsWith("@@") -> key.substring(1) // Unescape @@ -> @
                            else -> key
                        }
                        put(targetKey, desugarElement(value))
                    }
                }
            }
            is JsonArray -> {
                JsonArray(element.map { desugarElement(it) })
            }
            is JsonPrimitive -> {
                if (element.isString && element.content == SPEC_1_0_BASIC_CATALOG_ID) {
                    JsonPrimitive(A2uiBasicCatalogV1.CatalogId)
                } else {
                    element
                }
            }
        }
    }
}
