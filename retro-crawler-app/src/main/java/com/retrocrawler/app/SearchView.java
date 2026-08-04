package com.retrocrawler.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.JsonFileRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.progress.ProgressStage;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.demo.DemoModels;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
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
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

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

	private TreeData<VaadinGearNode> parts;

	private final TreeGrid<VaadinGearNode> treeGrid = new TreeGrid<>();

	private final VerticalLayout contentArea = new VerticalLayout();

	private final VerticalLayout drawer = new VerticalLayout();

	private final SplitLayout splitLayout = new SplitLayout(contentArea, drawer);

	private final ComboBox<DemoArchive> archives = new ComboBox<>();

	private final Paragraph messageBar = new Paragraph();

	private Progressor progressor;

	private final List<DemoArchive> demoArchives;

	private DemoArchive activeArchive;

	private final Image drums = drums(0);

	public SearchView() {
		setHeightFull();
		final Repository repository = new JsonFileRepository();
		final List<DemoArchive> configuredArchives = new ArrayList<>();
		try {
			for (final DemoModels model : DemoModels.values()) {
				configuredArchives.addAll(DemoArchive.create(model, repository));
			}
		} catch (final IOException e) {
			throw new IllegalStateException("Failed to materialize demo archive data.", e);
		}
		demoArchives = List.copyOf(configuredArchives);
		activeArchive = demoArchives.getFirst();
	}

	@Override
	protected void onAttach(final AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		contentArea.setPadding(false);

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

		treeGrid.addHierarchyColumn(p -> p.gear().getTitle() != null ? p.gear().getTitle() : "<unknown>")
				.setHeader("Title").setSortable(true).setWidth("400px").setResizable(true).setFrozen(true);
		treeGrid.addComponentColumn(this::openFolderButton).setWidth("100px").setHeader("Id").setResizable(true);
		treeGrid.addComponentColumn(node -> archiveImage(node).orElse(null)).setWidth("100px").setHeader("Image");
		treeGrid.addColumn(p -> p.gear().getFolderName()).setWidth("100%").setResizable(true).setHeader("Folder Name");

		treeGrid.setHeightFull();
		treeGrid.addClassName("retro-treegrid");
		treeGrid.addSelectionListener(event -> {
			final Set<VaadinGearNode> selected = event.getAllSelectedItems();
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
			activeArchive = Objects.requireNonNull(archives.getValue(), "selected archive");
			refreshAsync(eventUI, ReindexScope.all());
		});

		final Button cancel = retroButton("Cancel Indexing");
		cancel.addClickListener(event -> {
			progressor.cancel("Cancel requested...");
			logger.info("Repository indexing cancelled.");
		});

		final Button expand = retroButton("Expand All");
		expand.addClickListener(event -> treeGrid.expand(getParts().getRootItems()));

		final Button collapse = retroButton("Collapse All");
		collapse.addClickListener(event -> treeGrid.collapse(getParts().getRootItems()));

		archives.setItems(demoArchives);
		archives.setItemLabelGenerator(DemoArchive::label);
		archives.setValue(activeArchive);

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
		refreshAsync(attachEvent.getUI(), ReindexScope.none());
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

	private Button openFolderButton(final VaadinGearNode node) {
		final Button button = new Button(node.gear().id.toString());
		button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
		final DemoArchive archive = activeArchive;
		if (archive.folderOpener().isPresent()) {
			button.setTooltipText("Open archive folder");
			button.addClickListener(event -> openArchiveFolder(archive, node.sourcePath()));
		} else {
			button.setEnabled(false);
			button.setTooltipText("This archive source does not expose local folders");
		}
		return button;
	}

	private void openArchiveFolder(final DemoArchive archive, final Path sourcePath) {
		try {
			archive.folderOpener().orElseThrow().open(sourcePath, archive.archive().paths());
		} catch (final IOException | IllegalArgumentException failure) {
			logger.warning("Could not open archive folder: " + failure.getMessage());
			Notification.show("Could not open archive folder: " + failure.getMessage(), 5000, Position.MIDDLE);
		}
	}

	private void refreshAsync(final UI ui, final ReindexScope reindexScope) {
		final DemoArchive archive = activeArchive;
		final Progressor activeProgressor = createProgressor(ui);
		final RetroCrawler crawler = archive.crawler();
		this.progressor = activeProgressor;
		activeProgressor.indeterminate(ProgressStage.of("LOADING"), "Loading index...");
		CompletableFuture.supplyAsync(() -> {
			try {
				return crawler.crawl(archive.archive().id(), activeProgressor, reindexScope,
						new VaadinTreeDataFactory());
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}).thenAccept(successResult -> ui.access(() -> {
			activeProgressor.complete(INDEX_READY);
			setParts(successResult);
		})).exceptionally(failureException -> {
			ui.access(() -> {
				activeProgressor.fail(INDEX_FAILED + " " + failureException.getMessage());
				setParts(new TreeData<>());
				failureException.printStackTrace();
			});
			return null;
		});
	}

	private Optional<Component> archiveImage(final VaadinGearNode node) {
		final DemoArchive archive = activeArchive;
		final RetroCrawler crawler = archive.crawler();
		return node.gear().getPicFront().map(path -> {
			final String fileName = path.getFileName().toString();
			final DownloadHandler download = DownloadHandler.fromInputStream(event -> {
				final Optional<byte[]> content = crawler.inspect(archive.archive().id(), path,
						InputStream::readAllBytes);
				if (content.isEmpty()) {
					return DownloadResponse.error(404, "Archive source did not expose content for: " + path);
				}
				final byte[] bytes = content.get();
				return new DownloadResponse(new ByteArrayInputStream(bytes), fileName, "image/jpeg", bytes.length);
			}).inline();
			final Image image = new Image(download, fileName);
			image.setMaxHeight("3em");
			image.setMaxWidth("4em");

			final HorizontalLayout wrapper = new HorizontalLayout(image);
			wrapper.setPadding(false);
			wrapper.setSpacing(false);
			wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
			wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
			wrapper.setWidthFull();
			return wrapper;
		});
	}

	private Progressor createProgressor(final UI ui) {
		final AtomicInteger counter = new AtomicInteger(0);
		return Progressor.reportingMessages(message -> ui.access(() -> {
			final int frame = counter.getAndUpdate(i -> (i + 1) % 4);
			drums.setSrc(drums(frame).getSrc());
			messageBar.setText(message);
			ui.push();
		}));
	}

	private void setParts(final TreeData<VaadinGearNode> tree) {
		final TreeDataProvider<VaadinGearNode> partsProvider = new TreeDataProvider<>(tree);
		treeGrid.setDataProvider(partsProvider);
		this.parts = tree;
	}

	private TreeData<VaadinGearNode> getParts() {
		return parts;
	}

	protected Optional<Progressor> getProgressor() {
		return Optional.ofNullable(progressor);
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
