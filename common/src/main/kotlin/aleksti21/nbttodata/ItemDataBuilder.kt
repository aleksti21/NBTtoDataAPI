package aleksti21.nbttodata

import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.Unbreakable
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.level.block.entity.BannerPatternLayers
import net.minecraft.world.level.saveddata.maps.MapId

private val enchantsTooltipField = ItemEnchantments::class.java.getDeclaredField("showInTooltip").apply { isAccessible = true }

val ItemEnchantments.showInTooltip: Boolean
    get() = enchantsTooltipField.get(this) as Boolean

class ItemDataBuilder(private val item: ItemStack, private val registries: RegistryAccess) {
    private fun <T> ItemStack.setOrRemove(component: DataComponentType<T>, value: T?) {
        if (value == null) {
            this.remove(component)
            return
        }
        val isEmpty = when (value) {
            is ItemEnchantments -> value.isEmpty
            is ItemAttributeModifiers -> value.modifiers().isEmpty()
            is CustomData -> value.isEmpty
            is ItemLore -> value.lines().isEmpty()
            else -> false
        }
        if (isEmpty) this.remove(component)
        else this.set(component, value)
    }

    private inline fun <B, T> updateComponent(
        builder: B,
        componentType: DataComponentType<T>,
        defaultEmpty: T,
        loadExisting: (B, T) -> Unit, // Лямбда загрузки старого состояния
        block: B.() -> Unit,
        factory: (B) -> T?            // Фабрика принимает готовый билдер
    ) {
        val existing = item.getOrDefault(componentType, defaultEmpty)
        loadExisting(builder, existing) // 1. Грузим старое
        builder.block()                 // 2. Выполняем изменения программиста
        val newComponent = factory(builder) // 3. Собираем новое
        item.setOrRemove(componentType, newComponent)
    }

    var damage: Int
        get() = item.getOrDefault(DataComponents.DAMAGE, 0)
        set(value) { item.set(DataComponents.DAMAGE, value) }

