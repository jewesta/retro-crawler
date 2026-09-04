# Issue 41: Model Printers

## Intent

Recognize printers as a dedicated Gear type from evidence in their own archive
folder. The initial collection evidence is the German `9-Nadel` tag used by
9-pin dot-matrix printers.

## Decisions

1. Add one collection `Printer` Gear type rather than separate Gear types for
   each printing technology.
2. Represent the technology as a typed `PrinterType` Fact. Keep the objective
   enum in the portable shared model, but keep its parsing in the collection:
   there is no public or technical representation boundary establishing the
   collection's `9-Nadel` vocabulary.
3. Use an extensive set of established English industry names for impact,
   inkjet, electrophotographic, thermal, dye-sublimation, and solid-ink
   mechanisms. Do not retain a generic `DOT_MATRIX` value: preserve the
   collection's explicitly recorded needle count by mapping `9-Nadel` directly
   to `DOT_MATRIX_9_PIN`, as the clue's one Fact.
4. Recognize a printer only from its resolved printer-type Fact. A title or
   ancestor folder concerning printers does not establish what an artifact is,
   so supplies remain fallback Gear unless they carry their own type evidence.
5. Keep the collection parser descriptive rather than predictive. It currently
   maps only the exact `9-Nadel` tag present in the collection. An exhaustive
   enum switch lists the types for which no tag is known yet without declaring
   possible future vocabulary forbidden.

## Progress

- Added and documented the shared printer-type vocabulary, including character
  and line impact mechanisms as well as modern non-impact mechanisms.
- Added the collection parser for its current exact `9-Nadel` term.
- Added the `Printer` Gear declaration and matcher.

## Verification

- Focused parser and collection-model tests cover the exact `9-Nadel` mapping,
  printer resolution, and the absence of title-based accessory classification.
- The canonical formatter passes for all seven changed Java sources.
- The complete nine-module `mvn test` reactor passes.
- The complete nine-module `mvn clean install` packaged reactor passes.
