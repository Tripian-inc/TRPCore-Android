package com.tripian.trpcore.util.extensions

import java.io.Serializable

/** Serializable form of a day → ids map, for passing through [android.os.Bundle] / Intent extras. */
typealias IdsByDay = HashMap<String, ArrayList<String>>

fun Map<String, List<String>>.toSerializableIdsByDay(): IdsByDay {
    val result = IdsByDay()
    forEach { (day, ids) -> result[day] = ArrayList(ids) }
    return result
}

/** Reads back a map written by [toSerializableIdsByDay]; empty when the extra is absent. */
@Suppress("UNCHECKED_CAST")
fun Serializable?.asIdsByDay(): Map<String, List<String>> =
    (this as? IdsByDay)?.mapValues { it.value.toList() } ?: emptyMap()

inline fun <T> MutableList<T>.replace(mutator: (T) -> T) {
    val iterate = this.listIterator()
    while (iterate.hasNext()) {
        val oldValue = iterate.next()
        val newValue = mutator(oldValue)
        if (newValue !== oldValue) {
            iterate.set(newValue)
        }
    }
}

inline fun <T> MutableList<T>.remove(mutator: (T) -> Boolean) {
    val iterate = this.listIterator()
    while (iterate.hasNext()) {
        val item = iterate.next()
        if (mutator(item)) {
            iterate.remove()
        }
    }
}