package com.anand.prohands.data

import com.google.gson.annotations.SerializedName

data class JobRequest(
    @SerializedName("providerId") val providerId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("wage") val wage: Double? = null,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("requiredSkills") val requiredSkills: List<String>? = null,
    @SerializedName("numberOfEmployees") val numberOfEmployees: Int? = null
)

data class JobResponse(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("providerId") val providerId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("wage") val wage: Double?,
    @SerializedName("status") val status: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("requiredSkills") val requiredSkills: List<String>?,
    @SerializedName("numberOfEmployees") val numberOfEmployees: Int?,
    @SerializedName("createdAt") val createdAt: String
)
