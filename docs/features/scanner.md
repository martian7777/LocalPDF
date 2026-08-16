# Feature Specification — Smart Document Scanner

## Purpose

Provide a camera-based mobile scanning experience powered by CameraX and OpenCV with real-time edge detection, auto-shutter capture, perspective transformation, shadow removal, and contrast enhancement.

## User Story

As a user with a physical document, bill, or receipt,
I want to point my camera at the page and have it automatically detected, cropped, enhanced, and saved,
so that I get a crisp, flatbed-grade digital scan without manual alignment.

## Scope

### In Scope
- CameraX live preview with dynamic aspect ratios.
- Real-time OpenCV contour analysis and quadrilateral edge detection.
- Interactive manual corner adjustment overlay.
- Automatic deskewing, perspective correction, and page dewarping.
- Filter modes: Color Enhanced, Grayscale, Pure Black & White (thresholded), and Original.
- Multi-page / batch scanning tray with thumbnail reordering.
- Blur detection and low-light quality alerts.

### Out of Scope (MVP)
- Video scanning mode.

## User Flow

```text
Tap "Scan" FAB in Library
→ CameraX Viewfinder Opens
→ Live Edge Detection Quad Overlaid on Document
→ Camera Holds Steady → Auto-Shutter Haptic Capture
→ Scanned Page Added to Bottom Tray
→ Tap "Review / Filters" or "Capture Next Page"
→ Tap "Done" → Triggers OCR & Classification Pipeline
```

## UI States

- **Previewing / Searching for Edges**: Camera feed active, scanning animation.
- **Edge Locked**: Green corner overlay with steady indicator.
- **Capturing / Processing**: Brief shutter blink animation and haptic feedback.
- **Review / Crop Adjustment**: High-resolution image canvas with draggable 4-point magnetic corner handles.
- **Permission Denied**: Explanatory permission rationale with button to launch system settings.
