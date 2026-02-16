package de.linkel.service

import de.linkel.model.api.DocumentQuery
import de.linkel.model.dto.DocumentTask
import de.linkel.model.dto.MultiDocumentTask
import de.linkel.model.dto.SingleDocumentTask
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory

class QueueService(
    private val paperlessDispatcherService: PaperlessRegistryService
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaperlessInstanceService::class.java)
    }

    private val queue = Channel<DocumentTask>(256)

    suspend fun loop() {
        queue.consumeEach { task ->
            log.info("processing task $task")
            paperlessDispatcherService.getInstanceByName(task.instance)
                .also {
                    if (it == null) log.warn("Could not find paperless instance ${task.instance}")
                }
                ?.also { service ->
                    try {
                        when (task) {
                            is SingleDocumentTask -> service.process(task.documentId)
                            is MultiDocumentTask -> service.query(task.query)
                                .onEach { schedule(service, it) }
                        }
                    } catch (e: Exception) {
                        log.error("Error processing task $task", e)
                        if (task.attempt < 5) {
                            queue.send(task.retryCopy())
                        }
                    }
                }
        }
    }

    suspend fun schedule(instance: PaperlessInstanceService, id: Long) {
        schedule(SingleDocumentTask(instance.name, id))
    }

    suspend fun schedule(instance: PaperlessInstanceService, query: DocumentQuery) {
        schedule(MultiDocumentTask(instance.name, query))
    }

    suspend fun schedule(task: DocumentTask) {
        log.info("Scheduling task $task")
        queue.send(task)
    }
}