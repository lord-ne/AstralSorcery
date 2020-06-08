/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.client.screen.journal;

import com.mojang.blaze3d.systems.RenderSystem;
import hellfirepvp.astralsorcery.client.ClientScheduler;
import hellfirepvp.astralsorcery.client.lib.TexturesAS;
import hellfirepvp.astralsorcery.client.screen.journal.page.RenderablePage;
import hellfirepvp.astralsorcery.client.util.RenderingDrawUtils;
import hellfirepvp.astralsorcery.client.util.RenderingGuiUtils;
import hellfirepvp.astralsorcery.client.util.RenderingUtils;
import hellfirepvp.astralsorcery.common.data.journal.JournalPage;
import hellfirepvp.astralsorcery.common.data.research.ResearchNode;
import hellfirepvp.astralsorcery.common.lib.SoundsAS;
import hellfirepvp.astralsorcery.common.util.sound.SoundHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ScreenJournalPages
 * Created by HellFirePvP
 * Date: 03.08.2019 / 16:54
 */
public class ScreenJournalPages extends ScreenJournal {

    private static ScreenJournalPages openGuiInstance;
    private static boolean saveSite = true;

    @Nullable
    private final ScreenJournalProgression origin;
    @Nullable
    private final Screen previous;
    private final ResearchNode researchNode;
    private List<RenderablePage> pages;

    private boolean informPreviousClose = true;
    private int currentPageOffset = 0; //* 2 = left page.
    private Rectangle rectBack, rectNext, rectPrev;

    public ScreenJournalPages(@Nullable ScreenJournalProgression origin, ResearchNode node) {
        super(node.getName(), NO_BOOKMARK);
        this.researchNode = node;
        this.origin = origin;
        this.previous = null;
        List<JournalPage> pageList = node.getPages();
        this.pages = new ArrayList<>(pageList.size());
        for (int i = 0; i < pageList.size(); i++) {
            this.pages.add(pageList.get(i).buildRenderPage(node, i));
        }
    }

    //Use this to use this screen independently of the actual journal.
    public ScreenJournalPages(@Nullable Screen previous, ResearchNode detailedInformation, int exactPage) {
        super(detailedInformation.getName(), NO_BOOKMARK);
        this.researchNode = detailedInformation;
        this.origin = null;
        this.previous = previous;
        this.currentPageOffset = exactPage / 2;
        List<JournalPage> pageList = detailedInformation.getPages();
        this.pages = new ArrayList<>(pageList.size());
        for (int i = 0; i < pageList.size(); i++) {
            this.pages.add(pageList.get(i).buildRenderPage(detailedInformation, i));
        }
    }

    public static ScreenJournalPages getClearOpenGuiInstance() {
        ScreenJournalPages gui = openGuiInstance;
        openGuiInstance = null;
        return gui;
    }

    public int getCurrentPageOffset() {
        return currentPageOffset;
    }

    public ResearchNode getResearchNode() {
        return researchNode;
    }

    @Override
    public void init() {
        super.init();

        if (origin != null) {
            origin.preventRefresh();
            origin.setSize(width, height);
            origin.init();
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float pTicks) {
        super.render(mouseX, mouseY, pTicks);

        if (origin != null) {
            drawDefault(TexturesAS.TEX_GUI_BOOK_BLANK, mouseX, mouseY);
        } else {
            drawWHRect(TexturesAS.TEX_GUI_BOOK_BLANK);
        }

        this.changeZLevel(100);
        int pageYOffset = 20;

        //Draw headline
        if (this.currentPageOffset == 0) {
            RenderSystem.pushMatrix();

            String headline = this.getTitle().getFormattedText();
            double width = font.getStringWidth(headline);
            RenderSystem.translated(guiLeft + 117, guiTop + 22, 0);
            RenderSystem.scaled(1.3, 1.3, 1.3);
            RenderSystem.translated(-(width / 2), 0, 0);
            RenderingDrawUtils.renderStringAtCurrentPos(font, headline, 0x00DDDDDD);

            RenderSystem.popMatrix();

            TexturesAS.TEX_GUI_BOOK_UNDERLINE.bindTexture();
            RenderingGuiUtils.drawRect(guiLeft + 30, guiTop + 35, this.getGuiZLevel(), 175, 6);

            pageYOffset += 30;
        }

        int index = currentPageOffset * 2;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            page.render    (guiLeft + 30, guiTop + pageYOffset, pTicks, this.getGuiZLevel(), mouseX, mouseY);
        }
        index = index + 1;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            page.render    (guiLeft + 220, guiTop + 20, pTicks, this.getGuiZLevel(), mouseX, mouseY);
        }

