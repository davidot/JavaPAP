import java.awt.Component;
import java.awt.Point;
import java.awt.event.*;
import java.util.*;

/**
 * Input is a utility class for providing easy access to all kinds of user input events
 * @author davidot
 */
public class Input implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    /**
     * Name of the default key for the left mouse button
     * @see #getKey(String) with this
     * @see #getLeftMouseButton()
     */
    public static final String LEFT_MOUSE_BUTTON_NAME = "lmb";

    /**
     * Name of the default key for the right mouse button
     * @see #getKey(String) with this
     * @see #getRightMouseButton()
     */
    public static final String RIGHT_MOUSE_BUTTON_NAME = "rmb";

    /**
     * Prefix when saving inputs
     */
    public static final char KEY_TYPE_CHAR = 'k';
    /**
     * Prefix when saving inputs
     */
    public static final char MOUSE_TYPE_CHAR = 'm';
    /**
     * Prefix when saving inputs
     */
    public static final char WHEEL_TYPE_CHAR = 'w';
    /**
     * Separator when saving inputs
     */
    public static final String INPUT_SEPARATOR = ",";

    /**
     * Separator between keys
     */
    public static final char KEY_END = ';';

    /**
     * Separator between key name and inputs
     */
    public static final String NAME_SEPARATOR = "=";

    private List<Key> keys = new ArrayList<Key>();

    private Key leftMouseButton;
    private Key rightMouseButton;

    //start at 0,0
    private int x;
    private int y;

    boolean anyKeyOn = false;
    private AnyInput currentAnyInput;

    private List<WheelListener> wheelListeners = new ArrayList<WheelListener>();

    private List<KeyToggle> keyToggles = new ArrayList<KeyToggle>();//todo

    /**
     * Create a Input which will listen on the {@link Component} specified
     * <p>
     *     If comp is {@code null} it will not listen to any events.
     * </p>
     * @param comp the component where to listen to
     */
    public Input(Component comp) {
        this(comp, true);
    }

    /**
     * Create a Input which will listen on the {@link Component} specified
     * <p>
     *     If comp is {@code null} it will not listen to any events.
     *     If mouseKeys is {@code false} this means that {@link #getLeftMouseButton()} and
     *     {@link #getRightMouseButton()} are null
     * </p>
     * @param comp the component where to listen to
     * @param mouseKeys whether to create the default left and right mouse button keys
     */
    public Input(Component comp, boolean mouseKeys) {

        if(comp != null) {
            comp.addKeyListener(this);
            comp.addMouseListener(this);
            comp.addMouseMotionListener(this);
            comp.addMouseWheelListener(this);
            setPoint(comp.getMousePosition());
        }

        if(mouseKeys) {
            leftMouseButton =
                    new Key(LEFT_MOUSE_BUTTON_NAME, asInputtableList(new MouseButtonInput(1)),
                            false);
            rightMouseButton =
                    new Key(RIGHT_MOUSE_BUTTON_NAME, asInputtableList(new MouseButtonInput(1)),
                            false);
        }
    }

    /**
     * The types of input used for {@link net.dinster.lightbringer.util.Input.AnyInput}
     */
    public enum InputType {
        /**
         * Accept any
         */
        ALL,
        /**
         * Accept only Mouse Button presses
         */
        MOUSE_BUTTON,
        /**
         * Accept only Mouse Wheel movement
         */
        MOUSE_WHEEL,
        /**
         * Accept only Key presses
         */
        KEY

    }

    /**
     * A Interface for accepting any input
     */
    public interface AnyInput {

        /**
         * When a MouseEvent is called
         * @param event the event
         * @return whether this is a accepted event
         */
        boolean onMousePressed(MouseEvent event);

        /**
         * When a WheelMouseEvent is called
         * @param event the event
         * @return whether this is a accepted event
         */
        boolean onWheelScrolled(MouseWheelEvent event);

        /**
         * When a KeyEvent is called
         * @param event the event
         * @return whether this is a accepted event
         */
        boolean onKeyPressed(KeyEvent event);

        /**
         * The Types will which might be accepted
         * @return the type
         */
        InputType getType();

    }

    /**
     * A AnyKey Will only listen to Key events
     * @see net.dinster.lightbringer.util.Input.AnyInput for other events
     */
    public static abstract class AnyKey implements AnyInput {

        @Override
        public boolean onMousePressed(MouseEvent event) {
            return false;
        }

        @Override
        public boolean onWheelScrolled(MouseWheelEvent event) {
            return false;
        }
        @Override
        public InputType getType() {
            return InputType.KEY;
        }

    }

    private interface UserInput extends AnyInput {

        String getDisplayName();

        String toSaveFormat();

    }

    private static class KeyInput implements UserInput {

        private final int code;

        private KeyInput(int code) {
            this.code = code;
        }

        private KeyInput(String loadData) {
            try {
                code = Integer.parseInt(loadData);
            } catch(NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public String getDisplayName() {
            return KeyEvent.getKeyText(code);
        }

        @Override
        public String toSaveFormat() {
            return KEY_TYPE_CHAR + String.valueOf(code);
        }

        @Override
        public boolean onMousePressed(MouseEvent event) {
            return false;
        }

        @Override
        public boolean onWheelScrolled(MouseWheelEvent event) {
            return false;
        }

        @Override
        public boolean onKeyPressed(KeyEvent event) {
            return event.getKeyCode() == code;
        }

        @Override
        public InputType getType() {
            return InputType.KEY;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }

            KeyInput keyInput = (KeyInput) o;

            return code == keyInput.code;
        }
        @Override
        public int hashCode() {
            return code;
        }

    }

    private enum WheelSide {
        UP("Up"),
        DOWN("Down");

        private final String name;
        WheelSide(String name) {
            this.name = name;
        }

    }
    private static final WheelInput WHEEL_UP_INPUT = new WheelInput(WheelSide.UP);

    private static final WheelInput WHEEL_DOWN_INPUT = new WheelInput(WheelSide.DOWN);

    private static class WheelInput implements UserInput {

        private final WheelSide side;

        private WheelInput(WheelSide side) {
            this.side = side;
        }

        private WheelInput(String loadData) {
            for(WheelSide side : WheelSide.values()) {
                if(side.name.equalsIgnoreCase(loadData)) {
                    this.side = side;
                    return;
                }
            }
            throw new IllegalArgumentException("Not a valid side:" + loadData);
        }

        @Override
        public String getDisplayName() {
            return "Scroll wheel" + side.name;
        }

        @Override
        public String toSaveFormat() {
            return WHEEL_TYPE_CHAR + side.name;
        }

        @Override
        public boolean onMousePressed(MouseEvent event) {
            return false;
        }

        @Override
        public boolean onWheelScrolled(MouseWheelEvent event) {
            return side == WheelSide.DOWN && event.getUnitsToScroll() > 0 ||
                    side == WheelSide.UP && event.getUnitsToScroll() < 0;
        }

        @Override
        public boolean onKeyPressed(KeyEvent event) {
            return false;
        }

        @Override
        public InputType getType() {
            return InputType.MOUSE_WHEEL;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }

            WheelInput that = (WheelInput) o;

            return side == that.side;
        }
        @Override
        public int hashCode() {
            return side.hashCode();
        }

    }

    private static class MouseButtonInput implements UserInput {

        private final int code;

        private MouseButtonInput(int code) {
            this.code = code;
        }

        private MouseButtonInput(String loadData) {
            try {
                code = Integer.parseInt(loadData);
            } catch(NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public String getDisplayName() {
            return "Mouse" + code;
        }

        @Override
        public String toSaveFormat() {
            return MOUSE_TYPE_CHAR + String.valueOf(code);
        }

        @Override
        public boolean onMousePressed(MouseEvent event) {
            return event.getButton() == code;
        }

        @Override
        public boolean onWheelScrolled(MouseWheelEvent event) {
            return false;
        }

        @Override
        public boolean onKeyPressed(KeyEvent event) {
            return false;
        }

        @Override
        public InputType getType() {
            return InputType.MOUSE_BUTTON;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }

            MouseButtonInput that = (MouseButtonInput) o;

            return code == that.code;

        }
        @Override
        public int hashCode() {
            return code;
        }

    }

    private static UserInput loadKey(String data) {
        if(data.length() < 2) {
            throw new IllegalArgumentException("Not enough info in saved key:" + data);
        }
        char type = data.charAt(0);
        String loadData = data.substring(1);
        switch(type) {
            case MOUSE_TYPE_CHAR:
                return new MouseButtonInput(loadData);
            case KEY_TYPE_CHAR:
                return new KeyInput(loadData);
            case WHEEL_TYPE_CHAR:
                return new WheelInput(loadData);
            default:
                throw new IllegalArgumentException("Not a valid type:" + data);
        }
    }

    private static List<UserInput> asInputtableList(UserInput... userInputs) {
        return Arrays.asList(userInputs);
    }


    public Key getLeftMouseButton() {
        return leftMouseButton;
    }

    public Key getRightMouseButton() {
        return rightMouseButton;
    }

    //returns null when we can't create the key

    /**
     * Get a Key or create it when it is not there yet
     * @param name the name of the Key
     * @param inputs the inputs to create the Key with if it does not exist
     * @return the Key found or created
     */
    public Key getOrCreateKey(String name, String inputs) {
        Key get = getKey(name);
        if(get != null) {
            return get;
        }
        String[] data = inputs.split(INPUT_SEPARATOR);
        List<UserInput> userInputs = new ArrayList<UserInput>(data.length);
        for(String aData : data) {
            try {
                userInputs.add(loadKey(aData));
            } catch(IllegalArgumentException ignored) {
            }
        }
        if(userInputs.size() <= 0) {
            return null;
        }
        return new Key(name, userInputs);
    }

    /**
     * Get the Key with the specified name
     * @param name the name of the Key
     * @return the Key with that name or {@code null} if none such Key exists
     */
    public Key getKey(String name) {
        for(Key key : keys) {
            if(key.name.equals(name)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Tick the Input
     * This is used to check whether a Key is clicked
     * @see net.dinster.lightbringer.util.Input.Key for info what clicked is
     *
     */
    public void tick() {
        for(Key key : keys) {
            key.tick();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Reset all the keys to a non pressed state
     */
    public void unPressAll() {
        for(Key key : keys) {
            key.pressed = false;
            key.clicked = false;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        onKey(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        onKey(e, false);
    }

    private void onKey(KeyEvent event, boolean state) {
        if(anyKeyOn && state) {
            if(currentAnyInput.onKeyPressed(event)) {
                anyKeyOn = false;
                currentAnyInput = null;
            }
            return;
        }
        for(Key key : keys) {
            for(UserInput input : key.inputs) {
                if(input.getType() == InputType.KEY || input.getType() == InputType.ALL) {
                    if(input.onKeyPressed(event)) {
                        key.toggle(state);
                    }
                }
            }
        }
    }

    private void onMouseWheel(MouseWheelEvent event) {
        if(anyKeyOn) {
            if(currentAnyInput.onWheelScrolled(event)) {
                anyKeyOn = false;
                currentAnyInput = null;
            }
            return;
        }
        for(Key key : keys) {
            for(UserInput input : key.inputs) {
                if(input.getType() == InputType.MOUSE_WHEEL || input.getType() == InputType.ALL) {
                    if(input.onMousePressed(event)) {
                        key.pressOnce();
                    }
                }
            }
        }
    }

    private void onMouseButton(MouseEvent event, boolean state) {
        if(anyKeyOn && state) {
            if(currentAnyInput.onMousePressed(event)) {
                anyKeyOn = false;
                currentAnyInput = null;
            }
            return;
        }
        for(Key key : keys) {
            for(UserInput input : key.inputs) {
                if(input.getType() == InputType.MOUSE_BUTTON || input.getType() == InputType.ALL) {
                    if(input.onMousePressed(event)) {
                        key.toggle(state);
                    }
                }
            }
        }
    }

    /**
     * Add a {@link WheelListener}
     * @param list the WheelListener to add
     */
    public void addWheelListener(WheelListener list) {
        wheelListeners.add(list);
    }

    /**
     * Remove a {@link WheelListener}
     * @param list the WheelListener to remove
     */
    public void removeWheelListener(WheelListener list) {
        wheelListeners.remove(list);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        setPoint(e.getPoint());
        for(WheelListener list : wheelListeners) {
            list.onScroll(e.getWheelRotation());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setPoint(e.getPoint());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        setPoint(e.getPoint());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        setPoint(e.getPoint());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        setPoint(e.getPoint());
        onMouseButton(e, true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        setPoint(e.getPoint());
        onMouseButton(e, false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setPoint(e.getPoint());
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setPoint(e.getPoint());
        unPressAll();
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public void setPoint(Point p) {
        if(p == null) {
            return;
        }
        x = p.x;
        y = p.y;
    }

    public void setAnyInput(AnyInput event) {
        currentAnyInput = event;
        anyKeyOn = true;
    }

    /**
     * Remove any anyKey events which are activated
     */
    public void closeAnyKey() {
        currentAnyInput = null;
        anyKeyOn = false;
    }

    /**
     * Add A KeyListener which listens to specific keys to toggle
     * @param keyToggle the KeyToggleListener to call
     * @param keys the Key to listen for
     */
    public void addKeyListener(KeyToggleListener keyToggle, Key... keys) {
        keyToggles.add(new KeyToggle(keyToggle, keys));
    }

    /**
     * Remove Keys from a KeyToggleListener
     * <p>
     *     If no keys are left the whole KeyToggleListener will be removed
     * </p>
     * @param keyToggle the KeyToggleListener to remove keys from
     * @param keys the keys to remove
     */
    public void removeKeyListener(KeyToggleListener keyToggle, Key... keys) {
        for(Iterator<KeyToggle> iterator = keyToggles.iterator(); iterator.hasNext(); ) {
            KeyToggle toggle = iterator.next();
            if(toggle.listener == keyToggle) {
                for(Key key : keys) {
                    toggle.keys.remove(key);
                }
                if(toggle.keys.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Remove a KeyToggleListener
     * @param keyToggle the KeyToggleListener to remove
     */
    public void removeKeyListener(KeyToggleListener keyToggle) {
        keyToggles.remove(keyToggle);
    }

    /**
     * A WheelListener will receive calls when the scroll wheel is moved
     */
    public interface WheelListener {

        /**
         * gives amount of scrolls negative is up positve is down
         * @param change the amount of movement from the mousewheel
         */
        void onScroll(int change);

    }

    private class KeyToggle {

        private final KeyToggleListener listener;
        //we suspect to only need one key
        List<Key> keys = new ArrayList<Key>(1);

        private KeyToggle(KeyToggleListener listener, Key[] listenKeys) {
            this.listener = listener;

            Collections.addAll(this.keys, listenKeys);
        }

        private boolean hasKey(Key key) {
            return keys.contains(key);
        }

        private boolean call(boolean state) {
            return listener.onKeyToggle(state);
        }

        public boolean willConsume(boolean state) {
            return listener.shouldConsume(state);
        }
    }

    /**
     * A KeyToggleListener will receive calls when one of the Key specified is pressed
     * @see #addKeyListener(KeyToggleListener, Key...)
     */
    public interface KeyToggleListener {


        /**
         * Whether the key press should be consumed so that the normal keys don't get trigged
         * @param state the new state of the key {@code true} when the key is pressed down {@code
         *              false} when the key is released
         *
         * @return whether the key should be consumed
         */
        boolean shouldConsume(boolean state);

        /**
         * Called when the keys to which this Listener listens
         * @param state the new state of the key {@code true} when the key is pressed down {@code
         *              false} when the key is released
         *
         * @return whether the Listener should still be listening
         */
        boolean onKeyToggle(boolean state);

    }

    /*
     * called when a key is pressed add this with (Input.setAnyInput(AnyKey event));
     * @param event the keyevent which is trigged
     *
     * @return wheter to stop listening
     */

    /**
     * Load a Key from a String
     * @param data the String to load from
     * @return the Key created or {@code null} if no key could be created or if a key with that name
     *  already exits then the already present key
     */
    public Key fromString(String data) {
        if(data.length() < 3) {
            return null;
        }
        String[] parts = data.split(NAME_SEPARATOR);
        if(parts.length < 2) {
            return null;
        }
        String name = parts[0];
        return getOrCreateKey(name, parts[1]);
    }

    /**
     * A Key can be assinged inputs on which it will react
     * <p>
     *     When pressed {@link Key#isPressed()} it means any of the inputs is being pressed right
     *     now.
     *     When clicked {@link Key#isClicked()} it means the input was pressed this last tick
     * </p>
     */
    public class Key {
        private final boolean saveable;
        private boolean pressed;
        private boolean clicked;
        private int once;
        private int presses;
        private int pressdone;
        private final String name;
        private Set<UserInput> inputs = new HashSet<UserInput>();

        private Key(String name, List<UserInput> userInputs) {
            this(name, userInputs, true);
        }

        private Key(String name, List<UserInput> userInputs, boolean saveable) {
            this.name = name;
            this.saveable = saveable;
            inputs.addAll(userInputs);
            keys.add(this);
        }

        private void pressOnce() {
            toggle(true);
            once = 2;
        }

        void toggle(boolean in) {
            once = 0;
            if(in != pressed) {
                pressed = in;
            }
            if(in) {
                presses++;
            }
            for(Iterator<KeyToggle> iter = keyToggles.iterator(); iter.hasNext(); ) {
                KeyToggle toggle = iter.next();
                if(toggle.hasKey(this)) {
                    if(!toggle.call(pressed)) {
                        iter.remove();
                    }
                    //if consumed was already true keep it that way
                    if(toggle.willConsume(pressed)) {
                        //don't count this clicked
                        presses--;
                    }
                }
            }
        }

        /**
         * Call to update the Key
         * <p>
         *     When a Key has just been pressed this will activate the clicked state
         * </p>
         */
        public void tick() {
            if(pressdone < presses) {
                pressdone++;
                clicked = true;
            } else {
                clicked = false;
            }
            if(once > 0 && pressed) {
                once--;
                if(once <= 0) {
                    pressed = false;
                }
            }
        }

        public boolean isPressed() {
            return pressed;
        }

        public boolean isClicked() {
            return clicked;
        }

        public boolean isSaveable() {
            return saveable;
        }

        public String getName() {
            return name;
        }

        private void addInput(UserInput input) {
            inputs.add(input);
        }

        /**
         * Will capture the next input (mouse button, mouse wheel or keyboard) and add it as an
         * input to this key
         */
        public void addNextInput() {
            setAnyInput(new InputtableCreator(this));
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }

            Key k = (Key) o;

            return name.equals(k.getName());
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        /**
         * Check if this key has the keycode from the keyboard as input
         * @param keyCode the keycode of the keyboard
         * @return whether it has a input with that keycode
         */
        public boolean hasKey(int keyCode) {
            for(UserInput input : inputs) {
                if(input instanceof KeyInput) {
                    if(((KeyInput) input).code == keyCode) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Create a savable {@link String} for this Key
         * @return the String to save for this Key
         */
        public String save() {
            StringBuilder builder = new StringBuilder(name.length() + inputs.size() * 4);
            builder.append(name).append(NAME_SEPARATOR);
            for(UserInput input: inputs) {
                builder.append(input.toSaveFormat()).append(INPUT_SEPARATOR);
            }
            return builder.append(KEY_END).toString();

        }

    }

    private class InputtableCreator implements AnyInput {
        private final Key key;

        public InputtableCreator(Key key) {
            this.key = key;
        }

        @Override
        public boolean onMousePressed(MouseEvent event) {
            key.addInput(new MouseButtonInput(event.getButton()));
            return true;
        }

        @Override
        public boolean onWheelScrolled(MouseWheelEvent event) {
            key.addInput(event.getUnitsToScroll() < 0 ? WHEEL_UP_INPUT : WHEEL_DOWN_INPUT);
            return true;
        }

        @Override
        public boolean onKeyPressed(KeyEvent event) {
            key.addInput(new KeyInput(event.getKeyCode()));
            return true;
        }

        @Override
        public InputType getType() {
            return InputType.ALL;
        }
    }
}