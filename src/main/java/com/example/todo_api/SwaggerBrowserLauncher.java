package com.example.todo_api;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SwaggerBrowserLauncher {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerBrowserLauncher.class);
    private static final URI SWAGGER_UI_URI = URI.create("http://localhost:8080/swagger-ui/index.html");
    private static final AtomicBoolean browserOpened = new AtomicBoolean();

    @EventListener(ApplicationReadyEvent.class)
    public void openSwaggerUi(ApplicationReadyEvent event) {
        Integer localPort = event.getApplicationContext()
                .getEnvironment()
                .getProperty("local.server.port", Integer.class);
        if (localPort == null || localPort != 8080 || !browserOpened.compareAndSet(false, true)) {
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(SWAGGER_UI_URI);
                    logger.info("Opened Swagger UI in the default browser: {}", SWAGGER_UI_URI);
                    return;
                }
            }
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            logger.warn("Java desktop browsing failed; trying the Windows browser fallback.", exception);
        }

        openWithWindowsCommand();
    }

    private void openWithWindowsCommand() {
        String operatingSystem = System.getProperty("os.name", "");
        if (!operatingSystem.toLowerCase(Locale.ROOT).startsWith("windows")) {
            logger.warn("Could not open Swagger UI automatically: Java desktop browsing is unavailable "
                    + "and the Windows browser fallback cannot be used on {}.", operatingSystem);
            return;
        }

        try {
            new ProcessBuilder("cmd.exe", "/c", "start", "", SWAGGER_UI_URI.toString()).start();
            logger.info("Requested Windows to open Swagger UI in the default browser: {}", SWAGGER_UI_URI);
        } catch (IOException | SecurityException exception) {
            logger.error("Could not launch the default browser for Swagger UI: {}", SWAGGER_UI_URI, exception);
        }
    }
}
