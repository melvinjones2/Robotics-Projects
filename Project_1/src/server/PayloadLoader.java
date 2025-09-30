package server;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Modifier;

/**
 * Utility to discover and load Payloads dynamically.
 */
public class PayloadLoader {

    /**
     * Returns a list of available payload classes in the server.payloads package.
     * You must manually list them here, or use a config file or classpath scanning library for full automation.
     */
    public static List<Class<? extends Payload>> discoverPayloads() {
        List<Class<? extends Payload>> payloads = new ArrayList<Class<? extends Payload>>();
        // Manually list your payload classes here:
        try {
            payloads.add(Class.forName("server.BatteryLoggingPayload").asSubclass(Payload.class));
        } catch (Exception ignored) {}
        try {
            payloads.add(Class.forName("server.RobotDrawingPayload").asSubclass(Payload.class));
        } catch (Exception ignored) {}
        // Add more payloads as needed
        return payloads;
    }

    /**
     * Instantiates a payload by class.
     */
    public static Payload loadPayload(Class<? extends Payload> clazz) {
        try {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                return clazz.newInstance();
            }
        } catch (Exception e) {
            LogManager.log("Failed to instantiate payload: " + clazz.getName());
        }
        return null;
    }

    /**
     * Finds a payload by name (case-insensitive).
     */
    public static Payload findPayloadByName(String name) {
        for (Class<? extends Payload> clazz : discoverPayloads()) {
            Payload p = loadPayload(clazz);
            if (p != null && p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}