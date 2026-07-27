package com.retrocrawler.model.identifier;

/**
 * A public content category in The Retro Web.
 *
 * <p>
 * This is The Retro Web's taxonomy, not a RetroCrawler gear taxonomy. Some
 * categories describe physical gear, while others describe documentation,
 * software, manufacturers, or standardized hardware vocabulary.
 */
public enum TheRetroWebCategory {

	MOTHERBOARD("motherboards"),
	BIOS_IMAGE("bios"),
	CHIP("chips"),
	CHIPSET("chipsets"),
	EXPANSION_CARD("expansioncards"),
	HARD_DRIVE("harddrives"),
	OPTICAL_DRIVE("cddrives"),
	FLOPPY_OR_TAPE_DRIVE("floppydrives"),
	DRIVER_OR_SOFTWARE("drivers"),
	CHIP_FAMILY("family"),
	IO_PORT("io-ports"),
	EXPANSION_SLOT("expansion-slots"),
	SOCKET("sockets"),
	POWER_CONNECTOR("power-connectors"),
	MANUFACTURER("manufacturers");

	private final String path;

	TheRetroWebCategory(final String path) {
		this.path = path;
	}

	public String path() {
		return path;
	}

	public TheRetroWebReference reference(final TheRetroWebId id) {
		return new TheRetroWebReference(this, id);
	}
}
