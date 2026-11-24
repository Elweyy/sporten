package fr.loic.seances

import android.app.Application
import fr.loic.seances.data.Library
import fr.loic.seances.data.SettingsStore
import fr.loic.seances.update.Updater

class App : Application() {
    val settings by lazy { SettingsStore(this) }
    val library by lazy { Library(this, settings.durations) }
    val updater by lazy { Updater(this) }
}
