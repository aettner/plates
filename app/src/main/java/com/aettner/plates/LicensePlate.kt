package com.aettner.plates

import kotlinx.serialization.Serializable

@Serializable
data class LicensePlate(
    val code: String,
    val city: String,
    val state: String
)