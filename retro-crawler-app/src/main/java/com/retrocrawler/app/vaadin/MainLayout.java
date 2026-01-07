package com.retrocrawler.app.vaadin;

import com.vaadin.flow.component.applayout.AppLayout;

public class MainLayout extends AppLayout {

	private static final long serialVersionUID = 8332273030146356725L;

	public MainLayout() {
		addClassName("retro-layout");
		setPrimarySection(Section.DRAWER);
		setDrawerOpened(false);
	}

}