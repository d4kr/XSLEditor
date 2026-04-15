# Phase 2: Project Management - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-15
**Phase:** 02-project-management
**Areas discussed:** Config assente al primo open, UX Set Entrypoint/XML Input senza file tree, New File dialog (PROJ-06)

---

## Config assente al primo open

| Option | Description | Selected |
|--------|-------------|----------|
| Apri vuoto, salva solo quando l'utente imposta qualcosa | Nessun file creato subito. entryPoint=null, xmlInput=null. File creato al primo set. | ✓ |
| Crea subito .xslfo-tool.json vuoto | Scrive file JSON vuoto al momento dell'apertura | |
| Mostra un dialog wizard | Chiede entrypoint + XML input prima di aprire | |

**User's choice:** Apri vuoto, salva solo quando l'utente imposta qualcosa

| Option | Description | Selected |
|--------|-------------|----------|
| Permetti config parziale — campi opzionali in lettura | Rilassa ProjectConfig.read(), validazione spostata al render | ✓ |
| Config sempre completa — gestita nel controller | ProjectConfig invariato, null gestito nel controller | |

**User's choice:** Permetti config parziale — campi opzionali in lettura

---

## UX Set Entrypoint / Set XML Input senza file tree

| Option | Description | Selected |
|--------|-------------|----------|
| FileChooser dialog filtrato per tipo | DirectoryChooser posizionato nella dir progetto, filtrato su .xsl/.xslt / .xml | |
| Azioni grayed-out fino a Phase 3 | Menu action esistono ma disabilitati. Phase 3 li abilita via tree. | ✓ |
| Selezione da lista dialog | Lista file nella directory in una dialog semplice | |

**User's choice:** Azioni grayed-out fino a Phase 3

**Scope clarification:**
| Option | Description | Selected |
|--------|-------------|----------|
| Phase 2 = config, Phase 3 = tree + abilita azioni | Separazione netta tra fasi | ✓ |
| Phase 2 include FileChooser (ridefinita in Phase 3) | Duplicazione di lavoro | |

**User's choice:** Phase 2 implementa Open Project + config. Phase 3 aggiunge tree e abilita Set Entrypoint/XML Input.

---

## New File dialog (PROJ-06)

| Option | Description | Selected |
|--------|-------------|----------|
| Qualsiasi estensione — input libero | Nessuna restrizione. Developer tool, fidarsi dell'utente. | ✓ |
| Solo .xsl/.xslt/.xml | Validazione estensione nel dialog | |

**User's choice:** Qualsiasi estensione — input libero

| Option | Description | Selected |
|--------|-------------|----------|
| Solo crea il file su disco — nessuna apertura automatica | File creato, conferma visiva in Phase 3 via tree refresh | ✓ |
| Apri subito nell'editor | Non fattibile in Phase 2 (editor in Phase 4) | |

**User's choice:** Solo crea il file su disco — nessuna apertura automatica

---

## Claude's Discretion

- Exact class name per project state (ProjectContext o AppController)
- Come viene aggiunto ProjectConfig.write() (metodo, static helper, o via ProjectManager)
- Menu placement e keyboard shortcuts
- Error handling UX per directory non valide

## Deferred Ideas

- Set Entrypoint/XML Input funzionale → Phase 3
- Auto-open file in editor → Phase 4
- Validazione estensioni su New File → non necessaria
