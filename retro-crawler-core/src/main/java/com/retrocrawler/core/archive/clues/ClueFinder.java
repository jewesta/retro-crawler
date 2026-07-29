package com.retrocrawler.core.archive.clues;

/**
 * Marks an observer that extracts model-independent {@link Clue clues} from an
 * archive.
 * <p>
 * A finder may preserve a key explicitly present in its source format. It must
 * not resolve clues into facts or consult the model's known fact-key registry;
 * that interpretation belongs to gear resolution.
 */
public interface ClueFinder {

}
