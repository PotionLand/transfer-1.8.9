package us.hughmung.transfer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SideOnly(Side.CLIENT)
@Mod(modid = "transfer", version = "1.0.0", name = "Transfer", acceptedMinecraftVersions = "[1.8.9]")
public class Transfer {

    public static ExecutorService SERVICE;
    public static SimpleNetworkWrapper CHANNEL;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SERVICE = Executors.newCachedThreadPool();

        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("loom:transfer");
        CHANNEL.registerMessage(InTransferRequest.Handler.class, InTransferRequest.class, 0, Side.CLIENT);
    }
}