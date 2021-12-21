package com.sandyplugins.plugin.walking;

import com.sandyplugins.plugin.game.Game;

public interface Requirement {

    Game game();

    boolean satisfies();
}
