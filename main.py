from __future__ import annotations

from flask import Flask, jsonify, request

app = Flask(__name__)

# In-memory storage for fitness tracker records.
# Each record is appended when uploaded, so list order represents upload order.
fitness_records: list[dict[str, int | str]] = []
next_record_id: int = 1


@app.post("/api/tracker")
def upload_tracker_data() -> tuple[object, int]:
    """Accept a fitness tracking record and store it in memory."""

    payload = request.get_json()

    global next_record_id

    record = {
        "id": next_record_id,
        "username": payload["username"],
        "activity": payload["activity"],
        "duration": payload["duration"],
        "calories": payload["calories"],
    }

    fitness_records.append(record)

    next_record_id += 1

    return jsonify(record), 201


@app.get("/api/tracker")
def list_tracker_data():
    """Return the stored tracker data, most recent uploads first."""

    # Return newest uploads first by reversing the upload order list.
    return jsonify(list(reversed(fitness_records)))


if __name__ == "__main__":
    app.run()
