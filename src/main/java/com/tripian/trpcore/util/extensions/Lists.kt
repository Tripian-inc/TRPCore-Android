package com.tripian.trpcore.util.extensions

/**
 * Created by semihozkoroglu on 2019-08-15.
 */
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