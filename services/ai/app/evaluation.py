import json
from . import config
from .schemas import Employee, StaffingRequest
from .engine import MODE_LABELS, recommend, evidence_supported


def evaluate(mode: str | None = None, k: int = 3) -> dict:
    mode = mode or config.MODE
    employees = [Employee.model_validate(e) for e in json.loads((config.DATA_DIR / "employees.json").read_text())]
    # Gold data is a separate evaluation-only file and is never sent to the
    # recommender, model or embedding cache.
    gold = json.loads((config.DATA_DIR / "evaluation_requests.json").read_text())
    cases, all_supported, all_claims = [], 0, 0
    for case in gold:
        request = StaffingRequest.model_validate(case["request"])
        ranked = recommend(request, employees, mode)[:k]
        actual = [r["employee"]["id"] for r in ranked]
        expected = set(case["expectedIds"])
        recalled = len(expected.intersection(actual))
        matched = [(e, r["employee"]) for r in ranked for e in r["evidence"] if e["status"] == "MATCHED"]
        # Independently authored evidence IDs are evaluation-only, not generated
        # by the matching function under test. LLM explanation prose still needs
        # a separate human review; this metric checks source-grounded claims.
        expected_evidence = case.get("expectedEvidence", {})
        supported = sum(evidence_supported(e, employee) and e["experienceId"] in
                        expected_evidence.get(e["requirementId"], {}).get(employee["id"], [])
                        for e, employee in matched)
        all_supported += supported
        all_claims += len(matched)
        cases.append({"id": case["id"], "title": request.title, "expectedIds": case["expectedIds"],
                      "actualIds": actual, "hit": recalled > 0, "recall": recalled / len(expected) if expected else 0,
                      "evidenceAccuracy": supported / len(matched) if matched else 0})
    count = len(cases)
    return {"mode": MODE_LABELS[mode], "k": k, "requestCount": count,
            "hitAtK": sum(c["hit"] for c in cases) / count if count else 0,
            "recallAtK": sum(c["recall"] for c in cases) / count if count else 0,
            "evidenceAccuracy": all_supported / all_claims if all_claims else 0,
            "cases": cases}
