# UI / UX & Design System

## Design Principles

Prioritize:

- clear hierarchy
- predictable navigation
- low cognitive load
- responsive/adaptive layouts
- accessibility
- fast perceived performance
- consistent components
- useful feedback

## App-Specific Design Direction

Visual style:

`[MINIMAL / ENTERPRISE / PLAYFUL / DARK / OTHER]`

### Colors

- Primary: `[COLOR]`
- Secondary: `[COLOR]`
- Background: `[COLOR]`
- Surface: `[COLOR]`
- Success: `[COLOR]`
- Warning: `[COLOR]`
- Error: `[COLOR]`

### Typography

`[FONT / MATERIAL TYPOGRAPHY DECISIONS]`

### Shape / Spacing

`[TOKEN NOTES]`

## Design Tokens

Prefer shared semantic tokens for:

- color
- typography
- spacing
- shapes
- elevation
- icons
- motion

Avoid arbitrary hardcoded values across screens.

## Reusable Components

Create/reuse as needed:

```text
AppButton
AppTextField
AppCard
AppTopBar
AppDialog
AppBottomSheet
AppSnackbar
AppListItem
AppLoading
AppEmptyState
AppErrorState
```

Search before creating new components.

## Adaptive Layout

Design by available space rather than one device.

Account for:

- compact
- medium
- expanded
- portrait
- landscape
- foldables
- tablets

Possible adaptation:

```text
Compact → bottom navigation + single pane
Medium  → navigation rail + wider content
Expanded → rail/drawer + list-detail/two-pane
```

Do not simply stretch a phone screen across tablets.

## Screen States

Consider applicable:

- initial
- loading
- content
- empty
- error
- refreshing
- offline/degraded
- permission denied
- session expired

## Accessibility

Check:

- TalkBack semantics
- content descriptions
- touch target size
- contrast
- font scaling
- focus order
- keyboard input where applicable
- reduced motion
- meaningful error text

Do not disable font scaling to preserve layout.
