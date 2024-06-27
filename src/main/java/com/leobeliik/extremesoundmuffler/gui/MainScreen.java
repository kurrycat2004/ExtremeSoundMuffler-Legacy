package com.leobeliik.extremesoundmuffler.gui;

import static com.leobeliik.extremesoundmuffler.SoundMuffler.renderGui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.leobeliik.extremesoundmuffler.Config;
import com.leobeliik.extremesoundmuffler.SoundMuffler;
import com.leobeliik.extremesoundmuffler.gui.buttons.MuffledSlider;
import com.leobeliik.extremesoundmuffler.interfaces.IColorsGui;
import com.leobeliik.extremesoundmuffler.interfaces.ISoundLists;
import com.leobeliik.extremesoundmuffler.utils.Anchor;
import com.leobeliik.extremesoundmuffler.utils.ComparableResource;
import com.leobeliik.extremesoundmuffler.utils.DataManager;
import com.leobeliik.extremesoundmuffler.utils.ESMButton;
import com.leobeliik.extremesoundmuffler.utils.Tips;

import cpw.mods.fml.client.config.GuiButtonExt;

public class MainScreen extends GuiScreen implements ISoundLists, IColorsGui {

    private static final Minecraft minecraft = Minecraft.getMinecraft();
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]*(?:[0-9]*)?");
    private final List<GuiButton> filteredButtons = new ArrayList<>();
    private static boolean isMuffling = true;
    private static String searchBarText = "";
    private static String screenTitle = "";
    private static String toggleSoundsListMessage;
    private final int xSize = 256;
    private final int ySize = 202;
    private final boolean isAnchorsDisabled = Config.getDisableAchors();
    private final String emptyText = "";
    private final String mainTitle = "ESM - Main Screen";
    private String tip = Tips.randomTip();
    private int minYButton, maxYButton, index;
    private GuiButton btnToggleMuffled, btnDelete, btnToggleSoundsList, btnSetAnchor, btnEditAnchor, btnNextSounds,
        btnPrevSounds, btnAccept, btnCancel;
    private GuiTextField searchBar, editAnchorTitleBar, editAnchorRadiusBar;
    private Anchor anchor;
    private final List<GuiTextField> textFields = new ArrayList<>();

    private MainScreen() {
        super();
    }

    private static void open(String title, String message, String searchMessage) {
        toggleSoundsListMessage = message;
        screenTitle = title;
        searchBarText = searchMessage;
        minecraft.displayGuiScreen(new MainScreen());
    }

    public static void open() {
        open("ESM - Main Screen", "Recent", "");
    }

    public static boolean isMuffled() {
        return isMuffling;
    }

    @Nullable
    public static Anchor getAnchorByName(String name) {
        return anchorList.stream()
            .filter(
                a -> a.getName()
                    .equals(name))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        renderGui();
        drawTexturedModalRect(getX(), getY(), 0, 0, xSize, ySize); // Main screen bounds
        drawCenteredString(fontRendererObj, screenTitle, getX() + 128, getY() + 8, whiteText); // Screen title
        renderButtonsTextures(mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        minYButton = getY() + 46;
        maxYButton = getY() + 164;

        buttonList.add(
            btnToggleSoundsList = new ESMButton(
                0,
                getX() + 13,
                getY() + 181,
                52,
                13,
                toggleSoundsListMessage,
                this::toggleSoundList));

        addSoundButtons();

        addAnchorButtons();

        buttonList.add(
            btnToggleMuffled = new ESMButton(0, getX() + 229, getY() + 179, 17, 17, () -> isMuffling = !isMuffling));
        buttonList.add(btnDelete = new ESMButton(1, getX() + 205, getY() + 179, 17, 17, () -> {
            anchor = getAnchorByName(screenTitle);
            if (clearRecentSounds()) {
                recentSoundsList.clear();
                if (screenTitle.equals(mainTitle)) {
                    open(mainTitle, btnToggleSoundsList.displayString, searchBar.getText());
                } else if (anchor != null) {
                    open(anchor.getName(), btnToggleSoundsList.displayString, searchBar.getText());
                }
                return;
            }

            if (screenTitle.equals(mainTitle)) {
                muffledSounds.clear();
                open(mainTitle, btnToggleSoundsList.displayString, searchBar.getText());
            } else {
                if (anchor != null) {
                    anchor.deleteAnchor();
                    buttonList.clear();
                    open(anchor.getName(), btnToggleSoundsList.displayString, searchBar.getText());
                }
            }
        }));

        buttonList.add(
            btnSetAnchor = new ESMButton(
                2,
                getX() + 260,
                getY() + 62,
                11,
                11,
                () -> Objects.requireNonNull(getAnchorByName(screenTitle))
                    .setAnchor()).renderNormalButton(true));

        buttonList.add(
            btnEditAnchor = new ESMButton(
                3,
                getX() + 274,
                getY() + 62,
                11,
                11,
                () -> editTitle(Objects.requireNonNull(getAnchorByName(screenTitle)))).renderNormalButton(true));

        addEditAnchorButtons();

        if (screenTitle.equals(mainTitle)) {
            btnSetAnchor.visible = false;
            btnEditAnchor.visible = false;
        }

        textFields.add(searchBar = new GuiTextField(fontRendererObj, getX() + 74, getY() + 183, 119, 13));
        searchBar.setText(searchBarText);
        searchBar.setEnableBackgroundDrawing(false);
        buttonList.add(
            btnPrevSounds = new ESMButton(
                0,
                getX() + 10,
                getY() + 22,
                13,
                20,
                () -> listScroll(
                    !searchBar.getText()
                        .isEmpty() ? filteredButtons : buttonList,
                    -1)));

        buttonList.add(
            btnNextSounds = new ESMButton(
                0,
                getX() + 233,
                getY() + 22,
                13,
                20,
                () -> listScroll(
                    !searchBar.getText()
                        .isEmpty() ? filteredButtons : buttonList,
                    1)));
        updateText();
    }

    private void addSoundButtons() {
        int buttonH = minYButton;
        anchor = getAnchorByName(screenTitle);

        if (!screenTitle.equals(mainTitle) && anchor == null) {
            return;
        }

        if (btnToggleSoundsList.displayString.equals("Recent")) {
            soundsList.clear();
            if (screenTitle.equals(mainTitle) && !muffledSounds.isEmpty()) {
                soundsList.addAll(muffledSounds.keySet());
            } else if (anchor != null && !anchor.getMuffledSounds()
                .isEmpty()) {
                    soundsList.addAll(
                        anchor.getMuffledSounds()
                            .keySet());
                }
            soundsList.addAll(recentSoundsList);
        } else if (btnToggleSoundsList.displayString.equals("All")) {
            soundsList.clear();
            ((Set<ResourceLocation>) Minecraft.getMinecraft()
                .getSoundHandler().sndRegistry.getKeys()).forEach(e -> soundsList.add(new ComparableResource(e)));
            if (Config.getLawfulAllList()) {
                forbiddenSounds.forEach(
                    fs -> soundsList.removeIf(
                        sl -> sl.toString()
                            .contains(fs)));
            }
        } else {
            soundsList.clear();
            if (screenTitle.equals(mainTitle) && !muffledSounds.isEmpty()) {
                soundsList.addAll(muffledSounds.keySet());
            } else if (anchor != null && !anchor.getMuffledSounds()
                .isEmpty()) {
                    soundsList.addAll(
                        anchor.getMuffledSounds()
                            .keySet());
                }
        }

        if (soundsList.isEmpty()) {
            return;
        }

        for (ComparableResource sound : soundsList) {

            float volume;
            float maxVolume = 1F;

            if (screenTitle.equals(mainTitle)) {
                volume = muffledSounds.get(sound) == null ? maxVolume : muffledSounds.get(sound);
            } else if (anchor != null) {
                volume = anchor.getMuffledSounds()
                    .get(sound) == null ? maxVolume
                        : anchor.getMuffledSounds()
                            .get(sound);
            } else {
                volume = maxVolume;
            }

            int x = Config.getLeftButtons() ? getX() + 36 : getX() + 11;

            MuffledSlider volumeSlider = new MuffledSlider(x, buttonH, 205, 11, volume, sound, screenTitle, anchor);

            boolean muffledAnchor = anchor != null && screenTitle.equals(anchor.getName())
                && !anchor.getMuffledSounds()
                    .isEmpty()
                && anchor.getMuffledSounds()
                    .containsKey(sound);
            boolean muffledScreen = screenTitle.equals(mainTitle) && !muffledSounds.isEmpty()
                && muffledSounds.containsKey(sound);

            if (muffledAnchor || muffledScreen) {
                volumeSlider.packedFGColour = cyanText;
            }

            buttonH += volumeSlider.height + 2;
            buttonList.add(volumeSlider);
            volumeSlider.visible = buttonList.indexOf(volumeSlider) < index + 10;
        }
    }

    private void addAnchorButtons() {
        int buttonW = getX() + 30;
        for (int i = 0; i <= 9; i++) {
            GuiButton btnAnchor;
            if (isAnchorsDisabled) {
                String[] disabledMsg = { "-", "D", "i", "s", "a", "b", "l", "e", "d", "-" };
                btnAnchor = new GuiButtonExt(0, buttonW, getY() + 24, 16, 16, disabledMsg[i]);
                btnAnchor.enabled = false;
            } else {
                int finalI = i;
                btnAnchor = new ESMButton(0, buttonW, getY() + 24, 16, 16, (String.valueOf(finalI)), () -> {
                    anchor = anchorList.get(finalI);
                    if (anchor == null) return;
                    if (screenTitle.equals(anchor.getName())) {
                        screenTitle = mainTitle;
                    } else {
                        screenTitle = anchor.getName();
                    }
                    buttonList.clear();
                    open(screenTitle, btnToggleSoundsList.displayString, searchBar.getText());
                }).setRenderText(true);

                if (!anchorList.isEmpty()) {
                    btnAnchor.packedFGColour = (anchorList.get(Integer.parseInt(btnAnchor.displayString))
                        .getAnchorPos() != null ? greenText : whiteText);
                }
            }
            buttonList.add(btnAnchor);
            buttonW += 20;
        }
    }

    private void addEditAnchorButtons() {

        textFields.add(
            editAnchorTitleBar = new GuiTextField(fontRendererObj, getX() + 302, btnEditAnchor.yPosition + 20, 84, 11));
        editAnchorTitleBar.setVisible(false);

        textFields.add(
            editAnchorRadiusBar = new GuiTextField(
                fontRendererObj,
                getX() + 302,
                editAnchorTitleBar.yPosition + 15,
                30,
                11) {

                @Override
                public void writeText(String string) {
                    if (NUMBER_PATTERN.matcher(string)
                        .matches()) {
                        super.writeText(string);
                    }
                }

                @Override
                public void setText(String p_146180_1_) {
                    if (NUMBER_PATTERN.matcher(p_146180_1_)
                        .matches()) {
                        super.setText(p_146180_1_);
                    }
                }
            });

        editAnchorRadiusBar.setVisible(false);

        btnAccept = new ESMButton(100, getX() + 259, editAnchorRadiusBar.yPosition + 15, 40, 20, "Accept", () -> {
            anchor = getAnchorByName(screenTitle);
            if (!editAnchorTitleBar.getText()
                .isEmpty()
                && !editAnchorRadiusBar.getText()
                    .isEmpty()
                && anchor != null) {
                int radius = Integer.parseInt(editAnchorRadiusBar.getText());

                if (radius > 32) {
                    radius = 32;
                } else if (radius < 1) {
                    radius = 1;
                }

                anchor.editAnchor(editAnchorTitleBar.getText(), radius);
                screenTitle = editAnchorTitleBar.getText();
                editTitle(anchor);
            }
        }).renderNormalButton(true)
            .setVisible(false);
        buttonList.add(btnAccept);
        btnCancel = new ESMButton(
            101,
            getX() + 300,
            editAnchorRadiusBar.yPosition + 15,
            40,
            20,
            "Cancel",
            () -> editTitle(Objects.requireNonNull(getAnchorByName(screenTitle)))).renderNormalButton(true)
                .setVisible(false);
        buttonList.add(btnCancel);
    }

    private void renderButtonsTextures(double mouseX, double mouseY, float partialTicks) {
        int x; // start x point of the button
        int y; // start y point of the button
        int mX; // start x point for mouse hovering
        int mY; // start y point for mouse hovering
        float v; // start x point of the texture
        String message; // Button message
        int stringW; // text width

        if (buttonList.size() < soundsList.size()) {
            return;
        }

        // Delete button
        x = btnDelete.xPosition + 8;
        y = btnDelete.yPosition;
        message = screenTitle.equals(mainTitle) ? "Delete Muffled List" : "Delete Anchor";
        stringW = fontRendererObj.getStringWidth(message) / 2;
        if (btnDelete.func_146115_a()) {
            drawRect(x - stringW - 2, y + 17, x + stringW + 2, y + 30, darkBG);
            drawCenteredString(fontRendererObj, message, x, y + 19, whiteText);
        }

        // reset recent sounds
        if (clearRecentSounds()) {
            renderGui();
            drawTexturedModalRect(x - 6, y + 2, 54, 217, 13, 13);
            message = "Clear recent sounds list";
            stringW = fontRendererObj.getStringWidth(message) / 2;
            if (btnDelete.func_146115_a()) {
                drawRect(x - stringW - 2, y + 17, x + stringW + 2, y + 30, darkBG);
                drawCenteredString(fontRendererObj, message, x, y + 19, whiteText);
            }
        }

        // toggle muffled button
        x = btnToggleMuffled.xPosition + 8;
        y = btnToggleMuffled.yPosition;
        renderGui();

        if (isMuffling) {
            drawTexturedModalRect(x - 7, y + 1, 54, 202, 15, 15); // muffle button
        }

        message = isMuffling ? "Stop Muffling" : "Start Muffling";
        stringW = fontRendererObj.getStringWidth(message) / 2;
        if (btnToggleMuffled.func_146115_a()) {
            drawRect(x - stringW - 2, y + 18, x + stringW + 2, y + 30, darkBG);
            drawCenteredString(fontRendererObj, message, x, y + 20, whiteText);
        }

        // Anchor coordinates and set coord button
        Anchor anchor = getAnchorByName(screenTitle);
        String dimensionName = "";
        String radius;
        x = btnSetAnchor.xPosition;
        y = btnSetAnchor.yPosition;

        if (anchor != null) {
            stringW = fontRendererObj.getStringWidth("Dimension: ");
            radius = anchor.getRadius() == 0 ? "" : String.valueOf(anchor.getRadius());
            if (anchor.getDimension() != null) {
                stringW += fontRendererObj.getStringWidth(anchor.getDimension());
                dimensionName = anchor.getDimension();
            }
            drawRect(x - 5, y - 56, x + stringW + 6, y + 16, darkBG);
            drawString(fontRendererObj, "X: " + anchor.getX(), x + 1, y - 50, whiteText);
            drawString(fontRendererObj, "Y: " + anchor.getY(), x + 1, y - 40, whiteText);
            drawString(fontRendererObj, "Z: " + anchor.getZ(), x + 1, y - 30, whiteText);
            drawString(fontRendererObj, "Radius: " + radius, x + 1, y - 20, whiteText);
            drawString(fontRendererObj, "Dimension: " + dimensionName, x + 1, y - 10, whiteText);
            renderGui();
            func_146110_a(x, y, 0, 69.45F, 11, 11, 88, 88); // set coordinates button

            if (anchor.getAnchorPos() != null) {
                btnEditAnchor.visible = true;
                drawTexturedModalRect(btnEditAnchor.xPosition, btnEditAnchor.yPosition, 32, 213, 11, 1); // change
                                                                                                         // title
                                                                                                         // button
            } else {
                btnEditAnchor.enabled = false;
            }

            // Indicates the Anchor has to be set before muffling sounds
            textFields.add(searchBar);
            for (GuiButton btn : buttonList) {
                if (btn instanceof MuffledSlider slider) {
                    if (slider.getBtnToggleSound()
                        .func_146115_a() && anchor.getAnchorPos() == null) {
                        drawRect(x - 5, y + 16, x + 65, y + 40, darkBG);
                        fontRendererObj.drawString("Set the", x, y + 18, whiteText);
                        fontRendererObj.drawString("Anchor first", x, y + 29, whiteText);
                    }
                } else {
                    renderGui();
                    if (btn.displayString.equals(String.valueOf(anchor.getAnchorId()))) {
                        drawTexturedModalRect(btn.xPosition - 5, btn.yPosition - 2, 71, 202, 27, 22);
                        break;
                    }
                }
            }
        }

        message = "Set Anchor";
        stringW = fontRendererObj.getStringWidth(message) + 2;

        // Set Anchor tooltip
        if (btnSetAnchor.func_146115_a() && !editAnchorTitleBar.getVisible()) {
            drawRect(x - 5, y + 16, x + stringW, y + 29, darkBG);
            fontRendererObj.drawString(message, x, y + 18, whiteText);
        }

        message = "Edit Anchor";
        stringW = fontRendererObj.getStringWidth(message) + 2;

        if (btnEditAnchor.visible && !editAnchorTitleBar.getVisible() && btnEditAnchor.func_146115_a()) {
            drawRect(x - 5, y + 16, x + stringW + 2, y + 29, darkBG);
            fontRendererObj.drawString(message, x, y + 18, whiteText);
        }

        // draw anchor buttons tooltip
        for (int i = 0; i <= 9; i++) {
            GuiButton btn = buttonList.get(soundsList.size() + 1 + i);
            x = btn.xPosition + 8;
            y = btn.yPosition + 5;
            message = isAnchorsDisabled ? "Anchors are disabled"
                : anchorList.get(i)
                    .getName();
            stringW = fontRendererObj.getStringWidth(message) / 2;

            if (btn.func_146115_a()) {
                drawRect(x - stringW - 2, y - 2, x + stringW + 2, y - 13, darkBG);
                drawCenteredString(fontRendererObj, message, x, y - 11, whiteText);
            }
        }

        // Toggle List button draw message
        x = btnToggleSoundsList.xPosition;
        y = btnToggleSoundsList.yPosition;
        message = btnToggleSoundsList.displayString;
        int centerText = x + (btnToggleSoundsList.width / 2) - (fontRendererObj.getStringWidth(message) / 2);
        fontRendererObj.drawString(message, centerText, y + 3, 0);
        String text = "Showing " + message + " sounds";
        int textW = fontRendererObj.getStringWidth(text);
        int textX = x + (btnToggleSoundsList.width / 2) - (textW / 2) + 6;

        if (btnToggleSoundsList.func_146115_a()) {
            drawRect(textX - 2, y + 14, textX + textW + 2, y + 18 + fontRendererObj.FONT_HEIGHT, darkBG);
            fontRendererObj.drawString(text, textX, y + 16, whiteText);
        }

        // Show Radius and Title text when editing Anchor and bg
        x = btnSetAnchor.xPosition;
        y = editAnchorTitleBar.yPosition;
        if (editAnchorRadiusBar.getVisible()) {
            drawRect(
                x - 4,
                y - 4,
                editAnchorTitleBar.xPosition + editAnchorTitleBar.getWidth() + 3,
                btnAccept.yPosition + 23,
                darkBG);
            fontRendererObj.drawString("Title: ", x - 2, y + 1, whiteText);
            fontRendererObj.drawString("Radius: ", x - 2, editAnchorRadiusBar.yPosition + 1, whiteText);

            x = editAnchorRadiusBar.xPosition + editAnchorRadiusBar.getWidth();
            y = editAnchorRadiusBar.yPosition;
            message = "Range: 1 - 32";
            stringW = fontRendererObj.getStringWidth(message);
            if (editAnchorRadiusBar.isFocused()) {
                drawRect(x + 3, y, x + stringW + 6, y + 12, darkBG);
                fontRendererObj.drawString(message, x + 5, y + 2, whiteText);
            }
        }

        // Draw Searchbar prompt text
        x = searchBar.xPosition;
        y = searchBar.yPosition;
        String searchHint = "Search";
        if (!this.searchBar.isFocused() && this.searchBar.getText()
            .isEmpty()) {
            drawString(fontRendererObj, searchHint, x + 1, y + 1, -1);
        }

        // next sounds button tooltip
        x = btnNextSounds.xPosition;
        y = btnNextSounds.yPosition;
        message = "Next Sounds";
        stringW = fontRendererObj.getStringWidth(message) / 2;

        if (btnNextSounds.func_146115_a()) {
            drawRect(x - stringW - 2, y - 2, x + stringW + 2, y - 13, darkBG);
            drawCenteredString(fontRendererObj, message, x, y - 11, whiteText);
        }

        // previous sounds button tooltip
        x = btnPrevSounds.xPosition;
        y = btnPrevSounds.yPosition;
        message = "Previous Sounds";
        stringW = fontRendererObj.getStringWidth(message) / 2;

        if (btnPrevSounds.func_146115_a()) {
            drawRect(x - stringW - 2, y - 2, x + stringW + 2, y - 13, darkBG);
            drawCenteredString(fontRendererObj, message, x, y - 11, whiteText);
        }

        // highlight every other row
        for (int i = 0; i < buttonList.size(); i++) {
            GuiButton button = buttonList.get(i);
            if (button instanceof MuffledSlider) {
                x = Config.getLeftButtons() ? button.xPosition - 3 : button.xPosition + 1;
                y = button.yPosition;
                int bW = Config.getLeftButtons() ? x + button.width + 5 : x + button.width + 28;

                if (i % 2 == 0 && button.visible) {
                    drawRect(x, y, bW, y + button.height, brightBG);
                }
            }
        }

        for (GuiTextField textField : textFields) {
            if (!textField.getVisible()) continue;
            textField.drawTextBox();
        }

        // Show a tip
        if (Config.getShowTip()) {
            renderTips(Collections.singletonList(tip));
        }
    }

    private boolean clearRecentSounds() {
        return btnToggleSoundsList.displayString.equals("Recent") && isShiftKeyDown();
    }

    private void renderTips(List<String> tips) {
        drawHoveringText(tips, getX() - 5, getY() + 223, fontRendererObj);
        // GuiUtils.drawHoveringText(ms, tips, getX() - 5, getY() + 223, width, h, 245, font);
    }

    private void editTitle(Anchor anchor) {
        editAnchorTitleBar.setText(anchor.getName());
        editAnchorTitleBar.setVisible(!editAnchorTitleBar.getVisible());

        editAnchorRadiusBar.setText(String.valueOf(anchor.getRadius()));
        editAnchorRadiusBar.setVisible(!editAnchorRadiusBar.getVisible());;

        btnAccept.visible = !btnAccept.visible;
        btnCancel.visible = !btnCancel.visible;

        editAnchorRadiusBar.setTextColor(whiteText);
    }

    @Override
    public void handleMouseInput() {
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            if (searchBar.getText()
                .isEmpty()) {
                listScroll(buttonList, scroll * -1);
            } else {
                listScroll(filteredButtons, scroll * -1);
            }
        }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private void listScroll(List<GuiButton> buttonList, double direction) {
        int buttonH = minYButton;

        if (index <= 0 && direction < 0) {
            return;
        }

        if ((index >= buttonList.size() - 10 || index >= soundsList.size() - 10) && direction > 0) {
            return;
        }

        index += direction > 0 ? 10 : -10;

        for (GuiButton btn : buttonList) {
            if (btn instanceof MuffledSlider slider) {
                int buttonIndex = buttonList.indexOf(btn);
                btn.visible = buttonIndex < index + 10 && buttonIndex >= index;

                if (btn.visible) {
                    btn.yPosition = buttonH;
                    buttonH += btn.height + 2;
                }
                slider.refreshButtons();
            }
        }
    }

    private void updateText() {
        int buttonH = minYButton;
        filteredButtons.clear();

        for (GuiButton button : buttonList) {
            if (button instanceof MuffledSlider btn) {
                if (btn.displayString.contains(
                    searchBar.getText()
                        .toLowerCase())) {
                    if (!filteredButtons.contains(btn)) {
                        filteredButtons.add(btn);
                    }

                    btn.yPosition = buttonH;
                    buttonH += btn.height + 2;

                    btn.visible = btn.yPosition < maxYButton;
                } else {
                    btn.visible = false;
                }
                btn.refreshButtons();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (searchBar.textboxKeyTyped(typedChar, keyCode)) {
            updateText();
            return;
        }

        if (editAnchorTitleBar.textboxKeyTyped(typedChar, keyCode)
            || editAnchorRadiusBar.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }

        // Search bar, Edit title bar & Edit Anchor Radius bar looses focus when pressed "Enter" or "Intro"
        if (keyCode == 257 || keyCode == 335) {
            searchBar.setFocused(false);
            editAnchorTitleBar.setFocused(false);
            editAnchorRadiusBar.setFocused(false);
            return;
        }

        // Close screen when press "E" or the mod hotkey outside the search bar or edit title bar
        if (!searchBar.isFocused() && !editAnchorTitleBar.isFocused()
            && !editAnchorRadiusBar.isFocused()
            && (minecraft.gameSettings.keyBindInventory.getKeyCode() == keyCode
                || keyCode == SoundMuffler.getHotkey())) {
            this.onGuiClosed();
            filteredButtons.clear();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (GuiTextField textField : textFields) {
            if (textField.getVisible()) {
                textField.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        // if (mouseButton == 1) {
        if (searchBar.isFocused()) {
            searchBar.setText("");
            updateText();
            return;
        }

        if (editAnchorTitleBar.isFocused()) {
            editAnchorTitleBar.setText("");
            return;
        }

        if (editAnchorRadiusBar.isFocused()) {
            editAnchorRadiusBar.setText("");
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (state == 1) {
            MuffledSlider.showSlider = false;
            MuffledSlider.tickSound = null;
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    @Override
    public void updateScreen() {
        for (GuiTextField textField : textFields) {
            textField.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        DataManager.saveData();
        super.onGuiClosed();
    }

    public static String getScreenTitle() {
        return screenTitle;
    }

    private int getX() {
        return (this.width - xSize) / 2;
    }

    private int getY() {
        return (this.height - ySize) / 2;
    }

    private void toggleSoundList() {
        boolean isAnchorMuffling = false;

        if (!screenTitle.equals(mainTitle)) {
            isAnchorMuffling = !Objects.requireNonNull(getAnchorByName(screenTitle))
                .getMuffledSounds()
                .isEmpty();
        }

        if (btnToggleSoundsList.displayString.equals("Recent")) {
            toggleSoundsListMessage = "All";
        } else if (btnToggleSoundsList.displayString.equals("All")) {
            if (!muffledSounds.isEmpty() || isAnchorMuffling) {
                toggleSoundsListMessage = "Muffled";
            } else {
                toggleSoundsListMessage = "Recent";
            }
        } else {
            toggleSoundsListMessage = "Recent";
        }

        btnToggleSoundsList.displayString = toggleSoundsListMessage;
        buttonList.clear();
        open(screenTitle, toggleSoundsListMessage, searchBar.getText());
    }
}
