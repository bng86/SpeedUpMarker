package andynag.tw.speedupmarker

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import andynag.tw.speedupmarker.model.YouBikeData
import andynag.tw.speedupmarker.model.YouBikeStation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var youBikeStations: List<YouBikeStation>
    private lateinit var markerFactory: MarkerFactory

    companion object {
        const val MARKER_SIZE = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        markerFactory = MarkerFactory(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        youBikeStations = getYouBikeStations()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        buttonLevel0.setOnClickListener { level0() }
        buttonLevel1.setOnClickListener { level1() }

        this.googleMap = googleMap
        this.googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(25.0408578889, 121.567904444), 11.5f
            )
        )
    }

    private fun level0() {
        googleMap.clear()
        youBikeStations.take(MARKER_SIZE)
            .forEach {
                val markerOptions = MarkerOptions().position(LatLng(it.lat, it.lng))
                this.googleMap.addMarker(markerOptions)
            }
    }

    private fun level1() {
        googleMap.clear()
        youBikeStations.take(MARKER_SIZE)
            .forEach {
                val bitmapDescriptor = BitmapDescriptorFactory
                    .fromBitmap(markerFactory.createBitmap(CustomMarker(it.id, it.availableBike)))
                val markerOptions = MarkerOptions()
                    .position(LatLng(it.lat, it.lng))
                    .icon(bitmapDescriptor)
                this.googleMap.addMarker(markerOptions)
            }
    }

    private fun getYouBikeStations(): List<YouBikeStation> {
        val rawData = applicationContext.assets.open("data.json").bufferedReader().use { it.readText() }
        val youBikeData = Gson().fromJson(rawData, YouBikeData::class.java)
        return youBikeData.retVal.map { it.value }.toList()
    }
}
