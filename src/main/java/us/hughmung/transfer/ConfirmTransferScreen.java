package us.hughmung.transfer;

import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;

public class ConfirmTransferScreen extends GuiYesNo {

    public ConfirmTransferScreen(GuiYesNoCallback callback, String current, String target) {
        super(callback,
              "Server Transfer",
              current + " wants to send you to " + target + ". Do you want to continue?",
              0);
    }

}
