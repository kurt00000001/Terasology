/*
 * Copyright 2013 MovingBlocks
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
package org.terasology.input;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.lwjgl.input.Keyboard;
import org.terasology.config.Config;
import org.terasology.engine.CoreRegistry;
import org.terasology.engine.GameEngine;
import org.terasology.engine.SimpleUri;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.In;
import org.terasology.input.cameraTarget.CameraTargetSystem;
import org.terasology.input.device.InputAction;
import org.terasology.input.device.MouseDevice;
import org.terasology.input.device.nulldevices.NullMouseDevice;
import org.terasology.input.events.InputEvent;
import org.terasology.input.events.KeyDownEvent;
import org.terasology.input.events.KeyEvent;
import org.terasology.input.events.KeyRepeatEvent;
import org.terasology.input.events.KeyUpEvent;
import org.terasology.input.events.LeftMouseDownButtonEvent;
import org.terasology.input.events.LeftMouseUpButtonEvent;
import org.terasology.input.events.MouseAxisEvent;
import org.terasology.input.events.MouseButtonEvent;
import org.terasology.input.events.MouseDownButtonEvent;
import org.terasology.input.events.MouseUpButtonEvent;
import org.terasology.input.events.MouseWheelEvent;
import org.terasology.input.events.MouseXAxisEvent;
import org.terasology.input.events.MouseYAxisEvent;
import org.terasology.input.events.RightMouseDownButtonEvent;
import org.terasology.input.events.RightMouseUpButtonEvent;
import org.terasology.input.internal.BindableAxisImpl;
import org.terasology.input.internal.BindableButtonImpl;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.Vector2i;

import java.util.List;
import java.util.Map;

/**
 * This system processes input, sending it out as events against the LocalPlayer entity.
 * <p/>
 * In addition to raw keyboard and mouse input, the system handles Bind Buttons and Bind Axis, which can be mapped
 * to one or more inputs.
 */
public class InputSystem implements ComponentSystem {

    @In
    private Config config;

    @In
    private GameEngine engine;

    private MouseDevice mouse = new NullMouseDevice();

    private Map<String, BindableAxisImpl> axisLookup = Maps.newHashMap();
    private Map<SimpleUri, BindableButtonImpl> buttonLookup = Maps.newHashMap();

    private List<BindableAxisImpl> axisBinds = Lists.newArrayList();
    private List<BindableButtonImpl> buttonBinds = Lists.newArrayList();

    // Links between primitive inputs and bind buttons
    private Map<Integer, BindableButtonImpl> keyBinds = Maps.newHashMap();
    private Map<MouseInput, BindableButtonImpl> mouseButtonBinds = Maps.newHashMap();
    private BindableButtonImpl mouseWheelUpBind;
    private BindableButtonImpl mouseWheelDownBind;

    private LocalPlayer localPlayer;
    private CameraTargetSystem targetSystem;

    public void initialise() {
        localPlayer = CoreRegistry.get(LocalPlayer.class);
        targetSystem = CoreRegistry.get(CameraTargetSystem.class);
    }

    @Override
    public void shutdown() {
        localPlayer = null;
        targetSystem = null;
    }

    public void setMouseDevice(MouseDevice mouseDevice) {
        this.mouse = mouseDevice;
    }

    public MouseDevice getMouseDevice() {
        return mouse;
    }

    public BindableButton registerBindButton(SimpleUri bindId, String displayName) {
        return registerBindButton(bindId, displayName, new BindButtonEvent());
    }

    public BindableButton registerBindButton(SimpleUri bindId, String displayName, BindButtonEvent event) {
        BindableButtonImpl bind = new BindableButtonImpl(bindId, displayName, event);
        buttonLookup.put(bindId, bind);
        buttonBinds.add(bind);
        return bind;
    }

    public BindableButton getBindButton(SimpleUri bindId) {
        return buttonLookup.get(bindId);
    }

    public void linkBindButtonToInput(Input input, SimpleUri bindId) {
        switch (input.getType()) {
            case KEY:
                linkBindButtonToKey(input.getId(), bindId);
                break;
            case MOUSE_BUTTON:
                MouseInput button = MouseInput.getInputFor(input.getType(), input.getId());
                linkBindButtonToMouse(button, bindId);
                break;
            case MOUSE_WHEEL:
                linkBindButtonToMouseWheel(input.getId(), bindId);
                break;
        }
    }

