package aleksti21.nbttodata

import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack

data class AttrData(
    val attributeId: String,
    val modifierName: String?,
    val amount: Double,
    val operation: AttributeModifier.Operation,
    val slot: EquipmentSlotGroup
)

data class EnchantData(
    val id: String,
    val level: Int,
)

data class BannerData(
    val pattern: String,
    val color: DyeColor,
)

data class FoodEffectData(
    val id: String,
    val duration: Int,
    val amplifier: Int = 0,
    val chance: Float = 1.0f
)

data class SlotData(
    val slot: Int,
    val item: ItemStack
)