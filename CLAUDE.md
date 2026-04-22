# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language Convention

- **Chat / conversation**: always in Italian
- **Everything else** (code, comments, commit messages, documentation, PR descriptions): always in English

## Project Overview

XSLEditor is a local desktop developer tool for editing multi-file XSLT/XSL-FO templates, generating PDFs on demand, and doing advanced debugging. It is strictly for internal developers; no auth, no multi-user, no external backend dependencies.

Core pipeline: **XML → XSLT → XSL-FO → PDF**

## Key Product Goals (from PRD)

- Edit-to-preview cycle < 5 seconds
- No context switching — everything in one tool
- Real-time XML validation and immediate error feedback with navigation from error to source line
- Multi-file project support with entrypoint XSLT definition and dependency tracking
- Support for custom preprocessing directives (e.g. `<?LIBRARY ...?>`) before the XSLT pipeline

## MVP Scope

- Project-based workflow: import from filesystem, define XSLT entrypoint, manage file dependencies
- Integrated editor: syntax highlighting for XML/XSLT, autocomplete XSL/XSL-FO, real-time XML validation, multi-file search, variable highlight, go-to-definition for templates
- Split view: editor + PDF preview side by side, manual render trigger
- Multiple XML input files with quick switching
- Error handling across all pipeline stages (XML syntax, XSLT compile, XSLT runtime, FO rendering) with user-friendly messages
- Log panel: errors / warnings / info

## Non-Goals

- No authentication
- No multi-user collaboration
- No HTML preview
- No integration with external company systems