    public void linkBindButtonToInput(InputEvent input, SimpleUri bindId) {
        if (input instanceof KeyEvent) {
            linkBindButtonToKey(((KeyEvent) input).getKey(), bindId);
        } else if (input instanceof MouseButtonEvent) {
            linkBindButtonToMouse(((MouseButtonEvent) input).getButton(), bindId);
        } else if (input instanceof MouseWheelEvent) {
            linkBindButtonToMouseWheel(((MouseWheelEvent) input).getWheelTurns(), bindId);
        }
    }

    public void linkBindButtonToKey(int key, SimpleUri bindId) {
        BindableButtonImpl bindInfo = buttonLookup.get(bindId);
        keyBinds.put(key, bindInfo);
    }

    public void linkBindButtonToMouse(MouseInput mouseButton, SimpleUri bindId) {
        BindableButtonImpl bindInfo = buttonLookup.get(bindId);
        mouseButtonBinds.put(mouseButton, bindInfo);
    }

    public void linkBindButtonToMouseWheel(int direction, SimpleUri bindId) {
        if (direction > 0) {
            mouseWheelDownBind = buttonLookup.get(bindId);
        } else if (direction < 0) {
            mouseWheelUpBind = buttonLookup.get(bindId);
        }
    }

    public BindableAxis registerBindAxis(String id, BindableButton positiveButton, BindableButton negativeButton) {
        return registerBindAxis(id, new BindAxisEvent(), positiveButton, negativeButton);
    }

    public BindableAxis registerBindAxis(String id, BindAxisEvent event, SimpleUri positiveButtonId, SimpleUri negativeButtonId) {
        return registerBindAxis(id, event, getBindButton(positiveButtonId), getBindButton(negativeButtonId));
    }

    public BindableAxis registerBindAxis(String id, BindAxisEvent event, BindableButton positiveButton, BindableButton negativeButton) {
        BindableAxisImpl axis = new BindableAxisImpl(id, event, positiveButton, negativeButton);
        axisBinds.add(axis);
        axisLookup.put(id, axis);
        return axis;
    }

    public void update(float delta) {
        processMouseInput(delta);
        processKeyboardInput(delta);
        processBindRepeats(delta);
        processBindAxis(delta);
    }

    private void processMouseInput(float delta) {
        if (!engine.hasFocus()) {
            return;
        }

        Vector2i deltaMouse = mouse.getDelta();
        //process mouse movement x axis
        if (deltaMouse.x != 0) {
            MouseAxisEvent event = new MouseXAxisEvent(deltaMouse.x * config.getInput().getMouseSensitivity(), delta);
            setupTarget(event);
            for (EntityRef entity : getInputEntities()) {
                entity.send(event);
                if (event.isConsumed()) {
                    break;
                }
            }
        }

        //process mouse movement y axis
        if (deltaMouse.y != 0) {
            MouseAxisEvent event = new MouseYAxisEvent(deltaMouse.y * config.getInput().getMouseSensitivity(), delta);
            setupTarget(event);
            for (EntityRef entity : getInputEntities()) {
                entity.send(event);
                if (event.isConsumed()) {
                    break;
                }
            }
        }

        //process mouse clicks
        for (InputAction action : mouse.getInputQueue()) {
            switch (action.getInput().getType()) {
                case MOUSE_BUTTON:
                    int id = action.getInput().getId();
                    if (id != -1) {
                        MouseInput button = MouseInput.getInputFor(action.getInput().getType(), action.getInput().getId());
                        boolean consumed = sendMouseEvent(button, action.getState().isDown(), mouse.getPosition(), delta);

                        BindableButtonImpl bind = mouseButtonBinds.get(button);
                        if (bind != null) {
                            bind.updateBindState(
                                    action.getState().isDown(),
                                    delta,
                                    getInputEntities(),
                                    targetSystem.getTarget(),
                                    targetSystem.getTargetBlockPosition(),
                                    targetSystem.getHitPosition(),
                                    targetSystem.getHitNormal(),
                                    consumed
                            );
                        }
                    }
                    break;
                case MOUSE_WHEEL:
                    int dir = action.getInput().getId();
                    if (dir != 0 && action.getTurns() != 0) {
                        boolean consumed = sendMouseWheelEvent(dir * action.getTurns(), delta);

                        BindableButtonImpl bind = (dir == 1) ? mouseWheelUpBind : mouseWheelDownBind;
                        if (bind != null) {
                            for (int i = 0; i < action.getTurns(); ++i) {
                                bind.updateBindState(
                                        true,
                                        delta,
                                        getInputEntities(),
                                        targetSystem.getTarget(),
                                        targetSystem.getTargetBlockPosition(),
                                        targetSystem.getHitPosition(),
                                        targetSystem.getHitNormal(),
                                        consumed
                                );
                                bind.updateBindState(
                                        false,
                                        delta,
                                        getInputEntities(),
                                        targetSystem.getTarget(),
                                        targetSystem.getTargetBlockPosition(),
                                        targetSystem.getHitPosition(),
                                        targetSystem.getHitNormal(),
                                        consumed
                                );
                            }
                        }
                    }
                    break;
                case KEY:
                    break;
            }
        }
    }

