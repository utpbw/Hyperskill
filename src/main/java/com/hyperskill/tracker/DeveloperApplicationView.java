package com.hyperskill.tracker;

public record DeveloperApplicationView(
    Long id,
    String name,
    String description,
    String apikey,
    String category
) {

    public static DeveloperApplicationView from(Application application) {
        return new DeveloperApplicationView(
            application.getId(),
            application.getName(),
            application.getDescription(),
            application.getApiKey(),
            application.getCategory()
        );
    }
}
