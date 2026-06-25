package aleksti21.nbttodata.fabric

import aleksti21.nbttodata.NBTtoDataMod
import net.fabricmc.api.ModInitializer

class NBTtoDataFabric : ModInitializer {
    override fun onInitialize() {
        NBTtoDataMod.init()
    }
}