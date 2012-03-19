/*
 * Copyright 2011 Benjamin Glatzel <benjamin.glatzel@me.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.gui.menus;

import org.terasology.game.Terasology;
import org.terasology.rendering.gui.components.UIButton;
import org.terasology.rendering.gui.components.UIImageOverlay;
import org.terasology.rendering.gui.components.UIText;
import org.terasology.rendering.gui.framework.IClickListener;
import org.terasology.rendering.gui.framework.UIDisplayElement;
import org.terasology.rendering.gui.framework.UIDisplayRenderer;
import org.terasology.rendering.gui.framework.UIGraphicsElement;

import javax.vecmath.Vector2f;

/**
 * Main menu screen.
 *
 * @author Anton Kireev <adeon.k87@gmail.com>
 */
public class UIMainMenu extends UIDisplayRenderer {

    private final UIImageOverlay _overlay;
    private final UIGraphicsElement _title;

    private final UIButton _exitButton;
    final UIButton _optionsButton;
    private final UIButton _startButton;
    private final UIButton _configButton;

    final UIText _version;

    public UIMainMenu() {
        _title = new UIGraphicsElement("terasology");
        _title.setVisible(true);
        _title.setSize(new Vector2f(512f, 128f));

        _version = new UIText("Pre Alpha");
        _version.setVisible(true);

        _exitButton = new UIButton(new Vector2f(256f, 32f));
        _exitButton.getLabel().setText("Exit Terasology");
        _exitButton.setVisible(true);

        _optionsButton = new UIButton(new Vector2f(256f, 32f));
        _optionsButton.getLabel().setText("Select World");
        _optionsButton.setVisible(true);

        _optionsButton.addClickListener(new IClickListener() {
            public void clicked(UIDisplayElement element) {
                Terasology.getInstance().getGameMode().deactivateScreen("main_menu");
                Terasology.getInstance().getGameMode().activateScreen("select_world");
            }
        });

        _exitButton.addClickListener(new IClickListener() {
            public void clicked(UIDisplayElement element) {
                Terasology.getInstance().exit();
            }
        });

        _configButton = new UIButton(new Vector2f(256f, 32f));
        _configButton.getLabel().setText("Settings");
        _configButton.setVisible(true);

        _startButton = new UIButton(new Vector2f(256f, 32f));
        _startButton.getLabel().setText("Play!");
        _startButton.setVisible(true);

        _overlay = new UIImageOverlay("menuBackground");
        _overlay.setVisible(false);

        addDisplayElement(_overlay);

        addDisplayElement(_title);
        addDisplayElement(_version);
        addDisplayElement(_optionsButton);
        addDisplayElement(_configButton);
        addDisplayElement(_exitButton);
        addDisplayElement(_startButton);

        update();
    }

    @Override
    public void update() {
        super.update();

        _version.centerHorizontally();
        _version.getPosition().y = 230f;

        _startButton.centerHorizontally();
        _startButton.getPosition().y = 300f + 40f;

        _optionsButton.centerHorizontally();
        _optionsButton.getPosition().y = 300f + 5 * 32f + 32f;

        _exitButton.centerHorizontally();
        _exitButton.getPosition().y = 300f + 3 * 32f + 64f;

        _configButton.centerHorizontally();
        _configButton.getPosition().y = 300f + 2 * 40f;

        _exitButton.centerHorizontally();
        _exitButton.getPosition().y = 300f + 4 * 40f;

        _title.centerHorizontally();
        _title.getPosition().y = 128f;
    }

    public UIButton getExitButton() {
        return _exitButton;
    }

    public UIButton getStartButton() {
        return _startButton;
    }

    public UIButton getConfigButton() {
        return _configButton;
    }
}
