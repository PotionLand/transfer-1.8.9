package us.hughmung.transfer;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class InTransferRequest implements IMessage {
    private String target;

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        try {
            this.target = buffer.readStringFromBuffer(32767);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        try {
            buffer.writeString(this.target);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static class Handler implements IMessageHandler<InTransferRequest, IMessage> {
        @Override
        public IMessage onMessage(InTransferRequest message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                String current = Verifier.getCurrentServerAddress();
                Verifier.Result result = Verifier.verify(current, message.target);

                if (!result.isTransferAllowed()) {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI()
                          .printChatMessage(new ChatComponentText(
                                EnumChatFormatting.RED + "[Loom] " +
                                      EnumChatFormatting.WHITE + "The server you are on attempted to send you to " +
                                      EnumChatFormatting.RED + message.target +
                                      EnumChatFormatting.WHITE + ", but their policy does not allow it."
                          ));
                    return null;
                }

                // Minecraft's addScheduledTask method executes the task instantly if already on the main thread
                // which is not good, so we use an executor service to trick it into scheduling the task for the next game tick.
                Runnable callback = () -> Transfer.SERVICE.execute(() -> {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        Minecraft mc = Minecraft.getMinecraft();
                        ServerData data = new ServerData(message.target, message.target, false);

                        mc.theWorld.sendQuittingDisconnectingPacket();
                        mc.loadWorld(null);

                        mc.displayGuiScreen(new GuiConnecting(
                              null,
                              mc,
                              data
                        ));
                        return null;
                    });
                });

                if (!result.hasTxtRecord()) {
                    Minecraft.getMinecraft().displayGuiScreen(new ConfirmTransferScreen((yes, __) -> {
                        Minecraft.getMinecraft().displayGuiScreen(null);
                        Transfer.CHANNEL.sendToServer(new OutTransferResponse(yes));

                        if (yes) {
                            callback.run();
                        }
                    }, current, message.target));
                } else {
                    Transfer.CHANNEL.sendToServer(new OutTransferResponse(true));
                    callback.run();
                }
                return null;
            });
            return null;
        }
    }

    public static class OutTransferResponse implements IMessage {
        private final boolean accepted;

        OutTransferResponse(boolean accepted) {
            this.accepted = accepted;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
        }

        @Override
        public void toBytes(ByteBuf buf) {
            PacketBuffer buffer = new PacketBuffer(buf);
            try {
                buffer.writeBoolean(this.accepted);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
