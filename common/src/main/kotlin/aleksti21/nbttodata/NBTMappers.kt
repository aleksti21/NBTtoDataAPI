package aleksti21.nbttodata

import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.Unbreakable
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.level.saveddata.maps.MapId
import java.util.UUID

object NbtMappers {
    fun CompoundTag.takeBoolean(key: String, default: Boolean = false): Boolean {
        if (!this.contains(key)) return default
        val value = this.getBoolean(key)
        this.remove(key)
        return value
    }

    fun CompoundTag.takeInt(key: String, default: Int = 0): Int {
        if (!this.contains(key)) return default
        val value = this.getInt(key)
        this.remove(key)
        return value
    }

    fun CompoundTag.takeString(key: String, default: String = ""): String {
        if (!this.contains(key)) return default
        val value = this.getString(key)
        this.remove(key)
        return value
    }


    val RULES = listOf(
        NbtRule.int("Damage", DataComponents.DAMAGE),
        NbtRule.int("map", DataComponents.MAP_ID) { MapId(it) },
        NbtRule("Enchantments") { tag, item, registries -> item.set(DataComponents.ENCHANTMENTS, parseEnchantments(tag, "Enchantments", registries))},
        NbtRule("StoredEnchantments") {tag, item, registries -> item.set(DataComponents.STORED_ENCHANTMENTS, parseEnchantments(tag, "StoredEnchantments", registries))},
        NbtRule("Potion") { tag, item, registries ->
            item.set(DataComponents.POTION_CONTENTS, NBTFactories.buildPotion(registries, tag.getString("Potion"), tag.takeInt("CustomPotionColor")))
        },
        NbtRule.int("CustomModelData", DataComponents.CUSTOM_MODEL_DATA) { CustomModelData(it) },
        NbtRule.int("RepairCost", DataComponents.REPAIR_COST),
        NbtRule.boolean("GlintOverride", DataComponents.ENCHANTMENT_GLINT_OVERRIDE) { it },

        NbtRule("Unbreakable") { tag, item, _ ->
            val value = tag.getBoolean("Unbreakable")
            if (value) { item.set(DataComponents.UNBREAKABLE, Unbreakable(true)) }
        },

        NbtRule("display") { tag, item, registries ->
            val displayTag = tag.getCompound("display")
            val nameJson = if (displayTag.contains("Name")) displayTag.getString("Name") else null
            val colorInt = if (displayTag.contains("color")) displayTag.getInt("color") else null

            val loreJsons = mutableListOf<String>()
            if (displayTag.contains("Lore")) {
                val loreList = displayTag.getList("Lore", 8)
                for (i in 0 until loreList.size) loreJsons.add(loreList.getString(i))
            }

            val (nameComponent, loreComponent, colorComponent) = NBTFactories.buildDisplay(registries, nameJson, loreJsons, colorInt)
            if (nameComponent != null) item.set(DataComponents.CUSTOM_NAME, nameComponent)
            if (loreComponent != null) item.set(DataComponents.LORE, loreComponent)
            if (colorComponent != null) item.set(DataComponents.DYED_COLOR, colorComponent)
        },

        NbtRule("BlockEntityTag") { tag, item, registries ->
            val blockTag = tag.getCompound("BlockEntityTag")

            if (blockTag.contains("Items", 9)) {
                val itemsList = blockTag.getList("Items", 10)
                val inventoryMap = mutableListOf<SlotData>()

                for (i in 0 until itemsList.size) {
                    val itemTag = itemsList.getCompound(i)
                    val slot = itemTag.getByte("Slot").toInt() and 0xFF

                    val idStr = itemTag.getString("id")
                    val count = if (itemTag.contains("Count")) itemTag.getByte("Count").toInt() else itemTag.getInt("count")

                    val innerItem = ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(idStr)), if (count > 0) count else 1)

                    if (itemTag.contains("tag", 10)) {
                        innerItem.applySNBT(itemTag.getCompound("tag").toString(), registries)
                    }
                    inventoryMap.add(SlotData(slot, innerItem))
                }
                val component = NBTFactories.buildContainer(inventoryMap)
                item.set(DataComponents.CONTAINER, component)
                blockTag.remove("Items")
            }

