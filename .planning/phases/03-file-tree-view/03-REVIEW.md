---
phase: 03-file-tree-view
reviewed: 2026-04-17T00:00:00Z
depth: standard
files_reviewed: 7
files_reviewed_list:
  - src/main/java/ch/ti/gagi/xlseditor/ui/FileItem.java
  - src/main/java/ch/ti/gagi/xlseditor/ui/FileItemTreeCell.java
  - src/main/java/ch/ti/gagi/xlseditor/ui/FileTreeController.java
  - src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java
  - src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java
  - src/main/resources/ch/ti/gagi/xlseditor/ui/main.css
  - src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java
findings:
  critical: 0
  warning: 3
  info: 2
  total: 5
status: issues_found
---

# Phase 3: Code Review Report

**Reviewed:** 2026-04-17
**Depth:** standard
**Files Reviewed:** 7
**Status:** issues_found

## Summary

Phase 3 introduces the file tree view with `FileItem`, `FileItemTreeCell`,
`FileTreeController`, and extensions to `MainController` and `ProjectContext`.
The overall structure is sound: immutable data carrier, clean cell factory with
correct JavaFX cell recycling hygiene, well-factored sub-controller, and good
security hardening around path traversal in `ProjectContext`.

Three warnings stand out:

1. `ProjectContext.createFile` breaks the sort invariant that `openProject`
   establishes — newly created files are appended out of order without being
   inserted in sorted position.
2. `mountTree()` in `FileTreeController` casts a child node by position
   (index 0) rather than by field reference, creating a fragile dependency on
   `buildTreeView`'s internal child ordering.
3. `MainController.handleOpenProject` dereferences `System.getProperty("user.home")`
   without a null guard before passing it to `new File(...)`.

No critical (security or data-loss) issues were found.

---

## Warnings

### WR-01: `createFile` breaks the lexicographic sort invariant

**File:** `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java:112`

**Issue:** `openProject` / `refreshProjectFiles` populates `projectFiles` in
lexicographic order (guaranteed by the `sorted()` step at line 73). However,
`createFile` appends the new path at the end of the list (line 112) without
inserting it in sorted position. After a `createFile` call the tree will show
the new file at the bottom regardless of its alphabetical rank. The sort
invariant is restored only on the next `openProject`. The existing test
`createFileAppendsToProjectFiles` only asserts `contains()`, so the regression
is not caught.

**Fix:** Insert the new path in sorted position rather than appending blindly:

```java
// ProjectContext.java — createFile(), replace the final append
Path relative = currentProject.rootPath().toAbsolutePath().normalize().relativize(target);

// Maintain lexicographic sort order (same comparator as refreshProjectFiles)
int insertionIndex = Collections.binarySearch(
    projectFiles, relative, Comparator.comparing(Path::toString));
if (insertionIndex < 0) insertionIndex = -(insertionIndex + 1);
projectFiles.add(insertionIndex, relative);
```

Add a companion test that asserts position, not just presence:

```java
@Test
void createFileInsertsInSortedOrder(@TempDir Path tempDir) throws IOException {
    ProjectContext ctx = new ProjectContext();
    ctx.openProject(tempDir);
    ctx.createFile("zebra.xsl");
    ctx.createFile("apple.xsl");
    ctx.createFile("middle.xsl");
    ObservableList<Path> files = ctx.projectFilesProperty();
    assertEquals(Path.of("apple.xsl"),  files.get(0));
    assertEquals(Path.of("middle.xsl"), files.get(1));
    assertEquals(Path.of("zebra.xsl"),  files.get(2));
}
```

---

### WR-02: Fragile positional cast in `mountTree`

**File:** `src/main/java/ch/ti/gagi/xlseditor/ui/FileTreeController.java:266`

**Issue:** `mountTree` retrieves the header label by index:

```java
Label header = (Label) treeContainer.getChildren().get(0);
```

This relies on `buildTreeView` always placing the `Label` as the first child of
`treeContainer`. If a future change (e.g., adding a toolbar row above the
header) shifts the order, this produces a `ClassCastException` at runtime with
a misleading stack trace. The implicit invariant is documented only in a
comment, not enforced by the type system.

**Fix:** Store the header label as a field and reference it directly:

```java
// FileTreeController — add field
private Label treeHeader;

// buildTreeView — assign the field
treeHeader = new Label();
treeHeader.getStyleClass().add("file-tree-header");
treeHeader.setMaxWidth(Double.MAX_VALUE);
treeHeader.setAlignment(Pos.CENTER_LEFT);

treeContainer = new VBox();
treeContainer.getChildren().addAll(treeHeader, fileTree);

// mountTree — use field instead of positional cast
treeHeader.setText(project.rootPath().getFileName() + "/");
```

---

### WR-03: Potential `NullPointerException` in `handleOpenProject` via `user.home`

**File:** `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java:134`

**Issue:** `System.getProperty("user.home")` can return `null` on certain JVM
configurations (custom security managers, stripped-down container images). When
null is passed to `new File(null)`, a `NullPointerException` is thrown, which
is uncaught here and will propagate to the JavaFX event dispatch thread,
potentially crashing the application rather than showing a graceful error.

**Fix:** Null-check and fall back to the current working directory:

```java
String home = System.getProperty("user.home");
if (home != null) {
    chooser.setInitialDirectory(new File(home));
}
// If user.home is unavailable, DirectoryChooser opens at the OS default
```

---

## Info

### IN-01: `FileItemTreeCell.updateItem` missing `setGraphic(null)` in empty branch

**File:** `src/main/java/ch/ti/gagi/xlseditor/ui/FileItemTreeCell.java:48-52`

**Issue:** The JavaFX `TreeCell` contract requires that the empty/null branch
resets ALL display properties of a recycled cell. `setText(null)` is present,
but `setGraphic(null)` is absent. Currently no graphic is ever set in the
non-empty branch, so this is harmless. However, if Phase 4 or later adds an
icon `setGraphic(...)` call for one of the roles, recycled cells will display
stale icons for empty rows — the same class of bug that the `removeAll` on
line 46 correctly guards against for CSS classes.

**Fix:** Add `setGraphic(null)` to the empty branch as a defensive precaution:

```java
if (empty || item == null) {
    setText(null);
    setGraphic(null);   // add this line
    setTooltip(null);
    return;
}
```

---

### IN-02: `validateSimpleFilename` rejects valid filenames containing `".."`

**File:** `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java:194`

**Issue:** The traversal guard uses `filename.contains("..")`, which is
intentionally conservative but rejects legitimate filenames such as
`"report..v2.xsl"` or `"foo..bar.xml"`. The path-separator checks on lines
190-191 already block subdirectory traversal (`../`). The `contains("..")`
check therefore only provides value against the bare string `".."` itself or
sequences like `"..foo"` (which has no traversal power on POSIX systems).

This is a product-level decision (developer tool, internal use only), but worth
documenting so Phase 4 does not confuse the rejection for a bug when a user
names a file with double dots.

**Fix (optional):** If broader filename acceptance is desired, replace the
string check with a path-element check:

```java
// Instead of: filename.contains("..")
// Check that no path element equals ".." (safer and more precise)
Path p = Path.of(filename);
for (int i = 0; i < p.getNameCount(); i++) {
    if ("..".equals(p.getName(i).toString())) {
        throw new IllegalArgumentException(
            "File name must not contain parent references: " + filename);
    }
}
```

If the current conservative behavior is intentional, add a comment to
`validateSimpleFilename` explaining why double-dot in the middle of a name is
also rejected.

---

_Reviewed: 2026-04-17_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
