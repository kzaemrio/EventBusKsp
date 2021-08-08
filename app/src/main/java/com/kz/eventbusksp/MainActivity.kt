package com.kz.eventbusksp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.EventBus.builder as eventBusBuilder
import org.greenrobot.eventbus.EventBus.getDefault as eventBus

class MainActivity : ComponentActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        eventBusBuilder().addIndex(EventBusIndexKt()).installDefaultEventBus()

        findViewById<View>(R.id.bt).setOnClickListener {
            eventBus().post("click at ${System.currentTimeMillis()}")
        }
    }

    override fun onResume() {
        super.onResume()
        eventBus().register(this)
    }

    override fun onPause() {
        super.onPause()
        eventBus().unregister(this)
    }

    @SinceKotlin(version = "1.4.10")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: String) {
        findViewById<TextView>(R.id.text).append("$event\n")
    }

    @Subscribe(sticky = true)
    fun onEvent(event: TextView) {
        findViewById<TextView>(R.id.text).append("$event\n")
    }

    @Subscribe
    fun onEvent(event: SimpleEvent) {
        findViewById<TextView>(R.id.text).append("$event\n")
    }
}

object SimpleEvent