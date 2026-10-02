import { useState } from "react";
import "./App.css";

const LEVELS = ["NONE", "CERTIFICATE", "DIPLOMA", "DEGREE", "POSTGRADUATE"];

const SAMPLE_CV =
  "Thabo Mokoena. Durban. BSc Computer Science. 1 year as a junior developer using Python and SQL. Open to relocating.";

// One row of the score breakdown: label, bar, and "26.7 / 40"
function Bar({ label, value, max }) {
  return (
    <div className="bar">
      <span>{label}</span>
      <progress value={value} max={max}></progress>
      <span>
        {value} / {max}
      </span>
    </div>
  );
}

function App() {
  // job form
  const [title, setTitle] = useState("Senior Python Developer");
  const [skills, setSkills] = useState("Python, Java, SQL");
  const [years, setYears] = useState(3);
  const [city, setCity] = useState("Johannesburg");
  const [level, setLevel] = useState("DEGREE");

  // CV text
  const [cv, setCv] = useState(SAMPLE_CV);

  // what comes back
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [fileStatus, setFileStatus] = useState("");

  async function handleSubmit(e) {
    e.preventDefault(); // stop the browser reloading the page
    setLoading(true);
    setError("");
    setResult(null);

    // The names here must match the Java records exactly
    const body = {
      cvText: cv,
      job: {
        title: title,
        requiredSkills: skills
          .split(",")
          .map((s) => s.trim())
          .filter((s) => s !== ""),
        minYearExperience: Number(years),
        city: city,
        minQualificationLevel: level,
      },
    };

    try {
      const res = await fetch("/api/evaluate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(
          data.error || "Request failed (status " + res.status + ")",
        );
      }
      setResult(await res.json());
    } catch (err) {
      setError(err.message);
    }
    setLoading(false);
  }

  async function handleFile(e) {
    const file = e.target.files[0];
    if (!file) return;
    setFileStatus("Reading " + file.name + "...");

    const formData = new FormData();
    formData.append("file", file);

    try {
      // No Content-Type header here. The browser sets it for file uploads.
      const res = await fetch("/api/extract-text", {
        method: "POST",
        body: formData,
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || "Could not read the file");
      setCv(data.text);
      setFileStatus(
        "Loaded " + file.name + ". Check the text below before evaluating.",
      );
    } catch (err) {
      setFileStatus("Error: " + err.message);
    }
  }

  return (
    <div className="page">
      <header>
        <h1>ShortList Copilot</h1>
        <p>
          Score a candidate against a job and see why. The recruiter makes the
          final decision.
        </p>
      </header>

      <div className="layout">
        {/* LEFT: input */}
        <form onSubmit={handleSubmit}>
          <h2>Job</h2>

          <label>Title</label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />

          <label>Required skills (separate with commas)</label>
          <input value={skills} onChange={(e) => setSkills(e.target.value)} />

          <label>Minimum years of experience</label>
          <input
            type="number"
            min="0"
            value={years}
            onChange={(e) => setYears(e.target.value)}
          />

          <label>City</label>
          <input value={city} onChange={(e) => setCity(e.target.value)} />

          <label>Minimum qualification</label>
          <select value={level} onChange={(e) => setLevel(e.target.value)}>
            {LEVELS.map((l) => (
              <option key={l}>{l}</option>
            ))}
          </select>

          <h2>CV</h2>
          <label>Upload a CV (PDF)</label>
          <input type="file" accept=".pdf" onChange={handleFile} />
          <p className="note">{fileStatus}</p>
          <label>Or paste the CV text</label>
          <textarea
            rows="8"
            value={cv}
            onChange={(e) => setCv(e.target.value)}
            required
          />
          <p className="note">Use sample or anonymised CVs only.</p>

          <button type="submit" disabled={loading}>
            {loading ? "Evaluating..." : "Evaluate"}
          </button>
        </form>

        {/* RIGHT: output */}
        <div className="output">
          <h2>Result</h2>

          {!result && !loading && !error && (
            <p className="note">
              Nothing yet. Fill in the form and press Evaluate.
            </p>
          )}
          {loading && <p>Evaluating...</p>}
          {error && <p className="error">Error: {error}</p>}

          {result && (
            <div>
              <p className="total">
                {result.score.total} <small>/ 100</small>
              </p>

              <Bar label="Skills" value={result.score.skills} max={40} />
              <Bar
                label="Experience"
                value={result.score.experience}
                max={25}
              />
              <Bar label="Location" value={result.score.location} max={20} />
              <Bar
                label="Qualifications"
                value={result.score.qualifications}
                max={15}
              />

              <h3>What ShortList read from the CV</h3>
              <table>
                <tbody>
                  <tr>
                    <td>Name</td>
                    <td>{result.candidate.name}</td>
                  </tr>
                  <tr>
                    <td>City</td>
                    <td>{result.candidate.city}</td>
                  </tr>
                  <tr>
                    <td>Years of experience</td>
                    <td>{result.candidate.yearsExperience}</td>
                  </tr>
                  <tr>
                    <td>Skills</td>
                    <td>{result.candidate.skills.join(", ")}</td>
                  </tr>
                  <tr>
                    <td>Willing to relocate</td>
                    <td>
                      {result.candidate.willingToRelocate ? "Yes" : "Not stated"}
                    </td>
                  </tr>
                  <tr>
                    <td>Qualification</td>
                    <td>{result.candidate.highestQualificationLevel}</td>
                  </tr>
                </tbody>
              </table>

              <h3>Summary</h3>
              <p>{result.explanation.summary}</p>

              <h3>Strengths</h3>
              <ul>
                {result.explanation.strengths.map((s, i) => (
                  <li key={i}>{s}</li>
                ))}
              </ul>

              <h3>Gaps</h3>
              <ul>
                {result.explanation.gaps.map((g, i) => (
                  <li key={i}>{g}</li>
                ))}
              </ul>

              <h3>Interview questions</h3>
              <ol>
                {result.explanation.interviewQuestions.map((q, i) => (
                  <li key={i}>{q}</li>
                ))}
              </ol>

              <h3>Draft message to the candidate</h3>
              <p className="message">{result.explanation.candidateMessage}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
