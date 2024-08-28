package io.metersphere.theme;

import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.Objects;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThemeTool {

    /**
     * 判断当前主题是否是深色
     **/
    public static boolean isDark() {
        EditorColorsManager colorsManager = EditorColorsManager.getInstance();
        EditorColorsScheme scheme = colorsManager.getGlobalScheme();
        Color caretRowColor = scheme.getColor(EditorColors.CARET_ROW_COLOR);
        if (Objects.nonNull(caretRowColor)) {
            float brightness = calculateBrightness(caretRowColor);
            return brightness < 0.5;
        }
        return true;
    }

    private static float calculateBrightness(Color color) {
        return (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000f / 255f;
    }
}
