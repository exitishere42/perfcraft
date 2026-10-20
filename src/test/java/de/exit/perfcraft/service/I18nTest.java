package de.exit.perfcraft.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    @Test
    void testDefaultLanguageIsEnglish() {
        I18n.setLanguage(I18n.Language.EN);
        assertEquals("File", I18n.get("menu.file"));
        assertEquals("Top Layer", I18n.get("layer.top"));
        assertEquals("Tactile Switch", I18n.get("comp.switch"));
    }

    @Test
    void testSwitchLanguageToGerman() {
        I18n.setLanguage(I18n.Language.DE);
        assertEquals("Datei", I18n.get("menu.file"));
        assertEquals("Bauteilseite", I18n.get("layer.top"));
        assertEquals("Taster", I18n.get("comp.switch"));

        // Reset to default English
        I18n.setLanguage(I18n.Language.EN);
        assertEquals("File", I18n.get("menu.file"));
    }
}
