## T1 ProjectManager

- [x] T1.1 Create Project class
- [x] T1.2 Load project from path
- [x] T1.3 Parse config file
- [x] T1.4 Validate config fields

---

## T2 File Model

- [x] T2.1 Create File class
- [x] T2.2 Implement dirty flag
- [x] T2.3 Implement load/save

---

## T3 DependencyResolver

- [x] T3.1 Parse xsl:include
- [x] T3.2 Parse xsl:import
- [x] T3.3 Build dependency graph
- [x] T3.4 Detect circular dependencies

---

## T4 LibraryPreprocessor

- [x] T4.1 Detect LIBRARY directive
- [x] T4.2 Resolve file path
- [x] T4.3 Merge file content
- [x] T4.4 Handle missing library error

---

## T5 ValidationEngine

- [x] T5.1 Validate XML well-formedness
- [x] T5.2 Validate XSLT files
- [x] T5.3 Aggregate errors

---

## T6 RenderEngine (XSLT)

- [x] T6.1 Integrate Saxon
- [x] T6.2 Execute transformation
- [x] T6.3 Render FO to PDF

---

## T7 Render Orchestrator

- [x] T7.1 Full render pipeline
- [x] T7.2 Structured result
- [x] T7.3 Handle rendering errors

---

## T8 PreviewManager

- [x] T8.1 Store current PDF
- [x] T8.2 Replace on success
- [x] T8.3 Mark outdated on failure

---

## T9 ErrorManager

- [x] T9.1 Normalize errors
- [x] T9.2 Map file positions
- [x] T9.3 Expose to UI

---

## T10 LogManager

- [ ] T10.1 Create log structure
- [ ] T10.2 Add entries
- [ ] T10.3 Filter logs