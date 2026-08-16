# UI / UX Design System & Guidelines — LocalPDF

## Design System Principles

1. **Rich Aesthetics & Precision**: A modern, sleek dark-and-light theme built on Material 3 design tokens, featuring subtle micro-animations, glassmorphism surface elevations, and crystal-clear document previews.
2. **Adaptive & Responsive Layouts**: Seamlessly adapting across Compact (Phones), Medium (Foldables), and Expanded (Tablets & Chromebooks) window size classes.
3. **Accessibility First**: WCAG 2.1 AA compliance, minimum 48dp touch targets, dynamic font scaling support up to 200%, high contrast ratios, and clear content descriptions on all interactive canvas elements.
4. **Immediate Tactile Feedback**: Visual edge detection overlays, haptic clicks on document capture/snapping, and smooth zoomable canvas transitions.

## Color Palette & Theme Tokens

- **Primary Accent**: Deep Indigo / Slate Blue (`#4F46E5` / Light, `#818CF8` / Dark) — conveys trust, enterprise precision, and security.
- **Secondary Accent**: Emerald Teal (`#059669` / Light, `#34D399` / Dark) — represents privacy safety, completed OCR, and verified documents.
- **Warning / Redaction**: Crimson Rose (`#E11D48` / Light, `#FB7185` / Dark) — used for detected sensitive data, redaction zones, and deletion actions.
- **Surface & Backgrounds**:
  - Light: Clean neutral tones (`#F8FAFC`, `#FFFFFF`, `#F1F5F9`)
  - Dark: Deep OLED slate (`#0B0F19`, `#111827`, `#1E293B`) to conserve battery during continuous camera/scanning workflows.

## Adaptive Window Size Class Strategy

```text
Compact (< 600dp)      → Bottom Navigation Bar, single-pane full-screen navigation.
Medium (600dp - 839dp) → Navigation Rail, foldable dual-screen posture (Camera viewfinder on one half, scan tray on the other).
Expanded (>= 840dp)    → Permanent Navigation Drawer, two-pane master-detail (Document Library on left, Interactive PDF Viewer + OCR Inspector on right).
```

## Key Interactive UI Experiences

### 1. Smart CameraX Scanner (`:feature:scanner`)
- **Live Polygon Overlay**: Draws real-time smooth Bezier-rounded quads around detected document boundaries in the camera feed using OpenCV contour analysis.
- **Auto-Capture Shutter Animation**: Haptic pulse and radial progress ring when the camera holds steady over high-confidence document edges.
- **Scan Tray Carousel**: Horizontal scrolling thumbnail bar of captured pages with quick swipe-to-delete and re-order animations.
- **Real-Time Quality Hints**: Floating pill alerts ("Hold steady", "Move closer", "More light needed", "Shadow detected").

### 2. Interactive OCR Correction Studio (`:feature:ocr-edit`)
- **Side-by-Side Synchronized View**:
  - Top/Left Pane: Zoomed high-resolution original image crop around the selected word/line.
  - Bottom/Right Pane: Editable text field with character confusion suggestions (`0` vs `O`, `1` vs `I`, `5` vs `S`).
- **Confidence Highlighting**: Low-confidence OCR words are tinted with an amber wavy underline; tapping a word automatically centers the viewport onto that specific bounding box in the original scan.

### 3. Redaction & Privacy Studio (`:feature:redaction`)
- **Detected Entity Chips**: Quick toggles to redact all instances of a category (e.g. "[x] Redact 3 Phone Numbers", "[x] Redact National ID", "[x] Redact Bank Account").
- **Manual Box & Freehand Brush Tool**: Interactive gesture overlay allowing users to drag rectangular redaction boxes or freehand brush over sensitive text and signatures.
- **Safe Copy Preview**: Live preview showing the flattened output with watermark overlays before saving or exporting.

### 4. Search & Filter Bar (`:feature:search`)
- **Unified Instant Search**: Search input with instant FTS5 highlight matching, filter chips for document types (Invoices, Receipts, Contracts, IDs, Medical), date range pickers, and amount range sliders.
- **Snippet Result Cards**: Document cards displaying highlighted search snippets with page numbers, document type badges, and total amount tags.

## Standard Screen States

Every significant screen must handle and render:
- **Initial / Idle**: Empty search or fresh scan session.
- **Loading / Processing**: Circular/linear indicators with progress percentage during batch OCR or PDF compression.
- **Content**: Loaded document list, viewer canvas, or settings dashboard.
- **Empty**: Friendly empty illustration with clear call-to-action (e.g. "No documents scanned yet — [Scan Document]").
- **Error**: Retryable error cards with actionable error messages.
- **Permission Denied**: Explanatory permission rationale for Camera or Biometric access with direct link to system settings.
