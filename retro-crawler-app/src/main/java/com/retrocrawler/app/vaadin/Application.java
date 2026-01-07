package com.retrocrawler.app.vaadin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SuppressWarnings("serial")
@SpringBootApplication
@Theme(value = "retro-crawler")
@EnableAsync
@Push(value = PushMode.MANUAL, transport = Transport.WEBSOCKET)
public class Application implements AppShellConfigurator {

	public static void main(final String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
