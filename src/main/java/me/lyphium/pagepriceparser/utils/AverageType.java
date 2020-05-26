package me.lyphium.pagepriceparser.utils;

import lombok.Getter;

import java.util.Calendar;

@Getter
public enum AverageType {

    DAY(0, "Tag", "Day"),
    WEEK(-1, "Woche", "Week"),

    MONDAY(Calendar.MONDAY, "Montag", "Monday"),
    TUESDAY(Calendar.TUESDAY, "Dienstag", "Tuesday"),
    WEDNESDAY(Calendar.WEDNESDAY, "Mittwoch", "Wednesday"),
    THURSDAY(Calendar.THURSDAY, "Donnerstag", "Thursday"),
    FRIDAY(Calendar.FRIDAY, "Freitag", "Friday"),
    SATURDAY(Calendar.SATURDAY, "Samstag", "Saturday"),
    SUNDAY(Calendar.SUNDAY, "Sonntag", "Sunday");

    private final int weekDay;
    private final String[] names;

    AverageType(int weekDay, String... names) {
        this.weekDay = weekDay;
        this.names = names;
    }

    public String getName() {
        return names[0];
    }

    public static AverageType getByName(String name) {
        name = name.trim().toLowerCase();
        for (AverageType type : values()) {
            for (String s : type.names) {
                if (s.toLowerCase().startsWith(name)) {
                    return type;
                }
            }
        }
        return null;
    }

}
