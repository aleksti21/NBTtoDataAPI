package aleksti21.nbttodata

import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.TagParser
import net.minecraft.world.item.ItemStack

/**
 * Applies legacy SNBT (String NBT) data from older Minecraft versions
 * directly to this ItemStack, converting and restructuring it into
 * modern Data Components (on 1.21+) or merging it into the item's tag (on 1.20.x).
 *
 * @param snbt The raw string-serialized NBT data to apply.
 * @param registries The current game RegistryAccess to resolve dynamic registry references (such as enchantments, attributes, etc.).
 */
fun ItemStack.applySNBT(snbt: String, registries: RegistryAccess? = null) {
    try {
        val parsedTag = TagParser.parseTag(snbt)
        this.getOrCreateTag().merge(parsedTag)
    } catch (e: Exception) {
        println("[NBT to Data API] Error: ${e.message}")
    }
}
/**
 * A powerful Type-Safe Kotlin DSL builder for editing item components.
 * Hides the complexity of 1.21 Data Components behind a clean and intuitive syntax.
 *
 * @param registries Access to server registries.
 * @param block The builder block where you can define item properties.
 * @return The modified ItemStack
 */
fun ItemStack.editData(registries: RegistryAccess? = null, block: ItemDataBuilder.() -> Unit): ItemStack {
    ItemDataBuilder(this, registries).block()
    return this
}