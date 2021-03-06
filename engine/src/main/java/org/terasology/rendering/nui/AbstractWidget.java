/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.nui;

import org.terasology.math.Vector2i;
import org.terasology.rendering.nui.skin.UIStyle;

/**
 * @author Immortius
 */
public abstract class AbstractWidget implements UIWidget {

    private final String id;
    private String family;
    private boolean focused;

    public AbstractWidget() {
        id = "";
    }

    public AbstractWidget(String id) {
        this.id = id;
    }

    @Override
    public String getMode() {
        return DEFAULT_MODE;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final String getFamily() {
        return family;
    }

    @Override
    public final void setFamily(String family) {
        this.family = family;
    }

    @Override
    public final <T extends UIWidget> T find(String targetId, Class<T> type) {
        if (this.id.equals(targetId)) {
            if (type.isInstance(this)) {
                return type.cast(this);
            }
            return null;
        }
        for (UIWidget contents : this) {
            T result = contents.find(targetId, type);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public void onGainFocus() {
        focused = true;
    }

    @Override
    public void onLoseFocus() {
        focused = false;
    }

    public final boolean isFocused() {
        return focused;
    }

    @Override
    public Vector2i calcContentSize(UIStyle style, Vector2i areaHint) {
        return areaHint;
    }

    @Override
    public boolean isSkinAppliedByCanvas() {
        return true;
    }
}
