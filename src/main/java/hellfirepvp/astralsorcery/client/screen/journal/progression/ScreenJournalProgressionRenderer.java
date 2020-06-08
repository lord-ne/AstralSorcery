/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.client.screen.journal.progression;

import com.mojang.blaze3d.systems.RenderSystem;
import hellfirepvp.astralsorcery.client.lib.TexturesAS;
import hellfirepvp.astralsorcery.client.resource.AbstractRenderableTexture;
import hellfirepvp.astralsorcery.client.screen.helper.ScalingPoint;
import hellfirepvp.astralsorcery.client.screen.helper.ScreenRenderBoundingBox;
import hellfirepvp.astralsorcery.client.screen.journal.ScreenJournalProgression;
import hellfirepvp.astralsorcery.client.util.Blending;
import hellfirepvp.astralsorcery.client.util.RenderingDrawUtils;
import hellfirepvp.astralsorcery.client.util.RenderingUtils;
import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ResearchHelper;
import hellfirepvp.astralsorcery.common.data.research.ResearchProgression;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ScreenJournalProgressionRenderer
 * Created by HellFirePvP
 * Date: 03.08.2019 / 17:02
 */
public class ScreenJournalProgressionRenderer {

    private GalaxySizeHandler sizeHandler;
    private ScreenJournalProgression parentGui;

    public ScreenRenderBoundingBox realRenderBox;
    private int realCoordLowerX, realCoordLowerY;
    private int realRenderWidth, realRenderHeight;

    private ScalingPoint mousePointScaled;
    private ScalingPoint previousMousePointScaled;

    private ResearchProgression focusedClusterZoom = null, focusedClusterMouse = null;
    private ScreenJournalClusterRenderer clusterRenderer = null;

    private long doubleClickLast = 0L;

    private boolean hasPrevOffset = false;
    private Map<Rectangle, ResearchProgression> clusterRectMap = new HashMap<>();

    public ScreenJournalProgressionRenderer(ScreenJournalProgression gui, int guiHeight, int guiWidth) {
        this.parentGui = gui;
        this.sizeHandler = new GalaxySizeHandler(guiHeight, guiWidth);
        refreshSize();
        this.mousePointScaled = ScalingPoint.createPoint(
                this.sizeHandler.clampX(this.sizeHandler.getMidX()),
                this.sizeHandler.clampY(this.sizeHandler.getMidY()),
                this.sizeHandler.getScalingFactor(),
                false);
        this.moveMouse(this.sizeHandler.getTotalWidth() / 2, this.sizeHandler.getTotalHeight() / 2);
        applyMovedMouseOffset();
    }

    public void refreshSize() {
        this.sizeHandler.updateSize();
    }

    public void setBox(int left, int top, int right, int bottom) {
        this.realRenderBox = new ScreenRenderBoundingBox(left, top, right, bottom);
        this.realRenderWidth = (int) this.realRenderBox.getWidth();
        this.realRenderHeight = (int) this.realRenderBox.getHeight();
    }

    public void moveMouse(float changedX, float changedY) {
        if (sizeHandler.getScalingFactor() >= 6.1D && clusterRenderer != null) {
            clusterRenderer.moveMouse(changedX, changedY);
        } else {
            if (hasPrevOffset) {
                mousePointScaled.updateScaledPos(
                        sizeHandler.clampX(previousMousePointScaled.getScaledPosX() + changedX),
                        sizeHandler.clampY(previousMousePointScaled.getScaledPosY() + changedY),
                        sizeHandler.getScalingFactor());
            } else {
                mousePointScaled.updateScaledPos(
                        sizeHandler.clampX(changedX),
                        sizeHandler.clampY(changedY),
                        sizeHandler.getScalingFactor());
            }
        }
    }

