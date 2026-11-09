package cn.maikuaidoufu.client.compat;

import cn.maikuaidoufu.MasaToolMod;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Centralises reflective access to private members of third-party mods.
 *
 * <p>Field lookups are cached, and a missing or renamed member is reported once
 * instead of on every client tick. Callers are expected to degrade gracefully
 * when {@link #field(Class, String)} returns an empty optional.</p>
 */
public final class ReflectionSupport
{
    private static final Map<String, Optional<Field>> FIELDS = new HashMap<>();
    private static final Set<String> LOGGED = new HashSet<>();

    private ReflectionSupport()
    {
    }

    public static synchronized Optional<Field> field(Class<?> owner, String name)
    {
        String key = owner.getName() + "#" + name;
        Optional<Field> cached = FIELDS.get(key);

        if (cached != null)
        {
            return cached;
        }

        Optional<Field> result;

        try
        {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            result = Optional.of(field);
        }
        catch (Throwable t)
        {
            warnOnce("lookup:" + key, "Cannot access {}; the related integration is disabled ({})", key, t.toString());
            result = Optional.empty();
        }

        FIELDS.put(key, result);
        return result;
    }

    /**
     * Reads a field value, returning an empty optional on any failure or type
     * mismatch. Repeated failures are only logged once per field.
     */
    public static <T> Optional<T> read(Field field, Object target, Class<T> type)
    {
        try
        {
            Object value = field.get(target);
            return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        }
        catch (Throwable t)
        {
            warnOnce("read:" + field.getName(), "Cannot read {} ({})", field.getName(), t.toString());
            return Optional.empty();
        }
    }

    private static synchronized void warnOnce(String key, String message, Object... args)
    {
        if (LOGGED.add(key))
        {
            MasaToolMod.LOGGER.warn(message, args);
        }
    }
}
