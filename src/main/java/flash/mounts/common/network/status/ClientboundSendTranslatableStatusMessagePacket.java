package flash.mounts.common.network.status;

import flash.mounts.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSendTranslatableStatusMessagePacket {

  Component component;
  boolean actionBar;

  public ClientboundSendTranslatableStatusMessagePacket(Component component, boolean actionBar) {
    this.component = component;
    this.actionBar = actionBar;
  }

  public static void encode(ClientboundSendTranslatableStatusMessagePacket msg, FriendlyByteBuf buf) {
    buf.writeComponent(msg.component);
    buf.writeBoolean(msg.actionBar);
  }

  public static ClientboundSendTranslatableStatusMessagePacket decode(FriendlyByteBuf buf) {
    return new ClientboundSendTranslatableStatusMessagePacket(buf.readComponent(), buf.readBoolean());
  }

  public static void handle(ClientboundSendTranslatableStatusMessagePacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.receiveTranslatableMessage(msg.component, msg.actionBar);
    });
    ctx.get().setPacketHandled(true);
  }

}
