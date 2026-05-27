# Backlog

## Page Layout

5. Add a UI setting for page padding / top offset on page breaks.
   This should let us tune receipts like `cript.html`, where stretched frames move to the next page correctly but start too close to the top edge. The setting can later drive JRXML page margins or a compensated print-area offset.

6. Clarify and fix target page format behavior.
   The UI offers page formats, but it is currently unclear what happens during generation and how source dimensions, print area, scaling, and JRXML page size interact. Make the behavior explicit for the user and ensure generated JRXML reflects the selected format.

## Preview

1. Add generated result preview.
   After JRXML generation, render the result immediately, preferably to PDF or PNG, and show it in the UI next to the source layout so the user can verify the output before downloading or using it.

## Repository Hygiene

1. Remove build artifacts and local junk from the repository.
   Clean up generated artifacts such as `BOOT-INF`, compiled classes, `target`, `.DS_Store`, and other non-source files. Add or update `.gitignore` so they do not return.

## Observability

1. Replace `System.out.println` / `System.out.printf` logging in `CoordinateEnrichmentStep`.
   Use structured application logging instead, with useful context such as a coordination/job/correlation id where available, so missing coordinate diagnostics are searchable and tied to a generation run.