    private void setupTarget(InputEvent event) {
        if (targetSystem.isTargetAvailable()) {
            event.setTargetInfo(targetSystem.getTarget(), targetSystem.getTargetBlockPosition(), targetSystem.getHitPosition(), targetSystem.getHitNormal());
        }
    }

    private void processKeyboardInput(float delta) {
        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();

            ButtonState state = getButtonState(Keyboard.getEventKeyState(), Keyboard.isRepeatEvent());
            boolean consumed = sendKeyEvent(key, state, delta);

            // Update bind
            BindableButtonImpl bind = keyBinds.get(key);
            if (bind != null && !Keyboard.isRepeatEvent()) {
                bind.updateBindState(
                        Keyboard.getEventKeyState(),
                        delta, getInputEntities(),
                        targetSystem.getTarget(),
                        targetSystem.getTargetBlockPosition(),
                        targetSystem.getHitPosition(),
                        targetSystem.getHitNormal(),
                        consumed
                );
            }
        }
    }

    private void processBindAxis(float delta) {
        for (BindableAxisImpl axis : axisBinds) {
            axis.update(getInputEntities(), delta, targetSystem.getTarget(), targetSystem.getTargetBlockPosition(),

                    targetSystem.getHitPosition(), targetSystem.getHitNormal());
        }
    }

    private void processBindRepeats(float delta) {
        for (BindableButtonImpl button : buttonBinds) {
            button.update(getInputEntities(), delta, targetSystem.getTarget(), targetSystem.getTargetBlockPosition(),
                    targetSystem.getHitPosition(), targetSystem.getHitNormal());
        }
    }

    private ButtonState getButtonState(boolean keyDown, boolean repeatEvent) {
        if (repeatEvent) {
            return ButtonState.REPEAT;
        }
        return (keyDown) ? ButtonState.DOWN : ButtonState.UP;
    }

    private boolean sendKeyEvent(int key, ButtonState state, float delta) {
        KeyEvent event;
        switch (state) {
            case UP:
                event = KeyUpEvent.create(key, delta);
                break;
            case DOWN:
                event = KeyDownEvent.create(key, delta);
                break;
            case REPEAT:
                event = KeyRepeatEvent.create(key, delta);
                break;
            default:
                return false;
        }
        setupTarget(event);
        for (EntityRef entity : getInputEntities()) {
            entity.send(event);
            if (event.isConsumed()) {
                break;
            }
        }

        boolean consumed = event.isConsumed();
        event.reset();
        return consumed;
    }

    private boolean sendMouseEvent(MouseInput button, boolean buttonDown, Vector2i position, float delta) {
        MouseButtonEvent event;
        switch (button) {
            case MOUSE_NONE:
                return false;
            case MOUSE_LEFT:
                event = (buttonDown) ? LeftMouseDownButtonEvent.create(position, delta) : LeftMouseUpButtonEvent.create(position, delta);
                break;
            case MOUSE_RIGHT:
                event = (buttonDown) ? RightMouseDownButtonEvent.create(position, delta) : RightMouseUpButtonEvent.create(position, delta);
                break;
            default:
                event = (buttonDown) ? MouseDownButtonEvent.create(button, position, delta) : MouseUpButtonEvent.create(button, position, delta);
                break;
        }
        setupTarget(event);
        for (EntityRef entity : getInputEntities()) {
            entity.send(event);
            if (event.isConsumed()) {
                break;
            }
        }
        boolean consumed = event.isConsumed();
        event.reset();
        return consumed;
    }

    private boolean sendMouseWheelEvent(int wheelTurns, float delta) {
        MouseWheelEvent mouseWheelEvent = new MouseWheelEvent(wheelTurns, delta);
        setupTarget(mouseWheelEvent);
        for (EntityRef entity : getInputEntities()) {
            entity.send(mouseWheelEvent);
            if (mouseWheelEvent.isConsumed()) {
                break;
            }
        }
        return mouseWheelEvent.isConsumed();
    }

    private EntityRef[] getInputEntities() {
        return new EntityRef[]{localPlayer.getClientEntity(), localPlayer.getCharacterEntity()};
    }

}

