package com.retrocrawler.app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.RetroCrawlerFactory;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.demo.collection.DemoFiles;
import com.retrocrawler.demo.collection.DemoTypes;
import com.retrocrawler.demo.collection.gear.MyKnownGear;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;

@PageTitle("Retro Crawler")
@Route(value = "retrocrawler", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class SearchView extends HorizontalLayout {

	private static final long serialVersionUID = 2777285852585235692L;

	public static final String RETRO_TEAL = "#5ad0ef";

	public static final String RETRO_BUTTON_BACK = "#feda37";

	public static final String RETRO_BUTTON_FONT = "#bd0054";

	public static final String RETRO_BACKGROUND = "#fdf4c3";

	private static final Logger logger = Logger.getLogger(SearchView.class.getSimpleName());

	public static final String INDEX_READY = "Index ready.";

	public static final String INDEX_FAILED = "Indexing failed.";

	private TreeData<MyKnownGear> parts;

	private final TreeGrid<MyKnownGear> treeGrid = new TreeGrid<>();

	private final VerticalLayout contentArea = new VerticalLayout();

	private final VerticalLayout drawer = new VerticalLayout();

	private final SplitLayout splitLayout = new SplitLayout(contentArea, drawer);

	private final ComboBox<ArchiveDescriptor> archives = new ComboBox<>();

	private final Paragraph messageBar = new Paragraph();

	private Monitor monitor;

	private final Map<ArchiveId, RetroCrawler> retroCrawler;

	private ArchiveId activeArchiveId;

	private final Image drums = drums(0);

	public SearchView() {
		setHeightFull();
		final RetroCrawlerFactory factory = new RetroCrawlerFactory();
		this.retroCrawler = Arrays.stream(DemoTypes.values()).map(dt -> reflectOn(dt, factory))
				.collect(Collectors.toUnmodifiableMap(rc -> rc.getArchiveDescriptor().getId(), Function.identity()));
		activeArchiveId = retroCrawler.keySet().iterator().next();
	}

	private static RetroCrawler reflectOn(final DemoTypes types, final RetroCrawlerFactory factory) {
		final RetroCrawler retroCrawler = factory.reflectOn(types.getTypes());
		final ArchiveDescriptor descriptor = retroCrawler.getArchiveDescriptor();
		try {
			DemoFiles.copyToWorkDirectory(descriptor);
		} catch (final IOException e) {
			throw new IllegalStateException("Failed to copy demo data to work directory.");
		}
		return retroCrawler;
	}

	@Override
	protected void onAttach(final AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		contentArea.setPadding(false);

		final UI ui = attachEvent.getUI();
		final AtomicInteger counter = new AtomicInteger(0);
		this.monitor = new Monitor(s -> ui.access(() -> {
			final int frame = counter.getAndUpdate(i -> (i + 1) % 4);
			drums.setSrc(drums(frame).getSrc());
			messageBar.setText(s);
			ui.push();
		}));

//		searchTerm = new TextField("Search Term");
//		searchButton = new Button("Search");
//		searchButton.addClickListener(e -> {
//			Notification.show("Hello " + searchTerm.getValue());
//		});
//		searchButton.addClickShortcut(Key.ENTER);
//
//		setMargin(true);
//		setVerticalComponentAlignment(Alignment.END, searchTerm, searchButton);
//
//		searchArea.add(searchTerm, searchButton);
//
//		mainLayout.add(searchArea);

		treeGrid.addHierarchyColumn(p -> p.getTitle() != null ? p.getTitle() : "<unknown>").setHeader("Title")
				.setSortable(true).setWidth("400px").setResizable(true).setFrozen(true);
		treeGrid.addComponentColumn(p -> new Anchor("file://" + p.getFolderName(), p.id + "")).setWidth("100px")
				.setHeader("Id").setResizable(true);
		treeGrid.addComponentColumn(p -> p.getPicFront().map(path -> {
			/*
			 * The StreamResource allows for loading the image from the local file system.
			 */
			final String fileName = path.getFileName().toString();
			final StreamResource resource = new StreamResource(fileName, () -> {
				try {
					return new FileInputStream(path.toFile());
				} catch (final FileNotFoundException e) {
					return null;
				}
			});
			final Image image = new Image(resource, fileName);
			image.setMaxHeight("3em");
			image.setMaxWidth("4em");

			final HorizontalLayout wrapper = new HorizontalLayout(image);
			wrapper.setPadding(false);
			wrapper.setSpacing(false);
			wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
			wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
			wrapper.setWidthFull();
			return wrapper;
		}).orElse(null)).setWidth("100px").setHeader("Image");
		treeGrid.addColumn(MyKnownGear::getFolderName).setWidth("100%").setResizable(true).setHeader("Folder Name");

		treeGrid.setHeightFull();
		treeGrid.addClassName("retro-treegrid");
		treeGrid.addSelectionListener(event -> {
			final Set<MyKnownGear> selected = event.getAllSelectedItems();
			switch (selected.size()) {
			case 1:
				drawer.setVisible(true);
				break;
			default:
				drawer.setVisible(false);
				break;
			}
		});

		final Image logo = new Image("/icons/retro_crawler_ai_slop_logo.png", "RetroCrawler AI Slop Icon");
		logo.setMaxHeight("4em");

		final Button crawl = retroButton("Reindex");
		crawl.addClickListener(event -> {
			final UI eventUI = event.getSource().getUI().orElseThrow();
			final ArchiveDescriptor location = archives.getValue();
			activeArchiveId = location.getId();
			refreshAsync(eventUI, true);
		});

		final Button cancel = retroButton("Cancel Indexing");
		cancel.addClickListener(event -> {
			monitor.cancel("Cancel requested...");
			logger.info("Repository indexing cancelled.");
		});

		final Button expand = retroButton("Expand All");
		expand.addClickListener(event -> treeGrid.expand(getParts().getRootItems()));

		final Button collapse = retroButton("Collapse All");
		collapse.addClickListener(event -> treeGrid.collapse(getParts().getRootItems()));

		// final List<ArchiveDescriptor> repoLocationList = getLocations();
		final List<ArchiveDescriptor> repoLocationList = retroCrawler.values().stream()
				.map(RetroCrawler::getArchiveDescriptor).toList();
		archives.setItems(repoLocationList);
		archives.setItemLabelGenerator(ArchiveDescriptor::getName);
		archives.setValue(repoLocationList.get(0));

		messageBar.setMaxWidth("100%");
		messageBar.getStyle().set("overflow", "hidden");
		messageBar.getStyle().set("white-space", "nowrap");
		// messageBar.getStyle().set("text-overflow", "ellipsis");

		final HorizontalLayout messageArea = new HorizontalLayout(drums, messageBar);
		messageArea.setWidthFull();
		messageArea.setAlignItems(FlexComponent.Alignment.CENTER);

		final VerticalLayout tableArea = new VerticalLayout(messageArea, treeGrid);
		tableArea.setHeightFull();
		tableArea.getStyle().set("padding-top", "0px");

		final HorizontalLayout header = createHeader(logo, crawl, cancel, expand, collapse, archives);
		final VerticalLayout headerArea = createHeaderArea(header);

		contentArea.add(headerArea, tableArea);

		final HorizontalLayout drawerHeader = createHeader();
		final VerticalLayout drawerHeaderArea = createHeaderArea(drawerHeader);

//		final TextField textField = new TextField();
		drawer.add(drawerHeaderArea);
		drawer.setPadding(false);
		// shown by selection event
		drawer.setVisible(false);

		// Hide by default
		// detailArea.setVisible(false);

		splitLayout.setWidthFull();

		splitLayout.setSplitterPosition(75);

		add(splitLayout);

		// Perform initial loading
		monitor.postUpdate("Loading index...");
		refreshAsync(attachEvent.getUI(), false);
	}

	private static VerticalLayout createHeaderArea(final Component... children) {
		final VerticalLayout headerArea = new VerticalLayout(children);
		headerArea.setPadding(false);
		headerArea.getStyle().setPaddingTop("1em");
		return headerArea;
	}

	private static HorizontalLayout createHeader(final Component... children) {
		final HorizontalLayout header = new HorizontalLayout(children);
		header.setAlignItems(FlexComponent.Alignment.CENTER);
		header.setHeight("calc(var(--lumo-space-xl) * 1.2)");
		header.getStyle().set("background-color", RETRO_TEAL);
		header.setWidthFull();
		header.getStyle().set("padding-left", "1em");
		header.getStyle().set("padding-right", "1em");
		return header;
	}

	private static Button retroButton(final String label) {
		final Button button = new Button(label);
		button.addClassName("retro-button");
		return button;
	}

	private void refreshAsync(final UI ui, final boolean reindex) {
		CompletableFuture.supplyAsync(() -> {
			try {
				final RetroCrawler activeCrawler = retroCrawler.get(activeArchiveId);
				return activeCrawler.crawl(monitor, reindex, new VaadinTreeDataFactory());
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}).thenAccept(successResult -> {
			ui.access(() -> {
				monitor.done(INDEX_READY);
				setParts(successResult);
			});
		}).exceptionally(failureException -> {
			ui.access(() -> {
				monitor.done(INDEX_FAILED + " " + failureException.getMessage());
				setParts(new TreeData<>());
				failureException.printStackTrace();
			});
			return null;
		});
	}

	public void setParts(final TreeData<MyKnownGear> tree) {
		final TreeDataProvider<MyKnownGear> partsProvider = new TreeDataProvider<MyKnownGear>(tree);
		treeGrid.setDataProvider(partsProvider);
		this.parts = tree;
	}

	public TreeData<MyKnownGear> getParts() {
		return parts;
	}

	protected Optional<Monitor> getMonitor() {
		return Optional.ofNullable(monitor);
	}

	private static final Image drums(final int i) {
		if (i < 0 || i > 3) {
			throw new IllegalArgumentException("Frame index out of bounds: " + i);
		}
		final Image image = new Image("/icons/drums-windows-98_" + (i + 1) + ".png", "Windows 98 Drums");
		image.setWidth("2em");
		image.setHeight("2em");
		return image;
	}

}