    public void applyMovedMouseOffset() {
        if (sizeHandler.getScalingFactor() >= 6.1D && clusterRenderer != null) {
            clusterRenderer.applyMovedMouseOffset();
        } else {
            this.previousMousePointScaled = ScalingPoint.createPoint(
                    mousePointScaled.getScaledPosX(),
                    mousePointScaled.getScaledPosY(),
                    sizeHandler.getScalingFactor(),
                    true);
            this.hasPrevOffset = true;
        }
    }

    public void updateOffset(int guiLeft, int guiTop) {
        this.realCoordLowerX = guiLeft;
        this.realCoordLowerY = guiTop;
    }

    public void centerMouse() {
        this.moveMouse(parentGui.getGuiLeft() + this.sizeHandler.getMidX(), parentGui.getGuiTop() + this.sizeHandler.getMidY());
    }

    public void updateMouseState() {
        moveMouse(0, 0);
    }

    public void unfocus() {
        focusedClusterZoom = null;
    }

    public void focus(@Nonnull ResearchProgression researchCluster) {
        this.focusedClusterZoom = researchCluster;
        this.clusterRenderer = new ScreenJournalClusterRenderer(researchCluster, realRenderHeight, realRenderWidth, realCoordLowerX, realCoordLowerY);
    }

    //Nothing to actually click here, we redirect if we can.
    public boolean propagateClick(float mouseX, float mouseY) {
        if (clusterRenderer != null) {
            if (sizeHandler.getScalingFactor() > 6) {
                if (clusterRenderer.propagateClick(parentGui, mouseX, mouseY)) {
                    return true;
                }
            }
        }
        if (focusedClusterMouse != null) {
            if (sizeHandler.getScalingFactor() <= 6) {
                long current = System.currentTimeMillis();
                if (current - this.doubleClickLast < 400L) {
                    int timeout = 500; //Handles irregular clicks on the GUI so it doesn't loop trying to find a focus cluster
                    while (focusedClusterMouse != null && sizeHandler.getScalingFactor() < 9.9 && timeout > 0) {
                        handleZoomIn(mouseX, mouseY);
                        timeout--;
                    }
                    this.doubleClickLast = 0L;
                    return true;
                }
                this.doubleClickLast = current;
            }
        }
        return false;
    }

    public void drawMouseHighlight(float zLevel, int mouseX, int mouseY) {
        if (clusterRenderer != null && sizeHandler.getScalingFactor() > 6) {
            clusterRenderer.drawMouseHighlight(zLevel, mouseX, mouseY);
        }
    }

    public void resetZoom() {
        sizeHandler.resetZoom();
        rescale(sizeHandler.getScalingFactor());
    }

    public void handleZoomOut() {
        this.sizeHandler.handleZoomOut();
        rescale(sizeHandler.getScalingFactor());

        if (this.sizeHandler.getScalingFactor() <= 4.0) {
            unfocus();
        } else if (this.sizeHandler.getScalingFactor() >= 6.0 && this.clusterRenderer != null) {
            clusterRenderer.handleZoomOut();
        }
    }

    /**
     * Thresholds for zooming in:
     * 1.0 - 4.0 don't care.
     * 4.0 - 6.0 has to have focus + centering to center of cluster
     * 6.0 - 10.0 transition (6.0 - 8.0) + cluster rendering + handling (cursor movement)
     */
    public void handleZoomIn(float mouseX, float mouseY) {
        float scale = sizeHandler.getScalingFactor();
        //double nextScale = Math.min(10.0D, scale + 0.2D);
        if (scale >= 4.0F) {
            if (focusedClusterZoom == null) {
                ResearchProgression prog = tryFocusCluster(mouseX, mouseY);
                if (prog != null) {
                    focus(prog);
                }
            }
            if (focusedClusterZoom == null) {
                return;
            }
            if (scale < 6.1F) { //Floating point shenanigans
                float vDiv = (2F - (scale - 4F)) * 10F;
                Rectangle2D rect = calcBoundingRectangle(focusedClusterZoom);
                Vector3 center = new Vector3(rect.getCenterX(), rect.getCenterY(), 0);
                Vector3 mousePos = new Vector3(mousePointScaled.getScaledPosX(), mousePointScaled.getScaledPosY(), 0);
                Vector3 dir = center.subtract(mousePos);
                if (vDiv > 0.05) {
                    dir.divide(vDiv);
                }
                if (!hasPrevOffset) {
                    mousePointScaled.updateScaledPos(
                            sizeHandler.clampX((float) (mousePos.getX() + dir.getX())),
                            sizeHandler.clampY((float) (mousePos.getY() + dir.getY())),
                            sizeHandler.getScalingFactor());
                } else {
                    previousMousePointScaled.updateScaledPos(
                            sizeHandler.clampX((float) (mousePos.getX() + dir.getX())),
                            sizeHandler.clampY((float) (mousePos.getY() + dir.getY())),
                            sizeHandler.getScalingFactor());
                }

                updateMouseState();
            } else if (clusterRenderer != null) {
                clusterRenderer.handleZoomIn();
            }
        }
        this.sizeHandler.handleZoomIn();
        rescale(sizeHandler.getScalingFactor());
    }

