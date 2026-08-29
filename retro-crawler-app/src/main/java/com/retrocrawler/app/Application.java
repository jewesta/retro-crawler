package com.retrocrawler.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SuppressWarnings("serial")
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("themes/retro-crawler/styles.css")
@Push(transport = Transport.WEBSOCKET)
public class Application implements AppShellConfigurator {

	public static void main(final String[] args) {
		final SpringApplication application = new SpringApplication(Application.class);
		application.setHeadless(false);
		application.run(args);
	}

}
