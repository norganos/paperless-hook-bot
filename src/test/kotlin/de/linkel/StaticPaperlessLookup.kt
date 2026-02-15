package de.linkel.de.linkel

import de.linkel.model.paperless.BaseObj
import de.linkel.service.PaperlessLookup

class StaticPaperlessLookup<T: BaseObj>(
    elements: Iterable<T>
): PaperlessLookup<T> {
    private val byId = mutableMapOf<Long, T>()
    private val byNameOrSlug = mutableMapOf<String, T>()

    init {
        elements.forEach {
            byId[it.id] = it
            byNameOrSlug[it.slug] = it
            byNameOrSlug[it.name] = it
        }
    }

    override fun resolve(value: String): T? {
        return byNameOrSlug[value]
    }

    override fun get(id: Long): T? {
        return byId[id]
    }
}