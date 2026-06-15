package com.k8bus.menu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import com.k8bus.UnnamedDungeons;

public class ModMenuTypes {
    public static final MenuType<WalletDepositMenu> WALLET_DEPOSIT = new MenuType<>(WalletDepositMenu::new, FeatureFlags.DEFAULT_FLAGS);

    public static void initialize() {
        Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "wallet_deposit"), WALLET_DEPOSIT);
    }
}
