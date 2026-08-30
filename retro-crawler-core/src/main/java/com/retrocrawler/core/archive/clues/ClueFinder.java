package com.retrocrawler.core.archive.clues;

/**
 * Marks an observer that extracts model-independent {@link Clue clues} from an
 * archive.
 * <p>
 * A finder may preserve a key explicitly present in its source format. It must
 * not resolve clues into facts or consult the model's known fact-key registry;
 * that interpretation belongs to gear resolution.
 * <p>
 * A configured finder must be a named class whose simple name is unique among
 * all finders in the archive definition. The crawler retains that name with
 * every clue the finder produces, so anonymous classes and lambdas are not
 * valid configured finders.
 */
public interface ClueFinder {

	/**
	 * Examines one candidate artifact after all of its child folders have been
	 * classified.
	 * <p>
	 * The view contains the candidate folder's own name and direct files. Its
	 * nested folders are limited to children that were positively established
	 * as clue-free metadata folders; child artifacts and failed folders are
	 * structurally absent. Every returned clue belongs to the view's root
	 * folder. A clue may explicitly name contributing resources through their
	 * {@link com.retrocrawler.core.archive.ARI ARIs}; access alone never
	 * implies provenance.
	 */
	Clues find(ArchiveFolderView folder);

}
