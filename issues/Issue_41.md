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
6. Model processor identity as a concrete Gear hierarchy. `Processor` matches
   any processor marking, `AmdProcessor` and `IntelProcessor` match the marking
   subtype, and family types combine that marking with the existing `title`
   Fact. Equal `EXACT` matches along one inheritance chain deliberately resolve
   to the most-specific Gear type under Issue 12's selection rule.
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
11. Keep processor-family knowledge in Gear matchers rather than synthesize a
    `ProcessorType` or other meta-Fact. A family matcher requires both a
    manufacturer-specific package marking and family language already present
    in the title. Archive ancestry remains irrelevant, and an unfamiliar title
    retains the most-specific identity the available evidence actually proves.
12. Rate the public model's broad `v`-plus-digit version syntax as `STRONG`
    rather than `EXACT`. A valid Intel FPO beginning with `V` satisfies both
    shapes, but its constrained production-week structure is the more specific
    anonymous-clue interpretation. Explicitly keyed version clues are
    unaffected.
13. Build quantitative model values on the Units of Measurement API (JSR 385),
    with Indriya as its implementation, instead of growing a parallel
    RetroCrawler `Number`-plus-unit hierarchy. Keep both dependencies in the
    optional shared-model module so the core framework remains independent.
14. Use standard quantity types where their meaning fits and dedicated custom
    `Quantity` subtypes for stable non-SI properties. Do not erase all integral
    counts to one generic dimensionless quantity type: parallel bus width,
    serial lane count, and package terminal count are different properties even
    though their units share the physical dimension of a count.
15. Establish and verify the quantity foundation before adding processor clock,
    expansion-bus width, or package Facts. Existing measurement values remain
    unchanged in this step; migrating them is a separate compatibility decision,
    not a prerequisite for new quantities.

## Progress

- Added and documented the shared printer-type vocabulary, including character
  and line impact mechanisms as well as modern non-impact mechanisms.
- Added the collection parser for its current exact `9-Nadel` term.
- Added the `Printer` Gear declaration and matcher.
- Added the portable processor-marking value hierarchy and canonical parser.
- Added the collection `Processor`, `AmdProcessor`, and `IntelProcessor` Gear
  hierarchy with marking-based matchers.
- Made `Processor`, `AmdProcessor`, and `IntelProcessor` matched Gear types and
  added the processor identities currently supported by collection evidence:
  AMD 8086, 80286, 80386, 80486, 5x86, K5, K6, K6-2, and K6-III; Intel 80286,
  80386, 80486, Pentium, Pentium MMX, Pentium 4, Core, and math coprocessors.
- Grouped the complete processor Gear hierarchy in the collection's dedicated
  `gear.cpu` package.
- Added title-and-marking family matchers. A title never establishes processor
  identity by itself, while Issue 12 resolves the intentional matches for the
  general, manufacturer, family, and subfamily types through inheritance.
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
- A focused hierarchy test covers every supported processor family, generic
  manufacturer fallback, the K6 and Pentium sub-hierarchies, and rejection of
  a family title that contradicts the package-marking manufacturer. It also
  covers an Intel FPO beginning with `V`, which overlaps the broad version
  syntax.
- Added the JSR 385 API and Indriya reference implementation to the optional
  shared-model module as the foundation for quantitative Facts.
- Documented the shared-model quantity boundary and proved standard frequency
  conversion plus distinct custom count quantity types without introducing the
  processor clock, expansion-bus width, or package models yet.
- Verified the foundation with the focused core/model reactor (302 core and 67
  model tests), confirmed the measurement dependencies are absent from core,
  and reran the complete nine-module `mvn clean install` reactor successfully.
- A live audit resolves all 135 normalized processor roots as processors
  without a genuine matcher tie: 134 use 16 fine-grained identities, while one
  unfamiliar AMD title remains at `AmdProcessor` rather than acquiring an
  invented family.
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
