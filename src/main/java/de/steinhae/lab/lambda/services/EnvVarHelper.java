package de.steinhae.lab.lambda.services;

import java.util.LinkedList;
import java.util.List;

public class EnvVarHelper {

    @SafeVarargs
    public static <E extends Enum<E>> void throwExceptionIfEnvVarMissing(E ... values) {
        List<String> missing = new LinkedList<>();
        for (Enum<E> enumKey : values) {
            String key = enumKey.name();
            if (!envVarPresent(key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("Please provide env vars: " + String.join(", ", missing));
        }
    }

    public static <E extends Enum<E>> String getValue(E key) {
        return getValueOfEnvVar(key.name());
    }

    private static String getValueOfEnvVar(String var) {
        return System.getenv(var);
    }

    private static boolean envVarPresent(String env) {
        return getValueOfEnvVar(env) != null;
    }


}