    var customModelData: Int
        get() = item.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(0)).value()
        set(value) { item.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(value)) }

    var unbreakable: Boolean
        get() = item.has(DataComponents.UNBREAKABLE)
        set(value) {
            if (value) item.set(DataComponents.UNBREAKABLE, Unbreakable(true))
            else item.remove(DataComponents.UNBREAKABLE)
        }

    var mapId: MapId
        get() = item.getOrDefault(DataComponents.MAP_ID, MapId(0))
        set(value) {item.set(DataComponents.MAP_ID, value)}

    fun potion(block: PotionBuilder.() -> Unit) {
        updateComponent(
            builder = PotionBuilder(),
            componentType = DataComponents.POTION_CONTENTS,
            defaultEmpty = PotionContents.EMPTY,
            loadExisting = { _, _ -> }, // Пустая лямбда — пишем с чистого листа!
            block = block
        ) { builder ->
            NBTFactories.buildPotion(registries, builder.id, builder.customColor)
        }
    }

    fun enchantments(block: EnchantmentBuilder.() -> Unit) {
        val isBook = item.item == Items.ENCHANTED_BOOK
        val componentType = if (isBook) DataComponents.STORED_ENCHANTMENTS else DataComponents.ENCHANTMENTS

        updateComponent(
            builder = EnchantmentBuilder(),
            componentType = componentType,
            defaultEmpty = ItemEnchantments.EMPTY,
            loadExisting = { builder, existing ->
                existing.entrySet().forEach { (holder, level) ->
                    val id = holder.unwrapKey().orElse(null)?.location()?.toString() ?: return@forEach
                    builder.list.add(EnchantData(id, level))
                }
            },
            block = block
        ) { builder ->
            NBTFactories.buildEnchantments(registries, builder.list)
        }
    }

    fun food(block: FoodBuilder.() -> Unit) {
        updateComponent(
            builder = FoodBuilder(),
            componentType = DataComponents.FOOD,
            defaultEmpty = null,
            loadExisting = { _, _ -> }, // Пустая лямбда — пишем с чистого листа!
            block = block
        ) { builder ->
            NBTFactories.buildFood(registries, builder.nutrition, builder.saturation, builder.canAlwaysEat, builder.eatSeconds, builder.convertsTo, builder.effects)
        }
    }

    fun banner(block: BannerBuilder.() -> Unit) {
        updateComponent(
            builder = BannerBuilder(),
            componentType = DataComponents.BANNER_PATTERNS,
            defaultEmpty = BannerPatternLayers.EMPTY,
            loadExisting = { builder, existing ->
                existing.layers.forEach { layer ->
                    val patternId = layer.pattern.unwrapKey().orElse(null)?.location()?.toString() ?: return@forEach
                    val cleanId = patternId.substringAfter("minecraft:")
                    builder.list.add(BannerData(cleanId, layer.color))
                }
            },
            block = block
        ) { builder ->
            NBTFactories.buildBanner(registries, builder.list)
        }
    }

    fun book(block: BookBuilder.() -> Unit) {
        if (item.item == Items.WRITTEN_BOOK) {
            updateComponent(
                builder = BookBuilder(),
                componentType = DataComponents.WRITTEN_BOOK_CONTENT,
                defaultEmpty = null,
                loadExisting = { builder, existing ->
                    if (existing != null) {
                        builder.title = existing.title.raw() // В 1.21.1 title — это Filterable<String>
                        builder.author = existing.author
                        builder.generation = existing.generation
                        builder.resolved = existing.resolved

                        // В подписанной книге страницы — это Filterable<Component>
                        existing.pages().forEach { page ->
                            val rawText = page.raw().string
                            builder.list.add(rawText)
                        }
                    }
                },
                block = block
            ) { builder ->
                NBTFactories.buildWrittenBook(
                    registries, builder.title, builder.author, builder.generation, builder.resolved, builder.pages
                )
            }
        } else if (item.item == Items.WRITABLE_BOOK) {
            updateComponent(
                builder = BookBuilder(),
                componentType = DataComponents.WRITABLE_BOOK_CONTENT,
                defaultEmpty = null,
                loadExisting = { builder, existing ->
                    if (existing != null) {
                        // В книге с пером страницы — это просто Filterable<String>
                        existing.pages().forEach { page ->
                            builder.list.add(page.raw())
                        }
                    }
                },
                block = block
            ) { builder ->
                NBTFactories.buildWritableBook(builder.pages)
            }
        }
    }

    fun attributes(block: AttributesBuilder.() -> Unit) {
        updateComponent(
            builder = AttributesBuilder(),
            componentType = DataComponents.ATTRIBUTE_MODIFIERS,
            defaultEmpty = ItemAttributeModifiers.EMPTY,

            loadExisting = { builder, existing ->
                if (existing != null) {
                    existing.modifiers().forEach { entry ->
                        val attrId = entry.attribute().unwrapKey().orElse(null)?.location()?.toString() ?: return@forEach
                        val modifier = entry.modifier()

                        builder.list.add(AttrData(
                            attributeId = attrId,
                            modifierName = modifier.id().path,
                            amount = modifier.amount(),
                            operation = modifier.operation(),
                            slot = entry.slot()
                        ))
                    }
                }
            },
            block = block
        ) { builder ->
            NBTFactories.buildAttributes(registries, builder.list)
        }
    }

    fun canDestroy(vararg blocks: String) {
        val component = NBTFactories.buildAdventurePredicate(blocks.toList())
        item.set(DataComponents.CAN_BREAK, component)
    }

    fun canPlaceOn(vararg blocks: String) {
        val component = NBTFactories.buildAdventurePredicate(blocks.toList())
        item.set(DataComponents.CAN_PLACE_ON, component)
    }

    var fireworkFlight: Int
        get() = item.getOrDefault(DataComponents.FIREWORKS, Fireworks(0, emptyList())).flightDuration()
        set(value) {
            item.set(DataComponents.FIREWORKS, Fireworks(value, emptyList()))
        }

    fun display(block: DisplayBuilder.() -> Unit) {
        val b = DisplayBuilder()
        item.get(DataComponents.CUSTOM_NAME)?.let { comp ->
            b.nameJson = Component.Serializer.toJson(comp, registries)
        }
        item.get(DataComponents.LORE)?.let { lore ->
            lore.lines().forEach { line ->
                b.loreJsons.add(Component.Serializer.toJson(line, registries))
            }
        }
        item.get(DataComponents.DYED_COLOR)?.let { dyed ->
            b.color = dyed.rgb()
        }
        b.apply(block)

        val (nameComponent, loreComponent, colorComponent) = NBTFactories.buildDisplay(registries, b.nameJson, b.loreJsons, b.color)
        item.setOrRemove(DataComponents.CUSTOM_NAME, nameComponent)
        item.setOrRemove(DataComponents.LORE, loreComponent)
        item.setOrRemove(DataComponents.DYED_COLOR, colorComponent)
    }

    var hideEnchantments: Boolean
        get() = !item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).showInTooltip
        set(value) {
            val enchs = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
            item.set(DataComponents.ENCHANTMENTS, enchs.withTooltip(!value))
        }

    var hideAttributes: Boolean
        get() = !item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip()
        set(value) {
            val attrs = item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
            item.set(DataComponents.ATTRIBUTE_MODIFIERS, attrs.withTooltip(!value))
        }

    var hideUnbreakable: Boolean
        get() {
            val unbreakable = item.get(DataComponents.UNBREAKABLE) ?: return false
            return !unbreakable.showInTooltip()
        }
        set(value) {
            if (item.has(DataComponents.UNBREAKABLE)) {
                item.set(DataComponents.UNBREAKABLE, Unbreakable(!value))
            }
        }

    var hideAdditionalTooltip: Boolean
        get() = item.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)
        set(value) {
            if (value) item.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
            else item.remove(DataComponents.HIDE_ADDITIONAL_TOOLTIP)
        }


    fun skull(block: SkullBuilder.() -> Unit) {
        updateComponent(
            builder = SkullBuilder(),
            componentType = DataComponents.PROFILE,
            defaultEmpty = null,
            loadExisting = { _, _ -> },
            block = block
        ) { builder ->
            NBTFactories.buildProfile(builder.name, null, builder.textureBase64)
        }
    }

    fun inventory(block: InventoryBuilder.() -> Unit) {
        updateComponent(
            builder = InventoryBuilder(),
            componentType = DataComponents.CONTAINER,
            defaultEmpty = ItemContainerContents.EMPTY,
            loadExisting = { builder, existing ->
                if (existing != null) {
                    val list = NonNullList.withSize(27, ItemStack.EMPTY)
                    existing.copyInto(list)
                    list.forEachIndexed { index, stack ->
                        if (!stack.isEmpty) {
                            builder.list.add(SlotData(index, stack))
                        }
                    }
                }
            },
            block = block
        ) { builder ->
            NBTFactories.buildContainer(builder.list)
        }
    }
    fun blockEntityData(block: CompoundTag.() -> Unit) {
        val currentData = item.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
        val tag = currentData.copyTag()

        tag.block()

        item.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag))
    }
    fun customData(block: CompoundTag.() -> Unit) {
        val currentData = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        val tag = currentData.copyTag()
        tag.block()
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    }
}