            if (blockTag.contains("Patterns", 9)) {
                val patternsList = blockTag.getList("Patterns", 10)
                val returnList = mutableListOf<BannerData>()

                for (i in 0 until patternsList.size) {
                    val pTag = patternsList.getCompound(i)
                    returnList.add(BannerData(pTag.getString("Pattern"), DyeColor.byId(pTag.getInt("Color"))))
                }

                item.set(DataComponents.BANNER_PATTERNS, NBTFactories.buildBanner(registries, returnList))
                blockTag.remove("Patterns")
            }

            if (!blockTag.isEmpty) {
                item.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockTag))
            }
        },

        NbtRule("SkullOwner") { tag, item, _ ->
            if (tag.contains("SkullOwner", 8)) {
                val name = tag.getString("SkullOwner")
                val profile = NBTFactories.buildProfile(name, null, null)
                item.set(DataComponents.PROFILE, profile)
            }
            else if (tag.contains("SkullOwner", 10)) {
                val skullTag = tag.getCompound("SkullOwner")

                val name = if (skullTag.contains("Name", 8)) skullTag.getString("Name") else null

                var id: UUID? = null
                if (skullTag.hasUUID("Id")) {
                    val parsedId = skullTag.getUUID("Id")
                    if (parsedId.mostSignificantBits != -1L) {
                        id = parsedId
                    }
                }

                var texture: String? = null
                if (skullTag.contains("Properties", 10)) {
                    val propsTag = skullTag.getCompound("Properties")
                    if (propsTag.contains("textures", 9)) { // 9 - это ID типа List
                        val texturesList = propsTag.getList("textures", 10)
                        if (texturesList.size > 0) {
                            texture = texturesList.getCompound(0).getString("Value")
                        }
                    }
                }
                val profile = NBTFactories.buildProfile(name, id, texture)
                item.set(DataComponents.PROFILE, profile)
            }
        },

        NbtRule("food") { tag, item, registries ->
            val fTag = tag.getCompound("food")
            val eatSec = if (fTag.contains("eatSeconds")) fTag.getFloat("eatSeconds") else 1.6f
            val convertsTo = if (fTag.contains("convertsTo")) fTag.getString("convertsTo") else null

            val effectsList = mutableListOf<FoodEffectData>()
            if (fTag.contains("effects", 9)) { // 9 — тип ListTag
                val list = fTag.getList("effects", 10) // 10 — тип CompoundTag
                for (i in 0 until list.size) {
                    val effectTag = list.getCompound(i)
                    effectsList.add(FoodEffectData(
                        effectTag.getString("id"),
                        effectTag.getInt("duration"),
                        if (effectTag.contains("amplifier")) effectTag.getInt("amplifier") else 0,
                        if (effectTag.contains("chance")) effectTag.getFloat("chance") else 1.0f
                    ))
                }
            }

            val foodComp = NBTFactories.buildFood(
                registries,
                fTag.getInt("nutrition"),
                fTag.getFloat("saturation"),
                fTag.getBoolean("canAlwaysEat"),
                eatSec,
                convertsTo,
                effectsList
            )

            item.set(DataComponents.FOOD, foodComp)
        },

        NbtRule("Fireworks") { tag, item, _ ->
            val fwTag = tag.getCompound("Fireworks")
            val flight = if (fwTag.contains("Flight")) fwTag.getByte("Flight").toInt() else 1
            item.set(DataComponents.FIREWORKS, Fireworks(flight, emptyList()))
        },

        NbtRule("CanDestroy") { tag, item, _ ->
            val list = tag.getList("CanDestroy", 8) // 8 = String
            val blocks = mutableListOf<String>()
            for (i in 0 until list.size) blocks.add(list.getString(i))

            item.set(DataComponents.CAN_BREAK, NBTFactories.buildAdventurePredicate(blocks))
        },

        NbtRule("CanPlaceOn") { tag, item, _ ->
            val list = tag.getList("CanPlaceOn", 8)
            val blocks = mutableListOf<String>()
            for (i in 0 until list.size) blocks.add(list.getString(i))

            item.set(DataComponents.CAN_PLACE_ON, NBTFactories.buildAdventurePredicate(blocks))
        },

        NbtRule("AttributeModifiers") { tag, item, registries ->
            val list = tag.getList("AttributeModifiers", 10)
            val attrList = mutableListOf<AttrData>()

            for (i in 0 until list.size) {
                val modTag = list.getCompound(i)

                val attrName = modTag.getString("AttributeName")
                val amount = modTag.getDouble("Amount")

                val opInt = modTag.getInt("Operation")
                val operation = when(opInt) {
                    1 -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    2 -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    else -> AttributeModifier.Operation.ADD_VALUE
                }

                val slotStr = modTag.getString("Slot")
                val slot = when(slotStr.lowercase()) {
                    "mainhand" -> EquipmentSlotGroup.MAINHAND
                    "offhand" -> EquipmentSlotGroup.OFFHAND
                    "feet" -> EquipmentSlotGroup.FEET
                    "legs" -> EquipmentSlotGroup.LEGS
                    "chest" -> EquipmentSlotGroup.CHEST
                    "head" -> EquipmentSlotGroup.HEAD
                    else -> EquipmentSlotGroup.ANY
                }
                val modName = if (modTag.contains("Name", 8)) modTag.getString("Name") else null
                attrList.add(AttrData(attrName, modName, amount, operation, slot))
            }
            val component = NBTFactories.buildAttributes(registries, attrList)
            item.set(DataComponents.ATTRIBUTE_MODIFIERS, component)
        },

        NbtRule("pages") { tag, item, registries ->
            val pagesList = tag.getList("pages", 8)
            val jsonList = mutableListOf<String>()
            for (i in 0 until pagesList.size) jsonList.add(pagesList.getString(i))

            if (item.item == Items.WRITTEN_BOOK) {
                item.set(
                    DataComponents.WRITTEN_BOOK_CONTENT,
                    NBTFactories.buildWrittenBook(
                        registries,
                        tag.takeString("title"),
                        tag.takeString("author"),
                        tag.takeInt("generation"),
                        tag.takeBoolean("resolved"),
                        jsonList
                    )
                )
            } else if (item.item == Items.WRITABLE_BOOK) {
                item.set(DataComponents.WRITABLE_BOOK_CONTENT, NBTFactories.buildWritableBook(jsonList))
            }
        },

        NbtRule("HideFlags") { tag, item, _ ->
            val flags = tag.getInt("HideFlags")

            // Бит 1 (Скрыть зачарования)
            if ((flags and 1) != 0) {
                val enchs = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                item.set(DataComponents.ENCHANTMENTS, enchs.withTooltip(false))
            }

            // Бит 2 (Скрыть атрибуты / урон)
            if ((flags and 2) != 0) {
                val attrs = item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                item.set(DataComponents.ATTRIBUTE_MODIFIERS, attrs.withTooltip(false))
            }

            // Бит 4 (Скрыть Неразрушимость)
            if ((flags and 4) != 0) {
                // Если у предмета есть неразрушимость, ставим флаг скрытия
                if (item.has(DataComponents.UNBREAKABLE)) {
                    item.set(DataComponents.UNBREAKABLE, Unbreakable(false))
                }
            }

            // Бит 32 (Скрыть дополнительные подсказки, например, описание зелий или фейерверков)
            if ((flags and 32) != 0) {
                item.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
            }
        }
    )

    // Изменяем сам метод
    private fun parseEnchantments(tag: CompoundTag, key: String, registries: RegistryAccess): ItemEnchantments {
        val list = tag.getList(key, 10)
        val enchMap = mutableListOf<EnchantData>()

        for (i in 0 until list.size) {
            val enchTag = list.getCompound(i)
            val idStr = enchTag.getString("id")
            if (idStr.isNotEmpty()) enchMap.add(EnchantData(idStr, enchTag.getInt("lvl")))
        }
        return NBTFactories.buildEnchantments(registries,  enchMap)
    }
}