package com.sandyplugins.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.MenuAction;

@Getter
@Setter
@AllArgsConstructor
public class LegacyMenuEntry
{
    String option;
    String target;
    int identifier;
    MenuAction type;
    int param0;
    int param1;
    boolean forceLeftClick;

    public int getOpcode()
    {
        return type.getId();
    }
}
