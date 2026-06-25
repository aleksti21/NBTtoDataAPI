package aleksti21.nbttodata

import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.advancements.critereon.BlockPredicate
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.NonNullList
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.network.Filterable
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.AdventureModePredicate
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.component.WritableBookContent
import net.minecraft.world.item.component.WrittenBookContent
import net.minecraft.world.level.block.entity.BannerPatternLayers
import java.util.Optional
import java.util.UUID
import kotlin.jvm.optionals.getOrNull


object NBTFactories {
    fun <T> RegistryAccess.getHolderSafe(registryKey: ResourceKey<Registry<T>>, id: String?): Holder<T>? {
        if (id.isNullOrBlank()) return null
        val resourceLocation = ResourceLocation.tryParse(id) ?: return null
        val registry = this.registry(registryKey).orElse(null) ?: return null
        return registry.getHolder(resourceLocation).getOrNull()
    }

    fun buildEnchantments(registries: RegistryAccess,enchantments: List<EnchantData>): ItemEnchantments {
        val builder = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
        for (i in enchantments) {
            val holder = registries.getHolderSafe(Registries.ENCHANTMENT, i.id) ?: continue
            builder.set(holder, i.level)
        }
        return builder.toImmutable()
    }

    fun buildPotion(registries: RegistryAccess, potionId: String?, customColor: Int?): PotionContents {
        val potionHolder = registries.getHolderSafe(Registries.POTION, potionId) ?: return PotionContents.EMPTY
        var potionComponent = PotionContents(potionHolder)
        if (customColor != null) potionComponent = PotionContents(potionComponent.potion, Optional.of(customColor), potionComponent.customEffects)
        return potionComponent
    }

    fun buildDisplay(registries: RegistryAccess, nameJson: String?, loreJsons: List<String>, color: Int?): Triple<MutableComponent?, ItemLore?, DyedItemColor?> {
        val customName = nameJson?.let { Component.Serializer.fromJson(it, registries) }
        val listOfLore = mutableListOf<Component>()
        for (json in loreJsons) Component.Serializer.fromJson(json, registries)?.let { listOfLore.add(it) }
        val customLore = if (listOfLore.isNotEmpty()) ItemLore(listOfLore) else null
        val customColor = color?.let { DyedItemColor(it, true) }
        return Triple(customName, customLore, customColor)
    }

    fun buildWrittenBook(registries: RegistryAccess, title: String?, author: String?, generation: Int?, resolved: Boolean?, pagesJson: List<String>): WrittenBookContent {
        val title = title ?: "Unknown"
        val author = author ?: "Unknown"
        val generation = generation ?: 0
        val resolved = resolved ?: false

        val pages = mutableListOf<Filterable<Component>>()
        for (pageJson in pagesJson) {
            try {
                val pageComp = Component.Serializer.fromJson(pageJson, registries) ?: Component.empty()
                pages.add(Filterable.passThrough(pageComp))
            } catch (e: Exception) {
                pages.add(Filterable.passThrough(Component.literal(pageJson)))
            }
        }
        return WrittenBookContent(Filterable.passThrough(title), author, generation, pages, resolved)
    }
    fun buildWritableBook(pagesText: List<String>): WritableBookContent {
        val pages = mutableListOf<Filterable<String>>()
        for (text in pagesText) {
            pages.add(Filterable.passThrough(text))
        }
        return WritableBookContent(pages)
    }

    fun buildProfile(name: String?, id: UUID?, textureBase64: String?): ResolvableProfile {
        val properties = PropertyMap()
        if (textureBase64 != null && textureBase64.isNotEmpty()) {
            properties.put("textures", Property("textures", textureBase64))
        }
        val finalId = id ?: if (textureBase64 != null) UUID.randomUUID() else null
        return ResolvableProfile(Optional.ofNullable(name), Optional.ofNullable(finalId), properties)
    }

    fun buildAttributes(registries: RegistryAccess,attributes: List<AttrData>): ItemAttributeModifiers {
        var modifiers = ItemAttributeModifiers.EMPTY

        attributes.forEachIndexed { i, data ->
            val holder = registries.getHolderSafe(Registries.ATTRIBUTE, data.attributeId) ?: return@forEachIndexed
            val modNameStr = data.modifierName?.takeIf { it.isNotEmpty() } ?: "legacy_modifier_$i"

            val cleanName = modNameStr.lowercase().replace(" ", "_").replace(":", "_")
            val modifierId = ResourceLocation.fromNamespaceAndPath("nbttodata", cleanName)
            val modifier = AttributeModifier(modifierId, data.amount, data.operation)
            modifiers = modifiers.withModifierAdded(holder, modifier, data.slot)
        }
        return modifiers
    }

    fun buildContainer(items: List<SlotData>): ItemContainerContents {
        if (items.isEmpty()) return ItemContainerContents.EMPTY
        val maxSlot = items.maxOfOrNull {it.slot} ?: 0
        val inventoryList = NonNullList.withSize(maxSlot + 1, ItemStack.EMPTY)
        for (slotData in items) {
            inventoryList[slotData.slot] = slotData.item
        }
        return ItemContainerContents.fromItems(inventoryList)
    }

    fun buildAdventurePredicate(blocks: List<String>): AdventureModePredicate {
        val predicates = mutableListOf<BlockPredicate>()
        for (blockId in blocks) {
            val blockHolder = HolderSet.direct(BuiltInRegistries.BLOCK.getHolder(ResourceLocation.parse(blockId)).orElse(null))
            val predicate = BlockPredicate(Optional.of(blockHolder), Optional.empty(), Optional.empty())
            predicates.add(predicate)
        }
        return AdventureModePredicate(predicates, true)
    }

    fun buildBanner(registries: RegistryAccess, list: List<BannerData>): BannerPatternLayers {
        val layers = mutableListOf<BannerPatternLayers.Layer>()
        for (i in list) {
            val holder = registries.getHolderSafe(Registries.BANNER_PATTERN, "minecraft:${i.pattern}")
            if (holder != null) {
                layers.add(BannerPatternLayers.Layer(holder, i.color))
            }
        }
        return BannerPatternLayers(layers)
    }

    fun buildFood(registries: RegistryAccess, nutrition: Int, saturation: Float, canAlwaysEat: Boolean, eatSeconds: Float, item: String?, effects: List<FoodEffectData>): FoodProperties {
        val effectsList = mutableListOf<FoodProperties.PossibleEffect>()
        for (effectData in effects) {
            val holder = registries.getHolderSafe(Registries.MOB_EFFECT, effectData.id) ?: continue
            val effectInstance = MobEffectInstance(holder, effectData.duration, effectData.amplifier)
            effectsList.add(FoodProperties.PossibleEffect(effectInstance, effectData.chance))
        }

        val convertsToStack = item?.let { id ->
            val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))
            ItemStack(item)
        }

        return FoodProperties(
            nutrition,
            saturation,
            canAlwaysEat,
            eatSeconds,
            Optional.ofNullable(convertsToStack),
            effectsList
        )
    }
}