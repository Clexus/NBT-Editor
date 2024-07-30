package com.luneruniverse.minecraft.mod.nbteditor.screens.containers;

import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ContainerItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.LocalFactoryScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class ContainerScreen<L extends LocalNBT> extends ClientHandledScreen {
	
	private boolean saved;
	private final Text unsavedTitle;
	
	private NBTReference<L> ref;
	private L localNBT;
	private int blockedInvSlot;
	private int blockedHotbarSlot;
	private int numSlots;
	
	private ItemStack[] prevInv;
	private boolean navigationClicked;
	
	private ContainerScreen(ContainerHandler handler, Text title) {
		super(handler, title);
		
		this.saved = true;
		this.unsavedTitle = TextInst.copy(title).append("*");
	}
	private ContainerScreen<L> build(NBTReference<L> ref) {
		this.ref = ref;
		this.localNBT = LocalNBT.copy(ref.getLocalNBT());
		this.blockedInvSlot = (ref instanceof ItemReference item ? item.getBlockedInvSlot() : -1);
		if (this.blockedInvSlot != -1)
			this.blockedInvSlot += 27;
		this.blockedHotbarSlot = (ref instanceof ItemReference item ? item.getBlockedHotbarSlot() : -1);
		
		ItemStack[] contents = ContainerIO.read(localNBT);
		for (int i = 0; i < contents.length; i++)
			this.handler.getSlot(i).setStackNoCallbacks(contents[i] == null ? ItemStack.EMPTY : contents[i]);
		this.numSlots = contents.length;
		
		return this;
	}
	public static <L extends LocalNBT> void show(NBTReference<L> ref, Optional<ItemStack> cursor) {
		ContainerHandler handler = new ContainerHandler();
		handler.setCursorStack(cursor.orElse(MainUtil.client.player.playerScreenHandler.getCursorStack()));
		MainUtil.client.setScreen(new ContainerScreen<L>(handler, TextInst.translatable("nbteditor.container.title")
				.append(ref.getLocalNBT().getName())).build(ref));
	}
	public static void show(NBTReference<?> ref) {
		show(ref, Optional.empty());
	}
	
	@Override
	protected void init() {
		super.init();
		
		if (ref instanceof ItemReference item && item.isLockable()) {
			this.addDrawableChild(MVMisc.newButton(16, 64, 83, 20, ConfigScreen.isLockSlots() ? TextInst.translatable("nbteditor.client_chest.slots.unlock") : TextInst.translatable("nbteditor.client_chest.slots.lock"), btn -> {
				navigationClicked = true;
				if (ConfigScreen.isLockSlotsRequired()) {
					btn.active = false;
					ConfigScreen.setLockSlots(true);
				} else
					ConfigScreen.setLockSlots(!ConfigScreen.isLockSlots());
				btn.setMessage(ConfigScreen.isLockSlots() ? TextInst.translatable("nbteditor.client_chest.slots.unlock") : TextInst.translatable("nbteditor.client_chest.slots.lock"));
			})).active = !ConfigScreen.isLockSlotsRequired();
		}
		
		addDrawableChild(MVMisc.newTexturedButton(width - 36, 22, 20, 20, 20,
				LocalFactoryScreen.FACTORY_ICON,
				btn -> {
					if (!handler.getCursorStack().isEmpty()) {
						MainUtil.get(handler.getCursorStack(), true);
						ref.clearParentCursor();
					}
					client.setScreen(new LocalFactoryScreen<>(ref));
				},
				new MVTooltip("nbteditor.factory")));
	}
	
	@Override
	protected Text getRenderedTitle() {
		return saved ? title : unsavedTitle;
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		navigationClicked = false;
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
		if (navigationClicked)
			return;
		if (slot != null && slot.id == this.blockedInvSlot)
			return;
		if (actionType == SlotActionType.SWAP && button == blockedHotbarSlot)
			return;
		if (slot != null && slot.id >= numSlots && slot.inventory == this.handler.getInventory() && (slot.getStack() == null || slot.getStack().isEmpty()))
			return;
		
		prevInv = new ItemStack[this.handler.getInventory().size()];
		for (int i = 0; i < prevInv.length; i++)
			prevInv[i] = this.handler.getInventory().getStack(i).copy();
		
		super.onMouseClick(slot, slotId, button, actionType);
	}
	@Override
	public boolean allowEnchantmentCombine(Slot slot) {
		return slot.id != this.blockedInvSlot;
	}
	@Override
	public void onEnchantmentCombine(Slot slot) {
		save();
	}
	@Override
	public SlotLockType getSlotLockType() {
		return ref instanceof ItemReference item && item.isLocked() ? SlotLockType.ITEMS_LOCKED : SlotLockType.UNLOCKED;
	}
	@Override
	public ItemStack[] getPrevInventory() {
		return prevInv;
	}
	@Override
	public void onChange() {
		save();
	}
	private void save() {
		ItemStack[] contents = new ItemStack[this.handler.getInventory().size()];
		for (int i = 0; i < contents.length; i++)
			contents[i] = this.handler.getInventory().getStack(i);
		ContainerIO.write(localNBT, contents);
		
		saved = false;
		ref.saveLocalNBT(localNBT, () -> {
			saved = true;
		});
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (MainUtil.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
			ref.showParent(Optional.of(handler.getCursorStack()));
			return true;
		}
		
		if (keyCode == GLFW.GLFW_KEY_SPACE) {
			if (focusedSlot != null && (focusedSlot.id < numSlots || focusedSlot.inventory != this.handler.getInventory())) {
				if (handleKeybind(keyCode, focusedSlot, this,
						slot -> new ContainerItemReference<>(ref, slot.getIndex()), handler.getCursorStack())) {
					return true;
				}
			}
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean shouldPause() {
		return true;
	}
	
	public NBTReference<L> getReference() {
		return ref;
	}
	
	@Override
	public void close() {
		ref.escapeParent(Optional.of(handler.getCursorStack()));
		MainUtil.client.player.closeHandledScreen();
	}
	@Override
	public void removed() {
		for (int i = numSlots; i < 27; i++) { // Items that may get deleted
			ItemStack item = this.handler.getInventory().getStack(i);
			if (item != null && !item.isEmpty())
				MainUtil.get(item, true);
		}
		
		super.removed();
	}
	
}
