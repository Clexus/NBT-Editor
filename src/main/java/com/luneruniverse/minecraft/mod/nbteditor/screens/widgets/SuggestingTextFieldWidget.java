package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.lang.invoke.MethodType;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class SuggestingTextFieldWidget extends NamedTextFieldWidget implements MVElement {
	
	private final ChatInputSuggestor suggestor;
	private Function<String, CompletableFuture<Suggestions>> suggestions;
	
	public SuggestingTextFieldWidget(Screen screen, int x, int y, int width, int height, TextFieldWidget copyFrom) {
		super(x, y, width, height, copyFrom);
		suggestor = new ChatInputSuggestor(MainUtil.client, screen, this, MainUtil.client.textRenderer, false, true, 0, 7, false, -2147483648) {
			@Override
			public void refresh() {
				if (!this.completingSuggestions) {
					SuggestingTextFieldWidget.this.setSuggestion(null);
					this.window = null;
				}
				this.messages.clear();
				if (this.window == null || !this.completingSuggestions) {
					if (suggestions == null)
						this.pendingSuggestions = new SuggestionsBuilder("", 0).buildFuture();
					else
						this.pendingSuggestions = suggestions.apply(SuggestingTextFieldWidget.this.text);
					this.pendingSuggestions.thenRun(() -> {
						if (!this.pendingSuggestions.isDone())
							return;
						showCommandSuggestions();
					});
				}
			}
			@Override
			protected boolean showUsages(Formatting formatting) {
				return true;
			}
			@Override
			protected OrderedText provideRenderText(String original, int firstCharacterIndex) {
				return OrderedText.styledForwardsVisitedString(original, Style.EMPTY);
			}
		};
		suggestor.parse = new ParseResults<>(null);
		
		setChangedListener(null);
	}
	public SuggestingTextFieldWidget(Screen screen, int x, int y, int width, int height) {
		this(screen, x, y, width, height, null);
	}
	
	@Override
	public void setChangedListener(Consumer<String> listener) {
		super.setChangedListener(str -> {
			suggestor.refresh();
			if (listener != null)
				listener.accept(str);
		});
	}
	
	public SuggestingTextFieldWidget suggest(Function<String, CompletableFuture<Suggestions>> suggestions) {
		this.suggestions = suggestions;
		suggestor.refresh();
		return this;
	}
	
	private static final Supplier<Reflection.MethodInvoker> ChatInputSuggestor_render =
			Reflection.getOptionalMethod(ChatInputSuggestor.class, "", MethodType.methodType(void.class, MatrixStack.class, int.class, int.class));
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		super.render(matrices, mouseX, mouseY, delta);
		matrices.push();
		matrices.translate(0, 0, 1.0f);
		Version.newSwitch()
				.range("1.20.0", null, () -> suggestor.render(MVDrawableHelper.getDrawContext(matrices), mouseX, mouseY))
				.range(null, "1.19.4", () -> ChatInputSuggestor_render.get().invoke(suggestor, matrices, mouseX, mouseY))
				.run();
		matrices.pop();
	}
	@Override
	protected boolean shouldShowName() {
		return !suggestor.isOpen();
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return suggestor.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return suggestor.mouseScrolled(verticalAmount) || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return suggestor.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		if (suggestor.window != null) {
			if (suggestor.window.area.contains((int) mouseX, (int) mouseY))
				return true;
		}
		return super.isMouseOver(mouseX, mouseY);
	}
	
	@Override
	public void onFocusChange(boolean focused) {
		MVElement.super.onFocusChange(focused);
		suggestor.setWindowActive(focused);
		suggestor.refresh();
	}
	
}
