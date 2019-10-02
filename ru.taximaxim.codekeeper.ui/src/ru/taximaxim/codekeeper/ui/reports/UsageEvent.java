package ru.taximaxim.codekeeper.ui.reports;

/**
 *  Represents a data which will be send to Google Analytics to track the user's event
 */
public class UsageEvent {

    private final UsageEventType type;
    private String label;
    private Integer value;

    public UsageEvent(UsageEventType type, String label, Integer value) {
        if (type == null) {
            throw new IllegalArgumentException("Type name may not be null"); //$NON-NLS-1$
        }
        this.type = type;
        if (type.getValueDescription() == null && value != null) {
            throw new IllegalArgumentException("The value of this event may not be null since its event type has a value description"); //$NON-NLS-1$
        }
        this.label = label;
        this.value = value;
    }

    /**
     * @return the event type
     */
    public UsageEventType getType() {
        return type;
    }

    /**
     * @return the event label name. May be null.
     */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the event value. May be null.
     */
    public Integer getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{Type=").append(type); //$NON-NLS-1$
        if (label != null) {
            sb.append("; Label=\"").append(label).append('"'); //$NON-NLS-1$
        }
        if (value != null) {
            sb.append("; Value=\"").append(value).append('"'); //$NON-NLS-1$
        }
        sb.append('}');
        return sb.toString();
    }

    public UsageEvent copy() {
        return new UsageEvent(this.type, this.label, this.value);
    }
}