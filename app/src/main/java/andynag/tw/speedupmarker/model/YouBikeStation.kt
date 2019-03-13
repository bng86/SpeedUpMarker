package andynag.tw.speedupmarker.model

import com.google.gson.annotations.SerializedName

data class YouBikeStation(
    @SerializedName("sno")
    val id: String,
    @SerializedName("sbi")
    val availableBike: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

data class YouBikeData(
    @SerializedName("retVal")
    val retVal: Map<String, YouBikeStation>
)