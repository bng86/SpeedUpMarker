package andynag.tw.speedupmarker.model

sealed class SpeedUpLevel(val displayName: String) {
    object None : SpeedUpLevel("None")
    object Level0 : SpeedUpLevel("Level0")
    object Level1 : SpeedUpLevel("Level1")
    object Level2 : SpeedUpLevel("Level2")
    object Level3 : SpeedUpLevel("Level3")
}