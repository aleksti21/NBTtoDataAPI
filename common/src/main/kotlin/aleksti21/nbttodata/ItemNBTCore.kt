package aleksti21.nbttodata

import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.TagParser
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

/**
 * Applies legacy SNBT (String NBT) data from older Minecraft versions
 * directly to this ItemStack, converting and restructuring it into
 * modern Data Components (on 1.21+) or merging it into the item's tag (on 1.20.x).
 *
 * @param snbt The raw string-serialized NBT data to apply.
 * @param registries The current game RegistryAccess to resolve dynamic registry references (such as enchantments, attributes, etc.).
 */
fun ItemStack.applySNBT(snbt: String, registries: RegistryAccess): ItemStack {
    try {
        val tag = TagParser.parseTag(snbt)
        NbtMappers.RULES.forEach { it.applyIfPresent(tag, this, registries) }
        if (!tag.isEmpty) {
            val currentCustomData = this.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            val tagToModify = currentCustomData.copyTag()
            tagToModify.merge(tag)
            this.set(DataComponents.CUSTOM_DATA, CustomData.of(tagToModify))
        }
    } catch (e: Exception) {
        println("❌ Ошибка парсинга SNBT: ${e.message}")
    }
    return this
}
/**
 * A powerful Type-Safe Kotlin DSL builder for editing item components.
 * Hides the complexity of 1.21 Data Components behind a clean and intuitive syntax.
 *
 * @param registries Access to server registries.
 * @param block The builder block where you can define item properties.
 * @return The modified ItemStack
 */
fun ItemStack.editData(registries: RegistryAccess, block: ItemDataBuilder.() -> Unit): ItemStack {
    ItemDataBuilder(this, registries).block()
    return this
}