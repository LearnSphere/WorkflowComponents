# CreateSession Component

**Component Type:** Transform  
**Language:** Python

## Description

The `CreateSession` component processes a merged DataShop-format log file and generates **session-level summaries** by identifying interaction sessions and bundling related events based on timing and similarity.

---

## What It Does

- Accepts input from a **Join** component (merged Interactions + Exercises in DataShop format).
- Splits activity into **sessions** using a 10-minute inactivity threshold.
- Within each session, events are **bundled** into groups based on:
  - Same event type (e.g., same _Step Name_)
  - Same Exercise Type (e.g., PE vs slideshow)
  - Time proximity (within 60 seconds)

---

## Input

**File Type:** Tab-delimited `.txt` file (DataShop format)  
**Expected Columns:**

- `Anon Student Id`
- `Time` (timestamp)
- `Step Name`
- `Action`
- `Level` (Book/module)
- `CF (Exercise Type)`
- `CF (Short Name)`

This file is typically the output of a **Join** operation.

---

## Output

**File:** `sessions_custom.txt`  
**Format:** Tab-delimited summary file

| Column            | Description                              |
| ----------------- | ---------------------------------------- |
| session           | Session number for the student           |
| Anon Student Id   | Unique student identifier                |
| Level (Book)      | Book/module label                        |
| Event name        | Normalized name of event type            |
| Event Description | Description of the interaction           |
| Start time        | Start timestamp of the event bundle      |
| End time          | End timestamp of the event bundle        |
| Action Time       | Duration of the event group (in seconds) |
| CF (Short Name)   | Exercise short name (content family)     |
| Number of events  | Count of bundled events in this group    |

---

## Session Logic

### A new **session** starts when:

- The gap between consecutive events > **10 minutes**

### A new **event group** starts within a session if:

- Time gap from previous event > **60 seconds**
- Event type changes (different _Step Name_ or _Action_)
- Exercise Type switches (e.g., PE → slideshow)

---

## Error Handling

- Malformed or incomplete lines are **skipped**
- Rows with missing timestamps are **dropped**
- If no input is provided in test mode, the component will try:
  1. `test/Import-1-x995490/output/joinedResult.txt`
  2. `test/data/joinedResult.txt`
  3. `joinedResult.txt` in the working directory

---

## Notes

- Designed to work after merging Interactions and Exercises using the **Join** component.
- Ideal for producing session-level features for educational log analysis.
