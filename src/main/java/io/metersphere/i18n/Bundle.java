package io.metersphere.i18n;

import com.intellij.DynamicBundle;
import io.metersphere.component.MeterSphereComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * 国际化 i18n
 **/
public class Bundle extends DynamicBundle {
    @NonNls
    public static final String I18N = "messages.metersphere";

    @NotNull
    private static final Bundle INSTANCE = new Bundle();

    private Bundle() {
        super(I18N);
    }

    @Nls
    @NotNull
    protected static String message(@PropertyKey(resourceBundle = I18N) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    @Nls
    @NotNull
    public static String get(@PropertyKey(resourceBundle = I18N) String key, Object... params) {
        return message(key, params);
    }

    @Override
    protected @NotNull ResourceBundle findBundle(
            @NotNull @NonNls String pathToBundle,
            @NotNull ClassLoader loader,
            @NotNull ResourceBundle.Control control) {
        if (Objects.requireNonNull(MeterSphereComponent.getInstance().getState()).getLocale().equals("English")) {
            return ResourceBundle.getBundle(pathToBundle, Locale.ROOT, loader, control);
        }
        return ResourceBundle.getBundle(pathToBundle, Locale.CANADA, loader, control);
    }
}