    private void rescale(float newScale) {
        this.mousePointScaled.rescale(newScale);
        if (this.previousMousePointScaled != null) {
            this.previousMousePointScaled.rescale(newScale);
        }
        updateMouseState();
    }

    public void drawProgressionPart(float zLevel, int mouseX, int mouseY) {
        drawBackground(zLevel);

        drawClusters(zLevel);

        focusedClusterMouse = tryFocusCluster(mouseX, mouseY);

        float scaleX = this.mousePointScaled.getPosX();
        float scaleY = this.mousePointScaled.getPosY();

        if (sizeHandler.getScalingFactor() >= 6.1D && focusedClusterZoom != null && clusterRenderer != null) {
            JournalCluster cluster = JournalProgressionClusterMapping.getClusterMapping(focusedClusterZoom);
            drawClusterBackground(cluster.clusterBackgroundTexture, zLevel);

            clusterRenderer.drawClusterScreen(zLevel);
            scaleX = clusterRenderer.getScaleMouseX();
            scaleY = clusterRenderer.getScaleMouseY();
        }

        if (focusedClusterMouse != null) {
            JournalCluster cluster = JournalProgressionClusterMapping.getClusterMapping(focusedClusterMouse);
            float lX = sizeHandler.evRelativePosX(cluster.x);
            float rX = sizeHandler.evRelativePosX(cluster.maxX);
            float lY = sizeHandler.evRelativePosY(cluster.y);
            float rY = sizeHandler.evRelativePosY(cluster.maxY);
            float scaledLeft = this.mousePointScaled.getScaledPosX() - sizeHandler.widthToBorder;
            float scaledTop = this.mousePointScaled.getScaledPosY() - sizeHandler.heightToBorder;
            float xAdd = lX - scaledLeft;
            float yAdd = lY - scaledTop;
            float offsetX = realCoordLowerX + xAdd;
            float offsetY = realCoordLowerY + yAdd;

            float scale = sizeHandler.getScalingFactor();
            float br = 1F;
            if (scale > 8.01F) {
                br = 0F;
            } else if (scale >= 6F) {
                br = 1F - ((scale - 6F) / 2F);
            }

            String name = focusedClusterMouse.getName().getFormattedText();
            float length = Minecraft.getInstance().fontRenderer.getStringWidth(name) * 1.4F;

            RenderSystem.pushMatrix();
            RenderSystem.translated(offsetX + ((rX - lX) / 2) - length / 2D, offsetY + ((rY - lY) / 3), 0);
            RenderSystem.scaled(1.4, 1.4, 1.4);
            int alpha = 0xCC;
            alpha *= br;
            alpha = Math.max(alpha, 5);
            int color = 0x5A28FF | (alpha << 24);
            RenderingDrawUtils.renderStringWithShadowAtCurrentPos(null, name, color);

            RenderSystem.popMatrix();
        }

        drawStarParallaxLayers(scaleX, scaleY, zLevel);
    }

