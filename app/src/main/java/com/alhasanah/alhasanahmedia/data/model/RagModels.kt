package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PublicRagRequest(
    val source: String,
    val query: String,
    @SerialName("session_id")
    val sessionId: String? = null
)

@Serializable
data class PublicRagResponse(
    val answer: String = "",
    val sources: List<RagSource> = emptyList(),
    @SerialName("has_relevant_context")
    val hasRelevantContext: Boolean = true,
    @SerialName("remaining_requests")
    val remainingRequests: Int? = null
)

@Serializable
data class WaliRagRequest(
    val query: String,
    @SerialName("child_ref")
    val childRef: String? = null,
    @SerialName("include_public_knowledge")
    val includePublicKnowledge: Boolean = true
)

@Serializable
data class WaliRagResponse(
    val answer: String = "",
    val children: List<WaliChild> = emptyList(),
    @SerialName("selected_child_ref")
    val selectedChildRef: String? = null,
    val sources: List<RagSource> = emptyList(),
    @SerialName("remaining_requests")
    val remainingRequests: Int? = null
)

@Serializable
data class WaliChild(
    @SerialName("child_ref")
    val childRef: String,
    val nama: String? = null,
    val kelas: String? = null,
    val jurusan: String? = null,
    @SerialName("status_santri")
    val statusSantri: String? = null
)

@Serializable
data class RagSource(
    val title: String = "",
    val metadata: JsonObject? = null,
    val similarity: Double? = null
)

@Serializable
data class RagErrorResponse(
    val error: String? = null
)
