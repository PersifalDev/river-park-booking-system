package ru.haritonenko.commonlibs.observability;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

import java.util.Locale;
import java.util.regex.Pattern;

public class PiiMaskingStructuredLoggingJsonMembersCustomizer
        implements StructuredLoggingJsonMembersCustomizer<Object> {

    private static final String MASK = "***";
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\d)");

    @Override
    public void customize(JsonWriter.Members<Object> members) {
        members.applyingValueProcessor(new JsonWriter.ValueProcessor<String>() {
            @Override
            public String processValue(JsonWriter.MemberPath path, String value) {
                if (value == null) {
                    return null;
                }
                if (isSensitivePath(path)) {
                    return MASK;
                }
                return maskInlinePii(value);
            }
        }.whenInstanceOf(String.class));
    }

    private boolean isSensitivePath(JsonWriter.MemberPath path) {
        String text = path.toUnescapedString().toLowerCase(Locale.ROOT);
        return text.contains("password")
                || text.contains("secret")
                || text.contains("token")
                || text.contains("authorization")
                || text.contains("credential")
                || text.contains("email")
                || text.contains("phone")
                || text.contains("passport");
    }

    private String maskInlinePii(String value) {
        String masked = EMAIL.matcher(value).replaceAll(MASK);
        return PHONE.matcher(masked).replaceAll(MASK);
    }
}
