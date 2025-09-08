# EngagementMetrics

**Component Type:** Analysis  
**Language:** Python 3  
**Version:** 1.0  
**Author:** Jharana Sapkota  
**Contact:** jhara@example.com

---

## Description

The **EngagementMetrics** component analyzes session-level log data to compute student engagement metrics such as:

- Time spent per engagement state
- Transitions between different activity states

It is designed to follow the `CreateSession` component in a LearnSphere workflow.

---

## Input

**Expected Input File (tab-delimited):**

- `sessions_custom.txt` from the `CreateSession` component
- Must contain at least the following columns:
  - `Anon Student Id`
  - `Event name`
  - `Event Description`
  - `Action Time`

---

## Method

This component:

1. Classifies each row into one of four engagement states:
   - `Reading`
   - `Visualization`
   - `Proficiency_Exercise`
   - `Multiple_choice_Exercise`
2. Computes:
   - Time spent in each state
   - Number of transitions between states (e.g., `Reading → Visualization`)

---

## Output

**File Produced:** `engagement_metrics.txt`  
**Format:** Tab-delimited text file

**Columns include:**

- `Anon Student Id`
- Transition counts like `Reading-Visualization`, `Proficiency_Exercise-Reading`, etc.
- Time spent per state: `Reading_Time`, `Visualization_Time`, etc.

---

## Technical Requirements

- Python 3.6+
- `pandas` (≥0.23)
- No use of `on_bad_lines` for compatibility with older environments
- Accepts and ignores `-userId` argument passed by LearnSphere

---

## Workflow Integration

Place this component **after** the `CreateSession` component in your workflow:
