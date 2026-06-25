package aleksti21.nbttodata

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import java.util.UUID

sealed class NbtProperty<B> {
    abstract val key: String
    abstract fun load(builder: B, tag: CompoundTag)
    abstract fun save(builder: B, tag: CompoundTag)
    abstract fun isEmpty(builder: B): Boolean
}

// Свойство-строка (например, Name)
class SimpleProperty<B, V>(
    override val key: String,
    val type: Int,
    val get: (B) -> V?,
    val set: (B, V?) -> Unit,
    val read: (CompoundTag, String) -> V,
    val write: (CompoundTag, String, V) -> Unit
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, type)) set(builder, read(tag, key))
    }
    override fun save(builder: B, tag: CompoundTag) {
        val value = get(builder)
        if (value != null) write(tag, key, value) else tag.remove(key)
    }
    override fun isEmpty(builder: B): Boolean = get(builder) == null
}

class StringListProperty<B>(
    override val key: String,
    val getList: (B) -> MutableList<String>
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, 9)) {
            val listTag = tag.getList(key, 8)
            val target = getList(builder)
            target.clear()
            for (i in 0 until listTag.size) {
                target.add(listTag.getString(i))
            }
        }
    }
    override fun save(builder: B, tag: CompoundTag) {
        val list = getList(builder)
        if (list.isNotEmpty()) {
            val listTag = ListTag()
            list.forEach { listTag.add(StringTag.valueOf(it)) }
            tag.put(key, listTag)
        } else {
            tag.remove(key)
        }
    }
    override fun isEmpty(builder: B): Boolean = getList(builder).isEmpty()
}

class EnchantmentListProperty<B>(
    override val key: String,
    val getList: (B) -> MutableList<EnchantData>
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, 9)) { // 9 — тип ListTag
            val listTag = tag.getList(key, 10) // 10 — тип CompoundTag внутри списка
            val target = getList(builder)
            target.clear()
            for (i in 0 until listTag.size) {
                val enchTag = listTag.getCompound(i)
                target.add(EnchantData(enchTag.getString("id"), enchTag.getInt("lvl")))
            }
        }
    }
    override fun save(builder: B, tag: CompoundTag) {
        val list = getList(builder)
        if (list.isNotEmpty()) {
            val listTag = ListTag()
            list.forEach { data ->
                val enchTag = CompoundTag()
                enchTag.putString("id", data.id)
                enchTag.putInt("lvl", data.level)
                listTag.add(enchTag)
            }
            tag.put(key, listTag)
        } else {
            tag.remove(key)
        }
    }
    override fun isEmpty(builder: B): Boolean = getList(builder).isEmpty()
}

class BannerListProperty<B>(
    override val key: String,
    val getList: (B) -> MutableList<BannerData>
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, 9)) {
            val listTag = tag.getList(key, 10)
            val target = getList(builder)
            target.clear()
            for (i in 0 until listTag.size) {
                val pTag = listTag.getCompound(i)
                val pattern = pTag.getString("Pattern")
                val colorId = pTag.getInt("Color")
                target.add(BannerData(pattern, DyeColor.byId(colorId)))
            }
        }
    }
    override fun save(builder: B, tag: CompoundTag) {
        val list = getList(builder)
        if (list.isNotEmpty()) {
            val listTag = ListTag()
            list.forEach { data ->
                val pTag = CompoundTag()
                pTag.putString("Pattern", data.pattern)
                pTag.putInt("Color", data.color.id)
                listTag.add(pTag)
            }
            tag.put(key, listTag)
        } else {
            tag.remove(key)
        }
    }
    override fun isEmpty(builder: B): Boolean = getList(builder).isEmpty()
}

class AttributeListProperty<B>(
    override val key: String,
    val getList: (B) -> MutableList<AttrData>
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, 9)) {
            val listTag = tag.getList(key, 10)
            val target = getList(builder)
            target.clear()
            for (i in 0 until listTag.size) {
                val modTag = listTag.getCompound(i)

                val attrName = modTag.getString("AttributeName")
                val amount = modTag.getDouble("Amount")

                val opInt = modTag.getInt("Operation")
                val operation = when(opInt) {
                    1 -> AttributeModifier.Operation.MULTIPLY_BASE
                    2 -> AttributeModifier.Operation.MULTIPLY_TOTAL
                    else -> AttributeModifier.Operation.ADDITION
                }

                // Если тега "Slot" нет — значит это "ANY" (null)
                val slot = if (modTag.contains("Slot", 8)) {
                    val slotStr = modTag.getString("Slot")
                    try {
                        EquipmentSlot.byName(slotStr.lowercase())
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val modName = if (modTag.contains("Name", 8)) modTag.getString("Name") else null
                target.add(AttrData(attrName, modName, amount, operation, slot))
            }
        }
    }

    override fun save(builder: B, tag: CompoundTag) {
        val list = getList(builder)
        if (list.isNotEmpty()) {
            val listTag = ListTag()
            list.forEach { data ->
                val modTag = CompoundTag()
                modTag.putString("AttributeName", data.attributeId)
                modTag.putString("Name", data.modifierName ?: "Modifier")
                modTag.putDouble("Amount", data.amount)

                val opInt = when(data.operation) {
                    AttributeModifier.Operation.MULTIPLY_BASE -> 1
                    AttributeModifier.Operation.MULTIPLY_TOTAL -> 2
                    else -> 0
                }
                modTag.putInt("Operation", opInt)

                if (data.slot != null) {
                    modTag.putString("Slot", data.slot.name.lowercase())
                }

                modTag.putUUID("UUID", UUID.randomUUID())
                listTag.add(modTag)
            }
            tag.put(key, listTag)
        } else {
            tag.remove(key)
        }
    }

    override fun isEmpty(builder: B): Boolean = getList(builder).isEmpty()
}

