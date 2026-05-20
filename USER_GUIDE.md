# How to Use the New Sort Features

## For Users

### Sorting Routines (RutinasScreen)

1. Open the "Rutinas" (Routines) screen
2. Look for the **"🔃 Ordenar"** button next to **"🧩 Filtrar"**
3. Click the **"🔃 Ordenar"** button
4. A bottom sheet will appear with sorting options:
   - **A-Z**: Sort alphabetically by routine name
   - **Veces realizada** (Times Done): Sort by how many times the routine was completed
5. Click on one of the options to select it
   - If you see **↑** (up arrow): sorting ascending
   - If you see **↓** (down arrow): sorting descending
6. Click the same option again to reverse the sort direction
7. Click **"Aplicar"** (Apply) to close the sheet

### Sorting Exercises in a Routine (RutinaDetailScreen)

1. Open a specific routine detail
2. Look for the **↑** (up arrow) button next to **"💪 Ejercicios"** (Exercises title)
3. Tap the **↑** button
4. A bottom sheet will appear with sorting options:
   - **A-Z**: Sort exercise names alphabetically
   - **Veces realizado** (Times Done): Sort by frequency within the routine
5. Select an option to sort:
   - **↑** means ascending (A→Z, low→high)
   - **↓** means descending (Z→A, high→low)
6. Click the same option again to reverse the sort direction
7. Click **"Aplicar"** (Apply) to close

## Technical Details for Developers

### Default Sorting Behavior
- **Initial State**: All screens start with A-Z (alphabetical ascending)
- **State Reset**: Sorts reset to A-Z when navigating between screens
- **Clear Button**: "Limpiar" button in sort sheet resets to A-Z

### Sort Options

#### RutinasScreen (Main Routines List)
- `ALPHABETIC_ASC`: A → Z
- `ALPHABETIC_DESC`: Z → A  
- `TIMES_DONE_DESC`: Most completed → Least completed
- `TIMES_DONE_ASC`: Least completed → Most completed

#### RutinaDetailScreen (Exercises in a Routine)
- `ALPHABETIC_ASC`: A → Z
- `ALPHABETIC_DESC`: Z → A
- `TIMES_DONE_DESC`: Most frequent → Least frequent
- `TIMES_DONE_ASC`: Least frequent → Most frequent

Note: "Times Done" in RutinaDetailScreen refers to how many times the exercise appears in the routine (typically 0-1 times per routine), not how many times it was performed in workouts.

### Implementation Architecture

```
RutinasScreen
├── State: viewModel.order (StateFlow from ViewModel)
├── UI: ModalBottomSheet with FilterChips
└── Updates: viewModel.onOrderChange()

RutinaDetailScreen  
├── State: ejercicioOrder (Local remember state)
├── Computed: ejerciciosOrdenados (remember block)
├── UI: ModalBottomSheet with FilterChips
└── Data: ejercicios → filtered/sorted → ejerciciosOrdenados
```

### Code References

**RutinasScreen**: Lines 101-220 contain the sort button and BottomSheet
**RutinaDetailScreen**: Lines 98-120 (state setup), Lines 225-236 (button), Lines 624-712 (BottomSheet)
**ViewModels**: Both `RutinaViewModel` and `EjercicioViewModel` contain the sort order enums

---

**Ready to Use!** ✅ No additional configuration needed.

