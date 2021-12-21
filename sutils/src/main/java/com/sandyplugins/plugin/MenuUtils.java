package com.sandyplugins.plugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class MenuUtils {
    @Inject
    private Client client;

    public LegacyMenuEntry entryAdded;
    public LegacyMenuEntry entry;

    public boolean consumeClick;
    public boolean modifiedMenu;
    public int modifiedItemID;
    public int modifiedItemIndex;
    public int modifiedOpCode;

    public void setSelectedSpell(WidgetInfo info) {
        final Widget widget = client.getWidget(info);

        if (widget != null) {
            client.setSelectedSpellWidget(widget.getId());
            client.setSelectedSpellChildIndex(-1);
        }
    }

    public void setEntry(LegacyMenuEntry LegacyMenuEntry) {
        entry = LegacyMenuEntry;
    }

    public void setEntry(LegacyMenuEntry LegacyMenuEntry, boolean consume) {
        entry = LegacyMenuEntry;
        consumeClick = consume;
    }

    public void setModifiedEntry(LegacyMenuEntry LegacyMenuEntry, int itemID, int itemIndex, int opCode) {
        entry = LegacyMenuEntry;
        modifiedMenu = true;
        modifiedItemID = itemID;
        modifiedItemIndex = itemIndex;
        modifiedOpCode = opCode;
    }

}
