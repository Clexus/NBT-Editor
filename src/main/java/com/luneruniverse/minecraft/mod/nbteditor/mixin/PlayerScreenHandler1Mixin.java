package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(targets = "net.minecraft.screen.PlayerScreenHandler$1")
public class PlayerScreenHandler1Mixin {
	@Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
	private void canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
		PlayerEntity owner = ((PlayerInventory) ((Slot) (Object) this).inventory).player;
		if (owner instanceof ServerPlayerEntity) {
			if (owner.hasPermissionLevel(2) && ServerMixinLink.NO_ARMOR_RESTRICTION_PLAYERS.getOrDefault(owner, false))
				info.setReturnValue(true);
		} else {
			if (NBTEditorClient.SERVER_CONN.isEditingAllowed() && ConfigScreen.isNoArmorRestriction())
				info.setReturnValue(true);
		}
	}
	@Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
	private void canTakeItems(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
		if (player instanceof ServerPlayerEntity) {
			if (player.hasPermissionLevel(2) && ServerMixinLink.NO_ARMOR_RESTRICTION_PLAYERS.getOrDefault(player, false))
				info.setReturnValue(true);
		} else {
			if (NBTEditorClient.SERVER_CONN.isEditingAllowed() && ConfigScreen.isNoArmorRestriction())
				info.setReturnValue(true);
		}
	}
}
