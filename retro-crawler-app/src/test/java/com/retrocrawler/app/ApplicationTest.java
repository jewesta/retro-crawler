package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

class ApplicationTest {

	@Test
	void usesAutomaticWebSocketPushForAsynchronousUiUpdates() {
		final Push push = Application.class.getAnnotation(Push.class);

		assertNotNull(push);
		assertEquals(PushMode.AUTOMATIC, push.value());
		assertEquals(Transport.WEBSOCKET, push.transport());
	}
}
