package com.tasteindia

import android.app.Application
import com.tasteindia.data.*

class TasteIndiaApplication : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val repository: MealsRepository = DefaultMealsRepository(
        service = createMealService(),
        store = JsonFileStore(app.filesDir),
        assets = app.assets,
    )
}