        drawBackArrow(pTicks, mouseX, mouseY);
        drawNavArrows(pTicks, mouseX, mouseY);

        index = currentPageOffset * 2;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            page.postRender(guiLeft + 30, guiTop + pageYOffset, pTicks, this.getGuiZLevel(), mouseX, mouseY);
        }
        index = index + 1;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            page.postRender(guiLeft + 220, guiTop + 20, pTicks, this.getGuiZLevel(), mouseX, mouseY);
        }
        this.changeZLevel(-100);
    }

    private void drawBackArrow(float partialTicks, int mouseX, int mouseY) {
        int width = 30;
        int height = 15;
        rectBack = new Rectangle(guiLeft + 197, guiTop + 230, width, height);

        RenderSystem.pushMatrix();
        RenderSystem.translated(rectBack.getX() + (width / 2F), rectBack.getY() + (height / 2F), 0);

        float uFrom, vFrom = 0.5F;
        if (rectBack.contains(mouseX, mouseY)) {
            uFrom = 0.5F;
            RenderSystem.scaled(1.1, 1.1, 1.1);
        } else {
            uFrom = 0F;
            double t = ClientScheduler.getClientTick() + partialTicks;
            float sin = ((float) Math.sin(t / 4F)) / 32F + 1F;
            RenderSystem.scaled(sin, sin, sin);
        }
        RenderSystem.translated(-(width / 2F), -(height / 2F), 0);
        TexturesAS.TEX_GUI_BOOK_ARROWS.bindTexture();
        RenderingUtils.draw(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX, buf -> {
            RenderingGuiUtils.rect(buf, this)
                    .at(0, 0)
                    .dim(width, height)
                    .tex(uFrom, vFrom, 0.5F, 0.5F)
                    .color(1F, 1F, 1F, 0.8F)
                    .draw();
        });

        RenderSystem.popMatrix();

        RenderSystem.enableDepthTest();
    }

    private void drawNavArrows(float partialTicks, int mouseX, int mouseY) {
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        RenderSystem.disableDepthTest();

        int cIndex = currentPageOffset * 2;
        rectNext = null;
        rectPrev = null;

        if (cIndex > 0) {
            int width = 30;
            int height = 15;
            rectPrev = new Rectangle(guiLeft + 25, guiTop + 220, width, height);
            RenderSystem.pushMatrix();
            RenderSystem.translated(rectPrev.getX() + (width / 2F), rectPrev.getY() + (height / 2F), 0);
            float uFrom = 0F, vFrom = 0.5F;
            if (rectPrev.contains(mouseX, mouseY)) {
                uFrom = 0.5F;
                RenderSystem.scaled(1.1, 1.1, 1.1);
            } else {
                double t = ClientScheduler.getClientTick() + partialTicks;
                float sin = ((float) Math.sin(t / 4F)) / 32F + 1F;
                RenderSystem.scaled(sin, sin, sin);
            }
            RenderSystem.translated(-(width / 2F), -(height / 2F), 0);
            TexturesAS.TEX_GUI_BOOK_ARROWS.bindTexture();
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            this.drawRect(buf).at(0, 0).dim(width, height).tex(uFrom, vFrom, 0.5F, 0.5F).color(1F, 1F, 1F, 0.8F).draw();
            buf.finishDrawing();
            WorldVertexBufferUploader.draw(buf);
            RenderSystem.popMatrix();
        }
        int nextIndex = cIndex + 2;
        if (pages.size() >= (nextIndex + 1)) {
            int width = 30;
            int height = 15;
            rectNext = new Rectangle(guiLeft + 367, guiTop + 220, width, height);
            RenderSystem.pushMatrix();
            RenderSystem.translated(rectNext.getX() + (width / 2F), rectNext.getY() + (height / 2F), 0);
            float uFrom = 0F, vFrom = 0F;
            if (rectNext.contains(mouseX, mouseY)) {
                uFrom = 0.5F;
                RenderSystem.scaled(1.1, 1.1, 1.1);
            } else {
                double t = ClientScheduler.getClientTick() + partialTicks;
                float sin = ((float) Math.sin(t / 4F)) / 32F + 1F;
                RenderSystem.scaled(sin, sin, sin);
            }
            RenderSystem.translated(-(width / 2F), -(height / 2F), 0);
            TexturesAS.TEX_GUI_BOOK_ARROWS.bindTexture();
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            this.drawRect(buf).at(0, 0).dim(width, height).tex(uFrom, vFrom, 0.5F, 0.5F).color(1F, 1F, 1F, 0.8F).draw();
            buf.finishDrawing();
            WorldVertexBufferUploader.draw(buf);
            RenderSystem.popMatrix();
        }

        RenderSystem.enableDepthTest();
    }

    @Override
    protected boolean shouldRightClickCloseScreen(double mouseX, double mouseY) {
        if (origin != null) {
            origin.expectReInit();
            saveSite = false;
        } else {
            informPreviousClose = false;
        }
        return true;
    }

    @Override
    public void onClose() {
        if (origin != null) {
            if (saveSite) {
                openGuiInstance = this;
                ScreenJournalProgression.getJournalInstance().preventRefresh();
                Minecraft.getInstance().displayGuiScreen(null);
            } else {
                saveSite = true;
                openGuiInstance = null;
                Minecraft.getInstance().displayGuiScreen(origin);
            }
        } else {
            if (previous != null && informPreviousClose) {
                previous.onClose();
            }
            Minecraft.getInstance().displayGuiScreen(previous);
        }
    }

    @Override
    protected void mouseDragTick(double mouseX, double mouseY, double mouseDiffX, double mouseDiffY, double mouseOffsetX, double mouseOffsetY) {
        int index = currentPageOffset * 2;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            if (page != null) {
                if (page.propagateMouseDrag(mouseOffsetX, mouseOffsetY)) {
                    return;
                }
            }
        }
        index += 1;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            if (page != null) {
                page.propagateMouseDrag(mouseOffsetX, mouseOffsetY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }

        if (mouseButton == 1) {
            return true;
        }

        if (mouseButton != 0) {
            return false;
        }

        if (origin != null) {
            if (handleBookmarkClick(mouseX, mouseY)) {
                saveSite = false;
                return true;
            }
        }
        if (rectBack != null && rectBack.contains(mouseX, mouseY)) {
            if (origin != null) {
                origin.expectReInit();
                saveSite = false;
                this.onClose();
                return true;
            } else {
                informPreviousClose = false;
                this.onClose();
                return true;
            }
        }
        if (rectPrev != null && rectPrev.contains(mouseX, mouseY)) {
            this.currentPageOffset -= 1;
            SoundHelper.playSoundClient(SoundsAS.GUI_JOURNAL_PAGE, 1F, 1F);
            return true;
        }
        if (rectNext != null && rectNext.contains(mouseX, mouseY)) {
            this.currentPageOffset += 1;
            SoundHelper.playSoundClient(SoundsAS.GUI_JOURNAL_PAGE, 1F, 1F);
            return true;
        }

        int index = currentPageOffset * 2;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            if (page != null) {
                if (page.propagateMouseClick(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        index += 1;
        if (pages.size() > index) {
            RenderablePage page = pages.get(index);
            if (page != null) {
                if (page.propagateMouseClick(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return false;
    }
}
