package aleksti21.nbttodata.neoforge

import aleksti21.nbttodata.NBTtoDataMod
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod

@Mod(NBTtoDataMod.MOD_ID)
class NBTtoDataForge(modEventBus: IEventBus) {
    init {
        NBTtoDataMod.init()
    }
}