    @Nullable
    private ResearchProgression tryFocusCluster(double mouseX, double mouseY) {
        for (Rectangle r : this.clusterRectMap.keySet()) {
            if (r.contains(mouseX, mouseY)) {
                return this.clusterRectMap.get(r);
            }
        }
        return null;
    }

    private Rectangle2D calcBoundingRectangle(ResearchProgression progression) {
        JournalCluster cluster = JournalProgressionClusterMapping.getClusterMapping(progression);
        float lX = sizeHandler.evRelativePosX(cluster.x);
        float rX = sizeHandler.evRelativePosX(cluster.maxX);
        float lY = sizeHandler.evRelativePosY(cluster.y);
        float rY = sizeHandler.evRelativePosY(cluster.maxY);
        return new Rectangle2D.Float(lX, lY, rX - lX, rY - lY);
    }

    private void drawClusters(float zLevel) {
        clusterRectMap.clear();
        if (sizeHandler.getScalingFactor() >= 8.01) return;

        PlayerProgress thisProgress = ResearchHelper.getClientProgress();
        for (ResearchProgression progress : thisProgress.getResearchProgression()) {
            JournalCluster cluster = JournalProgressionClusterMapping.getClusterMapping(progress);
            float lX = sizeHandler.evRelativePosX(cluster.x);
            float rX = sizeHandler.evRelativePosX(cluster.maxX);
            float lY = sizeHandler.evRelativePosY(cluster.y);
            float rY = sizeHandler.evRelativePosY(cluster.maxY);
            renderCluster(progress, cluster, lX, lY, rX, rY, zLevel);
        }
    }

    private void drawStarParallaxLayers(float scalePosX, float scalePosY, float zLevel) {
        TexturesAS.TEX_GUI_STARFIELD_OVERLAY.bindTexture();
        RenderSystem.enableBlend();
        Blending.OVERLAYDARK.apply();

        drawStarOverlay(zLevel, scalePosX, scalePosY, 1.5F);
        drawStarOverlay(zLevel, scalePosX, scalePosY, 2.5F);
        drawStarOverlay(zLevel, scalePosX, scalePosY, 3.5F);

        Blending.DEFAULT.apply();
        RenderSystem.disableBlend();
    }

    private void renderCluster(ResearchProgression p, JournalCluster cluster,
                               float lowerPosX, float lowerPosY, float higherPosX, float higherPosY, float zLevel) {
        float scaledLeft = this.mousePointScaled.getScaledPosX() - sizeHandler.widthToBorder;
        float scaledTop =  this.mousePointScaled.getScaledPosY() - sizeHandler.heightToBorder;
        float xAdd = lowerPosX - scaledLeft;
        float yAdd = lowerPosY - scaledTop;
        float offsetX = realCoordLowerX + xAdd;
        float offsetY = realCoordLowerY + yAdd;

        float width =  higherPosX - lowerPosX;
        float height = higherPosY - lowerPosY;

        Rectangle r = new Rectangle(MathHelper.floor(offsetX), MathHelper.floor(offsetY), MathHelper.floor(width), MathHelper.floor(height));
        clusterRectMap.put(r, p);

        cluster.cloudTexture.bindTexture();

        float scale = sizeHandler.getScalingFactor();
        float br;
        if (scale > 8.01F) {
            br = 0F;
        } else if (scale >= 6F) {
            br = 1F - ((scale - 6F) / 2F);
        } else {
            br = 1F;
        }

        RenderSystem.enableBlend();
        Blending.ADDITIVEDARK.apply();
        RenderingUtils.draw(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX, buf -> {
            buf.pos(offsetX + 0,     offsetY + height, zLevel).color(br, br, br, br).tex(0, 1).endVertex();
            buf.pos(offsetX + width, offsetY + height, zLevel).color(br, br, br, br).tex(1, 1).endVertex();
            buf.pos(offsetX + width, offsetY + 0,      zLevel).color(br, br, br, br).tex(1, 0).endVertex();
            buf.pos(offsetX + 0,     offsetY + 0,      zLevel).color(br, br, br, br).tex(0, 0).endVertex();
        });

        Blending.DEFAULT.apply();
        RenderSystem.disableBlend();
    }

