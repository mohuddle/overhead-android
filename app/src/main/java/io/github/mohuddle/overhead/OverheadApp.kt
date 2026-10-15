package io.github.mohuddle.overhead

import android.app.Application
import io.github.mohuddle.overhead.data.OverheadStore

class OverheadApp : Application() {
    lateinit var store: OverheadStore
        private set

    override fun onCreate() {
        super.onCreate()
        store = OverheadStore.create(this)
    }
}
