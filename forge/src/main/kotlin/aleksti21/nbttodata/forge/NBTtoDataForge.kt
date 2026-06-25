package aleksti21.nbttodata.forge

import aleksti21.nbttodata.NBTtoDataMod
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod

@Mod(NBTtoDataMod.MOD_ID)
class NBTtoDataForge(modEventBus: IEventBus) {
    init {
        NBTtoDataMod.init()
    }
}