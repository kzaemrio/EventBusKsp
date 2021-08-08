package com.kz.eventbusksp

import org.greenrobot.eventbus.Subscribe

class SimpleSubscriber {
    @Subscribe
    fun onEvent(event: String) {

    }

    public class Inner {
        @Subscribe
        fun onEvent(event: String) {

        }
    }
}