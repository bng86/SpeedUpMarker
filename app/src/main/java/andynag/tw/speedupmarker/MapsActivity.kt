package andynag.tw.speedupmarker

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.LruCache
import andynag.tw.speedupmarker.model.SpeedUpLevel
import andynag.tw.speedupmarker.model.YouBikeData
import andynag.tw.speedupmarker.model.YouBikeStation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.concurrent.TimeUnit

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var youBikeStations: List<YouBikeStation>
    private lateinit var markerFactory: MarkerFactory
    private var speedUpLevel: SpeedUpLevel = SpeedUpLevel.None
    private val markerCache = LruCache<YouBikeStation, Marker>(400)
    private val compositeDisposable = CompositeDisposable()

    companion object {
        const val TAG = "MapsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        youBikeStations = getYouBikeStations()

        showLevelText()
        textTimestamp.text = "init"
        textMarkerSize.text = "Data size: ${youBikeStations.size}"
        markerFactory = MarkerFactory(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        buttonLevel0.setOnClickListener {
            speedUpLevel = SpeedUpLevel.Level0
            showLevelText()
        }
        buttonLevel1.setOnClickListener {
            speedUpLevel = SpeedUpLevel.Level1
            showLevelText()
        }
        buttonLevel2.setOnClickListener {
            speedUpLevel = SpeedUpLevel.Level2
            showLevelText()
        }
        buttonLevel3.setOnClickListener {
            speedUpLevel = SpeedUpLevel.Level3
            showLevelText()
        }
        buttonClearMarker.setOnClickListener {
            googleMap.clear()
        }
        buttonClearCache.setOnClickListener {
            markerCache.evictAll()
        }

        googleMap.setOnCameraIdleListener {
            val start = System.currentTimeMillis()
            when (speedUpLevel) {
                SpeedUpLevel.Level0 -> level0()
                SpeedUpLevel.Level1 -> level1()
                SpeedUpLevel.Level2 -> level2()
                SpeedUpLevel.Level3 -> level3()
            }
            showTimestampText(start)
        }

        this.googleMap = googleMap
        this.googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(25.0408578889, 121.567904444), 11.5f
            )
        )
    }

    private fun showLevelText() {
        textLevel.text = speedUpLevel.displayName
    }

    private fun showTimestampText(start: Long) {
        textTimestamp.text = "${System.currentTimeMillis() - start} ms"
    }

    private fun level0() {
        googleMap.clear()
        youBikeStations
            .forEach {
                val markerOptions = MarkerOptions()
                    .position(LatLng(it.lat, it.lng))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker))
                this.googleMap.addMarker(markerOptions)
            }
    }

    private fun level1(useCache: Boolean = false) {
        googleMap.clear()
        markerFactory.useCache = useCache
        youBikeStations
            .forEach {
                val bitmapDescriptor = BitmapDescriptorFactory
                    .fromBitmap(markerFactory.getMarkerBitmap(CustomMarker(it.id, it.availableBike)))
                val markerOptions = MarkerOptions()
                    .position(LatLng(it.lat, it.lng))
                    .icon(bitmapDescriptor)
                this.googleMap.addMarker(markerOptions)
            }
    }

    private fun level2() {
        level1(useCache = true)
    }

    private fun level3() {
        markerFactory.useCache = true
        compositeDisposable.addAll(
            rxDelayData(youBikeStations)
                .flatMap { youBikeStation -> rxCheckCache(youBikeStation) }
                .flatMap { youBikeStation -> rxBitmap(youBikeStation) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (youBikeStations, markerOptions) ->
                    val start = System.currentTimeMillis()
                    val marker = this.googleMap.addMarker(markerOptions)
                    markerCache.put(youBikeStations, marker)
                    showTimestampText(start)
                }
        )
    }

    private fun rxDelayData(youBikeStations: List<YouBikeStation>): Observable<YouBikeStation> {
        return Observable.create { emitter ->
            Observable.fromIterable(youBikeStations)
                .concatMap { Observable.just(it).delay(2, TimeUnit.MILLISECONDS) }
                .subscribe { emitter.onNext(it) }
        }
    }

    private fun rxCheckCache(youBikeStation: YouBikeStation): Observable<YouBikeStation> {
        return Observable.create<YouBikeStation> { emitter ->
            if (markerCache[youBikeStation] == null) {
                Log.e(TAG, "miss marker cache")
                emitter.onNext(youBikeStation)
            } else {
                Log.e(TAG, "hit marker cache")
            }
        }
    }

    private fun rxBitmap(youBikeStation: YouBikeStation): Observable<Pair<YouBikeStation, MarkerOptions>> {
        return Observable.create<Pair<YouBikeStation, MarkerOptions>> { emitter ->
            val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                markerFactory.getMarkerBitmap(
                    CustomMarker(youBikeStation.id, youBikeStation.availableBike)
                )
            )
            val markerOptions = MarkerOptions()
                .position(LatLng(youBikeStation.lat, youBikeStation.lng))
                .icon(bitmapDescriptor)
            emitter.onNext(Pair(youBikeStation, markerOptions))
        }
    }

    private fun getYouBikeStations(): List<YouBikeStation> {
        val rawData = applicationContext.assets.open("data.json").bufferedReader().use { it.readText() }
        val youBikeData = Gson().fromJson(rawData, YouBikeData::class.java)
        return youBikeData.retVal.map { it.value }.toList()
    }
}