    private void drawClusterBackground(AbstractRenderableTexture tex, float zLevel) {
        float scale = sizeHandler.getScalingFactor();
        float br;
        if (scale > 8.01F) {
            br = 0.75F;
        } else if (scale >= 6F) {
            br = ((scale - 6F) / 2F) * 0.75F;
        } else {
            br = 0F;
        }

        tex.bindTexture();
        RenderSystem.enableBlend();
        Blending.ADDITIVEDARK.apply();

        RenderingUtils.draw(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX, buf -> {
            buf.pos(realCoordLowerX,                   realCoordLowerY + realRenderHeight, zLevel).color(br, br, br, br).tex(0, 1).endVertex();
            buf.pos(realCoordLowerX + realRenderWidth, realCoordLowerY + realRenderHeight, zLevel).color(br, br, br, br).tex(1, 1).endVertex();
            buf.pos(realCoordLowerX + realRenderWidth, realCoordLowerY,                    zLevel).color(br, br, br, br).tex(1, 0).endVertex();
            buf.pos(realCoordLowerX,                   realCoordLowerY,                    zLevel).color(br, br, br, br).tex(0, 0).endVertex();
        });

        Blending.DEFAULT.apply();
        RenderSystem.disableBlend();
    }

    private void drawBackground(float zLevel) {
        float br = 0.2F;
        TexturesAS.TEX_GUI_BACKGROUND_DEFAULT.bindTexture();
        RenderingUtils.draw(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX, buf -> {
            buf.pos(realCoordLowerX,                   realCoordLowerY + realRenderHeight, zLevel).color(br, br, br, 1.0F).tex(0, 1).endVertex();
            buf.pos(realCoordLowerX + realRenderWidth, realCoordLowerY + realRenderHeight, zLevel).color(br, br, br, 1.0F).tex(1, 1).endVertex();
            buf.pos(realCoordLowerX + realRenderWidth, realCoordLowerY,                    zLevel).color(br, br, br, 1.0F).tex(1, 0).endVertex();
            buf.pos(realCoordLowerX,                   realCoordLowerY,                    zLevel).color(br, br, br, 1.0F).tex(0, 0).endVertex();
        });
    }

    private void drawStarOverlay(float zLevel, float scalePosX, float scalePosY, float scaleFactor) {
        RenderSystem.pushMatrix();
        RenderSystem.scaled(scaleFactor, scaleFactor, scaleFactor);

        float th = sizeHandler.getTotalHeight() / sizeHandler.getScalingFactor();
        float tw = sizeHandler.getTotalWidth()  / sizeHandler.getScalingFactor();

        float lowU = (scalePosX - sizeHandler.widthToBorder) / tw;
        float highU = lowU + (((float) realRenderWidth) / tw);
        float lowV = (scalePosY - sizeHandler.heightToBorder) / th;
        float highV = lowV + (((float) realRenderHeight) / th);

        RenderingUtils.draw(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX, buf -> {
            buf.pos(0,               realRenderHeight, zLevel).color(0.6F, 0.6F, 0.6F, 1F).tex(lowU,  highV).endVertex();
            buf.pos(realRenderWidth, realRenderHeight, zLevel).color(0.6F, 0.6F, 0.6F, 1F).tex(highU, highV).endVertex();
            buf.pos(realRenderWidth, 0,                zLevel).color(0.6F, 0.6F, 0.6F, 1F).tex(highU, lowV).endVertex();
            buf.pos(0,               0,                zLevel).color(0.6F, 0.6F, 0.6F, 1F).tex(lowU, lowV).endVertex();
        });

        RenderSystem.popMatrix();
    }

}
