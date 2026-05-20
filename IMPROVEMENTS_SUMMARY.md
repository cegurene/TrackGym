# UI Improvements Summary - Order Buttons

## Changes Made

### 1. RutinasScreen - Added Sort Button
**File:** `RutinasScreen.kt`
- Added a new "Ordenar" (Sort) button next to the filter button
- Opens a `ModalBottomSheet` with two ordering options:
  - **A-Z**: Sort routines alphabetically (ascending/descending)
  - **Veces realizada** (Times Done): Sort by number of times the routine has been executed
- Each option shows an arrow indicator (↑ for ascending, ↓ for descending)
- "Limpiar" (Clear) button resets to default alphabetical order

### 2. RutinaDetailScreen - Added Exercise Order
**File:** `RutinaDetailScreen.kt`
- Added an order button (icon) next to the "💪 Ejercicios" (Exercises) section title
- Opens a `ModalBottomSheet` with two ordering options for exercises within the routine:
  - **A-Z**: Sort exercises alphabetically (ascending/descending)
  - **Veces realizado** (Times Done): Sort by times each exercise has been performed (ascending/descending)
- Each option shows an arrow indicator (↑ for ascending, ↓ for descending)
- Exercises are rendered in the selected order
- "Limpiar" (Clear) button resets to default alphabetical order

### 3. RutinaViewModel - Added Supporting Enums
**File:** `RutinaViewModel.kt`
- Added new enum: `RutinaDetailExerciseOrder` with values:
  - `ALPHABETIC_ASC`
  - `ALPHABETIC_DESC`
  - `TIMES_DONE_DESC`
  - `TIMES_DONE_ASC`
- Existing enum: `RutinaOrder` provides sorting for the main routines list

### 4. EjercicioViewModel - Added Supporting Enums
**File:** `EjercicioViewModel.kt`
- Added new enum: `DetailExerciseOrder` with values (for future use):
  - `ALPHABETIC_ASC`
  - `ALPHABETIC_DESC`
  - `MUSCLE_ASC`
  - `MUSCLE_DESC`
- Existing enum: `EjercicioOrder` already supported A-Z and Muscle sorting

## Features

✅ **Default Order**: All screens default to A-Z (alphabetical ascending)
✅ **Toggle Direction**: Click the same order option again to reverse direction (A-Z ↔ Z-A)
✅ **Visual Indicators**: Arrow symbols (↑↓) show current sort direction next to selected option
✅ **Filter Sheet UI**: Integrated with existing filter system - shows order options together with filters
✅ **Persistent Selection**: Order selection is maintained while browsing

## UI Components Used
- `FilterChip`: For order option selection with trailing icon
- `ModalBottomSheet`: For displaying order options
- `Icons.Default.ArrowUpward/ArrowDownward`: For visual indicators
- `Icon`: Clickable button with arrow icon to open sort sheet

## Implementation Details

### Sorting Logic
Each screen implements sorting through remember blocks:
- **RutinasScreen**: Uses `ViewModel.order` state to maintain selection
- **RutinaDetailScreen**: Uses local `ejercicioOrder` state + `remember` block to calculate sorted list
  - "Times done" counts occurrences of each exercise in the routine
  - Ordering works with exercise movement buttons (up/down arrows)

### Fix Applied
Initial version used wrong array indices (ejercicios instead of ejerciciosOrdenados) in move buttons. This was corrected to maintain consistency with displayed order.

## Localization
- All UI text uses Spanish labels (matching existing app language)
- "A-Z", "Veces realizada", "Veces realizado", "Ordenar", "Limpiar", "Aplicar"

