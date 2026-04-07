# PRD — XSL-FO Development Tool (Internal)

## 1. Overview
Tool interno desktop per sviluppatori per editare template XSLT/XSL-FO multi-file, generare PDF on-demand e fare debugging avanzato in locale.

## 2. Problem Statement
- Editing limitato (textarea)
- Ciclo lento (cambio pagina + sistema esterno)
- Nessun feedback sugli errori
- Complessità multi-file non gestita

## 3. Objectives
- Ridurre drasticamente il tempo di sviluppo
- Eliminare cambio contesto
- Migliorare debugging e comprensione codice

## 4. Users
- Sviluppatori interni
- Competenze tecniche (XML/XSLT)
- Pochi utenti, uso locale

## 5. Developer Experience Goals
- Ciclo modifica → preview < 5s
- Feedback errori immediato
- Navigazione fluida tra template
- Zero dipendenze da sistemi esterni

## 6. Scope (MVP)

### 6.1 Project-based Workflow
- Supporto progetti multi-file
- Import da filesystem
- Definizione entrypoint XSLT
- Gestione dipendenze tra file

### 6.2 Editor
- Syntax highlighting XML/XSLT
- Autocomplete XSL/XSL-FO
- Validazione XML real-time
- Ricerca multi-file
- Highlight variabili
- Navigazione template (go to definition)

### 6.3 Pipeline
XML → XSLT → XSL-FO → PDF

### 6.4 Preview
- Split view (editor + PDF)
- Rendering manuale (button)

### 6.5 XML Input
- Supporto multi XML
- Switch rapido tra input

### 6.6 Error Handling
- Errori XML/XSLT (syntax)
- Errori XSLT runtime
- Errori FO rendering
- Messaggi user-friendly
- Navigazione da errore a codice

### 6.7 Logs
- Errori / warning / info
- Accesso completo ai log

### 6.8 Library Handling
- Supporto direttive custom (es. <?LIBRARY ...?>)

## 7. Architecture (High-Level)
- Desktop app locale
- Editor integrato
- Rendering PDF locale
- Accesso diretto filesystem

## 8. Technical Considerations
- Supporto strutture multi-file complesse
- Necessità di pre-processing per direttive custom
- Pipeline XML → XSLT → FO → PDF
- Mapping errori verso codice sorgente
- Nessuna dipendenza da backend esterni

## 9. Non-Goals
- No autenticazione
- No collaborazione multi-user
- No integrazione sistemi aziendali
- No preview HTML

## 10. Future Enhancements
- Outline struttura template
- Diff tra versioni
- Debugger XSLT
- Template library
- Auto-render

## 11. Success Metrics
- Riduzione tempo sviluppo
- Riduzione errori runtime
- Adozione interna
