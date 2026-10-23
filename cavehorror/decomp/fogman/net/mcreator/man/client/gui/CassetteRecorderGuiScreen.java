package net.mcreator.man.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import net.mcreator.man.ManMod;
import net.mcreator.man.network.CassetteRecorderGuiButtonMessage;
import net.mcreator.man.world.inventory.CassetteRecorderGuiMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CassetteRecorderGuiScreen extends AbstractContainerScreen<CassetteRecorderGuiMenu> {
   private static final HashMap<String, Object> guistate = CassetteRecorderGuiMenu.guistate;
   private final Level world;
   private final int x;
   private final int y;
   private final int z;
   private final Player entity;
   Button button_empty;
   private static final ResourceLocation texture = new ResourceLocation("man:textures/screens/cassette_recorder_gui.png");

   public CassetteRecorderGuiScreen(CassetteRecorderGuiMenu container, Inventory inventory, Component text) {
      super(container, inventory, text);
      this.world = container.world;
      this.x = container.x;
      this.y = container.y;
      this.z = container.z;
      this.entity = container.entity;
      this.f_97726_ = 172;
      this.f_97727_ = 195;
   }

   public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      this.m_280273_(guiGraphics);
      super.m_88315_(guiGraphics, mouseX, mouseY, partialTicks);
      this.m_280072_(guiGraphics, mouseX, mouseY);
      if (mouseX > this.f_97735_ + 147 && mouseX < this.f_97735_ + 171 && mouseY > this.f_97736_ + 36 && mouseY < this.f_97736_ + 60) {
         guiGraphics.m_280557_(this.f_96547_, Component.m_237115_("gui.man.cassette_recorder_gui.tooltip_play"), mouseX, mouseY);
      }
   }

   protected void m_7286_(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      guiGraphics.m_280163_(texture, this.f_97735_, this.f_97736_, 0.0F, 0.0F, this.f_97726_, this.f_97727_, this.f_97726_, this.f_97727_);
      guiGraphics.m_280163_(
         new ResourceLocation("man:textures/screens/cassette_recorder_overlay.png"), this.f_97735_ + -24, this.f_97736_ + -33, 0.0F, 0.0F, 230, 153, 230, 153
      );
      RenderSystem.disableBlend();
   }

   public boolean m_7933_(int key, int b, int c) {
      if (key == 256) {
         this.f_96541_.f_91074_.m_6915_();
         return true;
      } else {
         return super.m_7933_(key, b, c);
      }
   }

   protected void m_280003_(GuiGraphics guiGraphics, int mouseX, int mouseY) {
   }

   public void m_7856_() {
      super.m_7856_();
      this.button_empty = new PlainTextButton(
         this.f_97735_ + 147, this.f_97736_ + 37, 25, 20, Component.m_237115_("gui.man.cassette_recorder_gui.button_empty"), e -> {
            ManMod.PACKET_HANDLER.sendToServer(new CassetteRecorderGuiButtonMessage(0, this.x, this.y, this.z));
            CassetteRecorderGuiButtonMessage.handleButtonAction(this.entity, 0, this.x, this.y, this.z);
         }, this.f_96547_
      );
      guistate.put("button:button_empty", this.button_empty);
      this.m_142416_(this.button_empty);
   }
}
