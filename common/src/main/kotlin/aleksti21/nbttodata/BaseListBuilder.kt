package aleksti21.nbttodata

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack

abstract class BaseListBuilder<T>(private val idSelector: ((T) -> String)? = null) {
    val list = mutableListOf<T>()

    fun clear() {
        list.clear()
    }
    protected fun add(item: T) {
        if (idSelector != null) {
            val itemId = idSelector.invoke(item)
            list.removeIf { idSelector.invoke(it) == itemId }
        }
        list.add(item)
    }

    fun remove(id: String) {
        if (idSelector != null) {
            list.removeIf { idSelector.invoke(it) == id }
        } else {
            list.removeIf { it.toString() == id }
        }
    }

    // Удаление по строковому ID: -"minecraft:sharpness" или -"Страница"
    operator fun String.not() {
        remove(this)
    }

    operator fun T.not() {
        list.remove(this)
    }

    open operator fun Int.not() {
        list.removeAt(this)
    }
}

class AttributesBuilder: BaseListBuilder<AttrData>({it.attributeId}) {
    operator fun String.invoke(
        amount: Double,
        operation: AttributeModifier.Operation = AttributeModifier.Operation.ADDITION,
        slot: EquipmentSlot? = null,
        modifierName: String? = null
    ) {
        add(AttrData(this, modifierName, amount, operation, slot))
    }
}

class EnchantmentBuilder : BaseListBuilder<EnchantData>({ it.id }) {
    operator fun String.invoke(level: Int) {
        add(EnchantData(this, level))
    }
}

class BannerBuilder: BaseListBuilder<BannerData>({it.pattern}) {
    operator fun String.invoke(color: DyeColor) {
        add(BannerData(this, color))
    }
}

class BookBuilder: BaseListBuilder<String>({it}) {
    var title: String? = null
    var author: String? = null
    var generation: Int? = null
    var resolved: Boolean? = null

    operator fun String.unaryPlus() {
        add(this)
    }

    val pages: List<String> get() = list
}

class DisplayBuilder : BaseListBuilder<String>() {
    var nameJson: String? = null
    var color: Int? = null

    fun setName(text: String, color: String = "white", italic: Boolean = false) {
        nameJson = """{"text": "$text", "color": "$color", "italic": $italic}"""
    }

    operator fun String.unaryPlus() {
        add("""{"text": "$this"}""")
    }
    val loreJsons: MutableList<String> get() = list
}

class InventoryBuilder : BaseListBuilder<SlotData>({ it.slot.toString() }) {
    operator fun Int.invoke(itemStack: ItemStack) {
        add(SlotData(this, itemStack))
    }

    override fun Int.not() {
        remove(this.toString())
    }
}