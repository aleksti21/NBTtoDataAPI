package aleksti21.nbttodata

class FoodBuilder {
    var nutrition: Int = 1
    var saturation: Float = 0.0f
    var canAlwaysEat: Boolean = false
    var eatSeconds: Float = 1.6f
    var convertsTo: String? = null
    val effects = mutableListOf<FoodEffectData>()
}


class PotionBuilder {
    var id: String? = null
    var customColor: Int? = null
}

class SkullBuilder {
    var name: String? = null
    var textureBase64: String? = null
}