class SkullOwnerProperty<B>(
    override val key: String,
    val getName: (B) -> String?,
    val setName: (B, String?) -> Unit,
    val getTexture: (B) -> String?,
    val setTexture: (B, String?) -> Unit
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, 8)) { // Если это просто строка
            setName(builder, tag.getString(key))
            setTexture(builder, null)
        } else if (tag.contains(key, 10)) { // Если это сложный Compound
            val ownerTag = tag.getCompound(key)
            setName(builder, if (ownerTag.contains("Name", 8)) ownerTag.getString("Name") else null)

            var texture: String? = null
            if (ownerTag.contains("Properties", 10)) {
                val props = ownerTag.getCompound("Properties")
                if (props.contains("textures", 9)) {
                    val texturesList = props.getList("textures", 10)
                    if (texturesList.size > 0) {
                        texture = texturesList.getCompound(0).getString("Value")
                    }
                }
            }
            setTexture(builder, texture)
        }
    }

    override fun save(builder: B, tag: CompoundTag) {
        val name = getName(builder)
        val texture = getTexture(builder)

        if (name != null || texture != null) {
            if (texture != null) {
                val ownerTag = CompoundTag()
                if (name != null) ownerTag.putString("Name", name)
                ownerTag.putUUID("Id", UUID.randomUUID()) // UUID обязателен для текстур!

                val props = CompoundTag()
                val texturesList = ListTag()
                val texTag = CompoundTag()
                texTag.putString("Value", texture)
                texturesList.add(texTag)
                props.put("textures", texturesList)
                ownerTag.put("Properties", props)

                tag.put(key, ownerTag)
            } else {
                tag.putString(key, name!!)
            }
        } else {
            tag.remove(key)
        }
    }

    override fun isEmpty(builder: B): Boolean = getName(builder) == null && getTexture(builder) == null
}

class InventoryListProperty<B>(
    override val key: String,
    val getList: (B) -> MutableList<SlotData>
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, 9)) {
            val listTag = tag.getList(key, 10)
            val target = getList(builder)
            target.clear()
            for (i in 0 until listTag.size) {
                val itemTag = listTag.getCompound(i)
                val slot = itemTag.getByte("Slot").toInt() and 0xFF
                val id = itemTag.getString("id")
                val count = if (itemTag.contains("Count")) itemTag.getByte("Count").toInt() else itemTag.getInt("count")
                val itemStack =
                    ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id)), if (count > 0) count else 1)

                if (itemTag.contains("tag", 10)) {
                    itemStack.tag = itemTag.getCompound("tag")
                }
                target.add(SlotData(slot, itemStack))
            }
        }
    }

    override fun save(builder: B, tag: CompoundTag) {
        val list = getList(builder)
        if (list.isNotEmpty()) {
            val listTag = ListTag()
            list.forEach { data ->
                val itemTag = CompoundTag()
                itemTag.putByte("Slot", data.slot.toByte())
                itemTag.putString("id", BuiltInRegistries.ITEM.getKey(data.item.item).toString())
                itemTag.putByte("Count", data.item.count.toByte())
                if (data.item.hasTag()) {
                    itemTag.put("tag", data.item.tag!!.copy())
                }
                listTag.add(itemTag)
            }
            tag.put(key, listTag)
        } else {
            tag.remove(key)
        }
    }

    override fun isEmpty(builder: B): Boolean = getList(builder).isEmpty()
}

class FoodEffectListProperty<B>(
    override val key: String,
    val getList: (B) -> MutableList<FoodEffectData>
) : NbtProperty<B>() {
    override fun load(builder: B, tag: CompoundTag) {
        if (tag.contains(key, 9)) {
            val listTag = tag.getList(key, 10)
            val target = getList(builder)
            target.clear()
            for (i in 0 until listTag.size) {
                val effTag = listTag.getCompound(i)
                target.add(FoodEffectData(
                    effTag.getString("id"),
                    effTag.getInt("duration"),
                    if (effTag.contains("amplifier")) effTag.getInt("amplifier") else 0,
                    if (effTag.contains("chance")) effTag.getFloat("chance") else 1.0f
                ))
            }
        }
    }
    override fun save(builder: B, tag: CompoundTag) {
        val list = getList(builder)
        if (list.isNotEmpty()) {
            val listTag = ListTag()
            list.forEach { data ->
                val effTag = CompoundTag()
                effTag.putString("id", data.id)
                effTag.putInt("duration", data.duration)
                effTag.putInt("amplifier", data.amplifier)
                effTag.putFloat("chance", data.chance)
                listTag.add(effTag)
            }
            tag.put(key, listTag)
        } else {
            tag.remove(key)
        }
    }
    override fun isEmpty(builder: B): Boolean = getList(builder).isEmpty()
}