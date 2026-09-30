package io.github.vncntz.hris.sharedkernel;

/** Caller-supplied details for one material action. Context is a short, non-sensitive summary. */
public record AuditRequest(String actorReference, String action, String targetType,
                           String targetReference, String reason, String context) {
    public AuditRequest {
        actorReference = checked(actorReference, "actorReference", 128, true);
        action = checked(action, "action", 64, true);
        targetType = checked(targetType, "targetType", 64, true);
        targetReference = checked(targetReference, "targetReference", 128, true);
        reason = checked(reason, "reason", 512, false);
        context = checked(context, "context", 1024, false);
    }

    private static String checked(String value, String name, int maxLength, boolean required) {
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException(name + " is required");
            }
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " exceeds " + maxLength + " characters");
        }
        if (normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " contains a control character");
        }
        return normalized;
    }
}
