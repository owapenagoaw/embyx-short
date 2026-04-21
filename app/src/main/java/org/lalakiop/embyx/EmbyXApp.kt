package com.lalakiop.embyx

import android.app.Application

class EmbyXApp : Application() {
	lateinit var appContainer: AppContainer
		private set

	override fun onCreate() {
		super.onCreate()
		appContainer = AppContainer(this)
	}
}
