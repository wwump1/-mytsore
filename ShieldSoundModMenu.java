package com.example.shieldsound;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Подключает экран настроек к кнопке «Настроить» в Mod Menu.
 * Если Mod Menu не установлен, этот класс просто не загружается.
 */
public class ShieldSoundModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ShieldSoundConfigScreen(parent);
    }
}
