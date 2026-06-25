package aleksti21.nbttodata

import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponentType
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

class NbtRule(
    val nbtKey: String,
    val action: (CompoundTag, ItemStack, RegistryAccess) -> Unit
) {
    fun applyIfPresent(tag: CompoundTag, item: ItemStack, registries: RegistryAccess) {
        if (tag.contains(nbtKey)) {
            try { action(tag, item, registries) }
            catch (e: Exception) { println("'$nbtKey': ${e.message}") }
            finally { tag.remove(nbtKey) }
        }
    }

    companion object {
        fun int(key: String, component: DataComponentType<Int>) =
            NbtRule(key) { tag, item, _ -> item.set(component, tag.getInt(key)) }

        fun <T> int(key: String, component: DataComponentType<T>, transform: (Int) -> T) =
            NbtRule(key) { tag, item, _ -> item.set(component, transform(tag.getInt(key))) }

        fun <T> boolean(key: String, component: DataComponentType<T>, transform: (Boolean) -> T) =
            NbtRule(key) { tag, item, _ -> item.set(component, transform(tag.getBoolean(key))) }
    }
}