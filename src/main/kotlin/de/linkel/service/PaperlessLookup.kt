package de.linkel.service

import de.linkel.model.paperless.BaseObj

interface PaperlessLookup<T : BaseObj> {
    fun resolve(value: String): T?
    fun get(id: Long): T?
}