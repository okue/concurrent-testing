package io.grpc.override

import io.grpc.Context

// Copy of ThreadLocalContextStorage.java
class ContextStorageOverride : Context.Storage() {
    override fun doAttach(toAttach: Context): Context {
        val current = current()
        localContext.set(toAttach)
        return current
    }

    override fun detach(toDetach: Context, toRestore: Context) {
        if (toRestore !== Context.ROOT) {
            localContext.set(toRestore)
        } else {
            localContext.set(null)
        }
    }

    override fun current(): Context {
        return localContext.get() ?: Context.ROOT
    }

    companion object {
        val localContext = ThreadLocal<Context?>()
    }
}

