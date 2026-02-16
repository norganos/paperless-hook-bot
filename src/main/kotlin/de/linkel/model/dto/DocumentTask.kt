package de.linkel.model.dto

interface DocumentTask {
    val instance: String
    val attempt: Int

    fun retryCopy(): DocumentTask
}
