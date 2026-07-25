package com.alhasanah.alhasanahmedia.showcase.domain


interface ShowcaseEventListener {
    fun onEvent(level: Level, event: String)
}
