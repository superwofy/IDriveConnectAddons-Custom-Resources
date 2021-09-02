package me.hufman.idriveconnectaddons.bimmerscrobbler

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Observer
import me.hufman.idriveconnectaddons.lib.CDSLiveData
import me.hufman.idriveconnectaddons.lib.CDSProperty
import com.google.gson.JsonObject
import me.hufman.idriveconnectaddons.bimmerscrobbler.MainService.Companion.TAG
import me.hufman.idriveconnectaddons.lib.GsonNullable.tryAsInt
import me.hufman.idriveconnectaddons.lib.GsonNullable.tryAsJsonObject
import me.hufman.idriveconnectaddons.lib.GsonNullable.tryAsJsonPrimitive
import me.hufman.idriveconnectaddons.lib.GsonNullable.tryAsString

/**
 * Needs a service intent to be discoverable in the Addons UI
 */
class MainService: Service() {
	companion object {
		const val TAG = "BimmerScrobbler"
	}

	val scrobbleAnnouncer by lazy {ScrobbleAnnouncer(this.applicationContext)}
	val multimedia = CDSLiveData(this, CDSProperty.ENTERTAINMENT_MULTIMEDIA)
	val multimediaObserver by lazy { MultimediaObserver(scrobbleAnnouncer) }

	override fun onCreate() {
		super.onCreate()
		Log.i(TAG, "Starting service")
		MainModel.isConnected.value = true
		multimedia.observeForever(multimediaObserver)
	}

	override fun onBind(p0: Intent?): IBinder? {
		Log.i(TAG, "Binding service")
		return null
	}

	override fun onUnbind(intent: Intent?): Boolean {
		Log.i(TAG, "Unbinding service")
		return super.onUnbind(intent)
	}

	override fun onDestroy() {
		super.onDestroy()
		Log.i(TAG, "Destroying service")
		MainModel.isConnected.value = false
		multimedia.removeObserver(multimediaObserver)
	}
}

class MultimediaObserver(val announcer: ScrobbleAnnouncer): Observer<JsonObject> {
	override fun onChanged(t: JsonObject?) {
		val sourceId = t?.tryAsJsonObject("multimedia")?.tryAsJsonPrimitive("source")?.tryAsInt ?: 0
		val artist = t?.tryAsJsonObject("multimedia")?.tryAsJsonPrimitive("artist")?.tryAsString ?: ""
		val album = t?.tryAsJsonObject("multimedia")?.tryAsJsonPrimitive("album")?.tryAsString ?: ""
		val title = t?.tryAsJsonObject("multimedia")?.tryAsJsonPrimitive("title")?.tryAsString ?: ""
		MainModel.source.value = if (sourceId > 0) sourceId.toString() else ""
		MainModel.artist.value = artist
		MainModel.album.value = album
		MainModel.title.value = title
		announcer.announce(ScrobblerMetadata(artist, album, title, sourceId))
	}

}