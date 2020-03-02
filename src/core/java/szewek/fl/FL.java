package szewek.fl;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import szewek.fl.network.FluxPlus;
import szewek.fl.recipe.CountedIngredient;
import szewek.fl.util.ValueToIDMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main mod class
 */
@Mod(FL.ID)
public final class FL {
	public static final String ID = "fl";

	public FL() {
		MinecraftForge.EVENT_BUS.register(Events.class);
	}

	private static boolean unfamiliar(final ResourceLocation loc) {
		final String ns = loc.getNamespace();
		return !"minecraft".equals(ns) && !"flux".equals(ns) && !"forge".equals(ns);
	}

	private static <T, K, V> Map<K, V> map(T t) {
		return new HashMap<>();
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModEvents {
		@SubscribeEvent
		public static void setup(final FMLCommonSetupEvent e) {
			FluxPlus.putAction("start");
			CraftingHelper.register(new ResourceLocation(ID, "counted"), CountedIngredient.Serializer.INSTANCE);
		}

		@SubscribeEvent
		public static void stop(final FMLServerStoppedEvent e) {
			FluxPlus.putAction("exit");
		}
	}

	static class Events {
		@SubscribeEvent
		public static void recipesLoaded(final RecipesUpdatedEvent e) {
			final Map<String, Object> recipeInfos = new ConcurrentHashMap<>();
			final ValueToIDMap<String> typeIds = new ValueToIDMap<>();
			final ValueToIDMap<String> itemIds = new ValueToIDMap<>();
			final Map<String, Map<String, Object>> namespaces = new ConcurrentHashMap<>();
			final Collection<IRecipe<?>> recipes = e.getRecipeManager().getRecipes();
			for (IRecipe<?> r : recipes) {
				ResourceLocation id = r.getId();
				if (unfamiliar(id)) {
					ResourceLocation serName = r.getSerializer().getRegistryName();
					if (serName == null || serName.getPath().startsWith("craft")) {
						continue;
					}
					ItemStack stack = r.getRecipeOutput();
					if (stack.isEmpty()) {
						continue;
					}
					ResourceLocation itemLoc = stack.getItem().getRegistryName();
					if (itemLoc == null) {
						continue;
					}
					int[] info = new int[4];
					info[0] = typeIds.get(serName.toString());
					info[1] = r.getIngredients().size();
					info[2] = itemIds.get(itemLoc.toString());
					info[3] = stack.getCount();
					namespaces.computeIfAbsent(id.getNamespace(), FL::map).put(id.getPath(), info);
				}
			}
			recipeInfos.put("$types", typeIds.values());
			recipeInfos.put("$items", itemIds.values());
			recipeInfos.putAll(namespaces);
			FluxPlus.sendRecipeInfos(recipeInfos);
		}

		@SubscribeEvent
		public static void playerLogin(final PlayerEvent.PlayerLoggedInEvent e) {
			FluxPlus.putAction("login");
		}
	}
}
