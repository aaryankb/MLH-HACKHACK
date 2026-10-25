# Glossary-Lookup Agent Skill

A standardized Agent Skill adhering to the **Agent Skills open standard** designed for real-time, context-aware machine translation in field operations (e.g., humanitarian relief, remote medical aid, and emergency legal support).

## Specification Overview

* **Skill Name:** `glossary-lookup`
* **Version:** `1.0.0`
* **Category:** Translation & Domain Knowledge Augmentation
* **Standard:** Agent Skills Open Standard (`skill.json` metadata + parameter schemas)

## Problem Solved

Standard open-weight or cloud translation models often produce literal, inaccurate, or ambiguous translations for critical domain terms (e.g., mistranslating "anaphylaxis" as general itching, or "potable water" as just water). 

This skill acts as a pre-inference knowledge retrieval layer. When a context is set (or detected in the input), the model harness executes this skill to retrieve canonical, verified local-language terminology and injects it into the prompt constraints.

## Supported Domains & Datasets

1. **Medical & Healthcare (`medical.json`)**:
   - Symptoms, conditions, emergency interventions (e.g., Anaphylaxis, Epinephrine, Intravenous, Sepsis, Hypoglycemia).
   - Languages: English, Kannada, Hindi, Telugu, Tamil, Spanish.
2. **Disaster Relief & Humanitarian Aid (`disaster_relief.json`)**:
   - Rations, clean water, shelter, evacuation, emergency signals (e.g., Potable water, Evacuation route, First aid post, Flash flood).
   - Languages: English, Kannada, Hindi, Telugu, Tamil, Spanish.
3. **Legal & Government Assistance (`legal.json`)**:
   - Rights, identification, affidavit, custody, refugee status (e.g., Affidavit, Bail, Temporary shelter permit, Legal guardian).
   - Languages: English, Kannada, Hindi, Telugu, Tamil, Spanish.

## Integration Contract

### Input Contract
```json
{
  "context": "Emergency triage at rural clinic",
  "source_text": "The patient has severe anaphylaxis and needs an epinephrine injection immediately.",
  "source_language": "en",
  "target_language": "kn"
}
```

### Output Contract
```json
{
  "matched_domain": "medical",
  "glossary_terms": {
    "anaphylaxis": "ತೀವ್ರ ಅಲರ್ಜಿ ಪ್ರತಿಕ್ರಿಯೆ (ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್)",
    "epinephrine": "ಎಪಿನೆಫ್ರಿನ್ ಚುಚ್ಚುಮದ್ದು",
    "patient": "ರೋಗಿ"
  },
  "system_instruction_prompt": "CRITICAL DOMAIN TERMINOLOGY (Glossary-Lookup Agent Skill):\nUse the following exact translations for domain concepts:\n- anaphylaxis -> ತೀವ್ರ ಅಲರ್ಜಿ ಪ್ರತಿಕ್ರಿಯೆ (ಅನಾಫಿಲ್ಯಾಕ್ಸಿಸ್)\n- epinephrine -> ಎಪಿನೆಫ್ರಿನ್ ಚುಚ್ಚುಮದ್ದು\n- patient -> ರೋಗಿ"
}
```

