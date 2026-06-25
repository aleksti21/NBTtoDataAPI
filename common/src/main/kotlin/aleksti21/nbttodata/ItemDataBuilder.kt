package aleksti21.nbttodata

import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items


class ItemDataBuilder(private val item: ItemStack, private val registries: RegistryAccess?) {
    private inline fun <B> updateNbtProperties(
        builder: B,
        nbtKey: String,
        properties: List<NbtProperty<B>>,
        block: B.() -> Unit
    ) {
        val parentTag = if (nbtKey.isEmpty()) item.getOrCreateTag() else item.getOrCreateTagElement(nbtKey)

        properties.forEach { it.load(builder, parentTag) }
        builder.block()
        properties.forEach { it.save(builder, parentTag) }

        val allEmpty = properties.all { it.isEmpty(builder) }
        if (allEmpty && nbtKey.isNotEmpty()) {
            item.tag?.remove(nbtKey)
        }
    }


    private fun setHideFlag(bit: Int, hide: Boolean) {
        val tag = item.getOrCreateTag()
        val currentFlags = tag.getInt("HideFlags")
        val newFlags = if (hide) currentFlags or bit else currentFlags and bit.inv()

        if (newFlags == 0) tag.remove("HideFlags")
        else tag.putInt("HideFlags", newFlags)
    }

    private fun getHideFlag(bit: Int): Boolean {
        val tag = item.tag ?: return false
        return (tag.getInt("HideFlags") and bit) != 0
    }

    var hideEnchantments: Boolean
        get() = getHideFlag(1)
        set(value) = setHideFlag(1, value)

    var hideAttributes: Boolean
        get() = getHideFlag(2)
        set(value) = setHideFlag(2, value)

    var hideUnbreakable: Boolean
        get() = getHideFlag(4)
        set(value) = setHideFlag(4, value)

    var hideAdditionalTooltip: Boolean
        get() = getHideFlag(32)
        set(value) = setHideFlag(32, value)

    var customModelData: Int
        get() = item.tag?.getInt("CustomModelData") ?: 0
        set(value) {
            if (value == 0) item.tag?.remove("CustomModelData")
            else item.getOrCreateTag().putInt("CustomModelData", value)
        }

    fun customData(block: CompoundTag.() -> Unit) {
        val tag = item.getOrCreateTag()
        tag.block() // Пишем кастомные данные прямо в корень NBT предмета!
    }

    fun blockEntityData(block: CompoundTag.() -> Unit) {
        val tag = item.getOrCreateTagElement("BlockEntityTag")
        tag.block() // Пишем данные блока в классический BlockEntityTag
    }


    fun display(block: DisplayBuilder.() -> Unit) {
        updateNbtProperties(
            builder = DisplayBuilder(),
            nbtKey = "display",
            properties = listOf(
                SimpleProperty("Name", 8, { b -> b.nameJson }, { b, v -> b.nameJson = v }, CompoundTag::getString, CompoundTag::putString),
                SimpleProperty("color", 99, { b -> b.color }, { b, v -> b.color = v }, CompoundTag::getInt, CompoundTag::putInt),
                StringListProperty("Lore", { b -> b.list })
            ),
            block = block
        )
    }

    var damage: Int
        get() = item.damageValue
        set(value) { item.damageValue = value }

    var unbreakable: Boolean
        get() = item.tag?.getBoolean("Unbreakable") ?: false
        set(value) {
            if (value) item.getOrCreateTag().putBoolean("Unbreakable", true)
            else item.tag?.remove("Unbreakable")
        }

    var mapId: Int?
        get() = if (item.tag?.contains("map", 99) == true) item.tag!!.getInt("map") else null
        set(value) {
            if (value != null) item.getOrCreateTag().putInt("map", value)
            else item.tag?.remove("map")
        }

    fun potion(block: PotionBuilder.() -> Unit) {
        updateNbtProperties(
            builder = PotionBuilder(),
            nbtKey = "",
            properties = listOf(
                SimpleProperty("Potion", 8, { b -> b.id }, { b, v -> b.id = v }, CompoundTag::getString, CompoundTag::putString),
                SimpleProperty("CustomPotionColor", 99, { b -> b.customColor }, { b, v -> b.customColor = v }, CompoundTag::getInt, CompoundTag::putInt)
            ),
            block = block
        )
    }

    fun book(block: BookBuilder.() -> Unit) {
        if (item.item == Items.WRITTEN_BOOK) {
            updateNbtProperties(
                builder = BookBuilder(),
                nbtKey = "",
                properties = listOf(
                    SimpleProperty("title", 8, { b -> b.title }, { b, v -> b.title = v }, CompoundTag::getString, CompoundTag::putString),
                    SimpleProperty("author", 8, { b -> b.author }, { b, v -> b.author = v }, CompoundTag::getString, CompoundTag::putString),
                    SimpleProperty("generation", 99, { b -> b.generation }, { b, v -> b.generation = v }, CompoundTag::getInt, CompoundTag::putInt),
                    SimpleProperty("resolved", 1, { b -> b.resolved }, { b, v -> b.resolved = v }, CompoundTag::getBoolean, CompoundTag::putBoolean),
                    StringListProperty("pages", { b -> b.list })
                ),
                block = block
            )
        } else if (item.item == Items.WRITABLE_BOOK) {
            updateNbtProperties(
                builder = BookBuilder(),
                nbtKey = "",
                properties = listOf(
                    StringListProperty("pages", { b -> b.list })
                ),
                block = block
            )
        }
    }

    fun enchantments(block: EnchantmentBuilder.() -> Unit) {
        val isBook = item.item == Items.ENCHANTED_BOOK
        val targetKey = if (isBook) "StoredEnchantments" else "Enchantments"

        updateNbtProperties(
            builder = EnchantmentBuilder(),
            nbtKey = "", // Пишем прямо в корень тега предмета
            properties = listOf(
                EnchantmentListProperty(targetKey, { b -> b.list })
            ),
            block = block
        )
    }

    fun food(block: FoodBuilder.() -> Unit) {
        updateNbtProperties(
            builder = FoodBuilder(),
            nbtKey = "food",
            properties = listOf(
                SimpleProperty("nutrition", 99, { b -> b.nutrition }, { b, v -> b.nutrition = v ?: 1 }, CompoundTag::getInt, CompoundTag::putInt),
                SimpleProperty("saturation", 99, { b -> b.saturation }, { b, v -> b.saturation = v ?: 0.0f }, CompoundTag::getFloat, CompoundTag::putFloat),
                SimpleProperty("canAlwaysEat", 1, { b -> b.canAlwaysEat }, { b, v -> b.canAlwaysEat = v ?: false }, CompoundTag::getBoolean, CompoundTag::putBoolean),
                SimpleProperty("eatSeconds", 99, { b -> b.eatSeconds }, { b, v -> b.eatSeconds = v ?: 1.6f }, CompoundTag::getFloat, CompoundTag::putFloat),
                SimpleProperty("convertsTo", 8, { b -> b.convertsTo }, { b, v -> b.convertsTo = v }, CompoundTag::getString, CompoundTag::putString),
                FoodEffectListProperty("effects", { b -> b.effects })
            ),
            block = block
        )
    }

    fun banner(block: BannerBuilder.() -> Unit) {
        updateNbtProperties(
            builder = BannerBuilder(),
            nbtKey = "BlockEntityTag",
            properties = listOf(
                BannerListProperty("Patterns", { b -> b.list })
            ),
            block = block
        )
    }

    fun attributes(block: AttributesBuilder.() -> Unit) {
        updateNbtProperties(
            builder = AttributesBuilder(),
            nbtKey = "",
            properties = listOf(
                AttributeListProperty("AttributeModifiers", { b -> b.list })
            ),
            block = block
        )
    }

    fun canPlaceOn(vararg blocks: String) {
        updateNbtProperties(
            builder = AdventureBuilder().apply { list.addAll(blocks) },
            nbtKey = "",
            properties = listOf(
                StringListProperty("CanPlaceOn", { b -> b.list })
            ),
            block = {}
        )
    }

    fun canDestroy(vararg blocks: String) {
        updateNbtProperties(
            builder = AdventureBuilder().apply { list.addAll(blocks) },
            nbtKey = "",
            properties = listOf(
                StringListProperty("CanDestroy", { b -> b.list })
            ),
            block = {}
        )
    }

    var fireworkFlight: Int?
        get() {
            val fwTag = item.tag?.getCompound("Fireworks") ?: return null
            return if (fwTag.contains("Flight", 99)) fwTag.getByte("Flight").toInt() else null
        }
        set(value) {
            if (value == null) {
                val fwTag = item.tag?.getCompound("Fireworks")
                fwTag?.remove("Flight")
                if (fwTag?.isEmpty == true) {
                    item.tag?.remove("Fireworks")
                }
            } else {
                val fwTag = item.getOrCreateTagElement("Fireworks")
                fwTag.putByte("Flight", value.toByte())
            }
        }


    fun skull(block: SkullBuilder.() -> Unit) {
        updateNbtProperties(
            builder = SkullBuilder(),
            nbtKey = "",
            properties = listOf(
                SkullOwnerProperty("SkullOwner",
                    { b -> b.name }, { b, v -> b.name = v },
                    { b -> b.textureBase64 }, { b, v -> b.textureBase64 = v }
                )
            ),
            block = block
        )
    }

    fun inventory(block: InventoryBuilder.() -> Unit) {
        updateNbtProperties(
            builder = InventoryBuilder(),
            nbtKey = "BlockEntityTag",
            properties = listOf(
                InventoryListProperty("Items", { b -> b.list })
            ),
            block = block
        )
    }
}