# Issue 41: Model Printers and Processors

## Intent

Recognize printers as a dedicated Gear type from evidence in their own archive
folder. The initial collection evidence is the German `9-Nadel` tag used by
9-pin dot-matrix printers.

Recognize AMD and Intel processors from manufacturer production markings,
including processors stored below a mainboard artifact. Correct collection
tags that describe those markings as serial numbers without reclassifying
values whose meaning remains ambiguous.

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
6. Model `Processor` as an abstract Gear type with `AmdProcessor` and
   `IntelProcessor` specializations. The marking format establishes those two
   manufacturers. Do not add K6-2, Pentium, or other family classes until a
   clue independently establishes the family; neither the artifact title nor
   its archive ancestors are type evidence.
7. Represent the observed package value as one typed `ProcessorMarking` Fact.
   Its AMD and Intel value subtypes preserve the one-clue/one-Fact rule while
   exposing the objectively identifiable components of that one marking.
8. Treat the AMD form as a production marking rather than claiming that the
   entire value is a batch number. Retain its prefix and suffix as opaque text,
   and expose only the intervening three- or four-digit production date code.
   AMD documentation identifies comparable text as a production marking but
   does not publicly define every component across generations.
9. Treat the supported Intel form as an FPO batch number with an optional
   partial ATPO serial suffix. Keep those components together as the package
   marking observed by the clue.
10. Normalize only collection values whose shapes are sufficiently specific:
    remove the erroneous `SN` key, replace separators in supported AMD
    production markings with hyphens, and leave supported Intel FPO markings
    otherwise unchanged. Do not change genuine serial numbers or shapes that
    are not yet understood.

## Progress

- Added and documented the shared printer-type vocabulary, including character
  and line impact mechanisms as well as modern non-impact mechanisms.
- Added the collection parser for its current exact `9-Nadel` term.
- Added the `Printer` Gear declaration and matcher.
- Added the portable processor-marking value hierarchy and canonical parser.
- Added the collection `Processor`, `AmdProcessor`, and `IntelProcessor` Gear
  hierarchy with marking-based matchers.
- Renamed 135 high-confidence collection folders: 76 AMD and 59 Intel. Of
  these, 39 are processor artifacts nested beneath mainboards. Each rename is
  recorded in the archive journal and in a private recovery manifest.
- Left 47 Intel or AMD processor folders unchanged because their values are
  ambiguous or use actual serial-number forms.

## Verification

- Focused parser and collection-model tests cover the exact `9-Nadel` mapping,
  printer resolution, and the absence of title-based accessory classification.
- The canonical formatter passes for all changed Java sources.
- The complete nine-module `mvn test` reactor passes.
- The complete nine-module `mvn clean install` packaged reactor passes.
- Focused processor parser and collection-model tests cover AMD production
  markings, Intel FPO/partial-ATPO markings, manufacturer-specific Gear
  resolution, and a processor nested below a mainboard.
- Post-migration verification found all 135 targets, no remaining source
  folders, no malformed normalized values, and the exact 135 journal entries.

## Processor-marking references

- Intel documents the FPO as the processor batch number and the ATPO as its
  serial number in [Where Can I Find Intel Processor Boxed Part Numbers (FPO
  and ATPO)?](https://www.intel.com/content/www/us/en/support/articles/000005609/processors.html).
- AMD's [AMD-K6 Processor Revision
  Guide](https://www.ardent-tool.com/CPU/docs/AMD/K6/revs/21641d.pdf) describes
  the comparable package field as a production marking and leaves most of its
  characters internally defined.
