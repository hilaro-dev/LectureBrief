package com.lecturebrief.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {

    @Serializable
    data object Library : AppDestination

    @Serializable
    data object RecordSession : AppDestination

    @Serializable
    data class RecordingDetail(val recordingId: Long) : AppDestination

    @Serializable
    data object Settings : AppDestination
}
