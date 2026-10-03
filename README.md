# Shortlist Copilot

An AI agent that helps recruiters screen candidates faster. A recruiter uploads a CV (PDF) or pastes its text, enters the job requirements, and gets back:

- a **match score out of 100** with a breakdown (skills, experience, location, qualifications)
- a **plain-language explanation**: summary, strengths and gaps
- **5 interview questions** aimed at the gaps
- a **draft message** to the candidate

A recruiter always makes the final decision. The agent never rejects anyone.

Built as an interview task for Ejoobi.

## Live demo

**https://ejoobi-shortlist-copilot-lethabo-dpgqcreufjcwafcg.southafricanorth-01.azurewebsites.net/**

No login is needed. Use the fictional CVs described in [Test data](#test-data). Please do not upload real CVs to the demo.

The first request after a period of inactivity can take a little longer while the app wakes up.

## The problem

Hiring teams spend a long time reading CVs. A bare match percentage (for example "88%") does not tell a recruiter why someone scored that, what is missing, or what to ask them, so they still open every CV to trust the number.

Shortlist Copilot turns a CV and a job into an explained, auditable result in a few seconds. See [docs/solution.md](docs/solution.md) for the pain point, how the agent solves it, how it scales globally, and how it can be improved.

## How it works

```
PDF or text  ->  Extract facts (AI)  ->  Score (Java)  ->  Explain (AI)  ->  Result
```

1. **Extract (AI).** The model reads the CV and returns structured facts: name, skills, years of experience, city, willingness to relocate, highest qualification.
2. **Score (Java).** Plain Java code calculates the score from those facts. The AI does **not** do the maths, so the same CV always gets the same score and every number can be explained.
3. **Explain (AI).** The model receives the job, the facts and the already-calculated score, and writes the summary, strengths, gaps, questions and draft message.

### Scoring rules

| Category       | Max | Rule                                                                                 |
| -------------- | --- | ------------------------------------------------------------------------------------ |
| Skills         | 40  | Share of the job's required skills the candidate has (exact match, case-insensitive) |
| Experience     | 25  | Candidate years divided by required years, capped at 100%                            |
| Location       | 20  | Same city or metro area = 100%. Stated willingness to relocate = 60%. Otherwise 0    |
| Qualifications | 15  | Meets or exceeds the required level = 100%, otherwise partial credit by level        |

Qualification levels, in order: `NONE`, `CERTIFICATE`, `DIPLOMA`, `DEGREE`, `POSTGRADUATE`.

## Architecture

![Architecture](docs/docs/architecture.drawio.png)

Editable source: `docs/architecture.drawio`.

| Part        | Technology                                         |
| ----------- | -------------------------------------------------- |
| Frontend    | React (Vite)                                       |
| Backend     | Java 21, Spring Boot 4                             |
| PDF reading | Apache PDFBox                                      |
| AI          | Azure OpenAI (`gpt-4.1-mini`) in Microsoft Foundry |
| Hosting     | Azure App Service (Linux, Java SE)                 |

The React app is built into static files and served by the Spring Boot app, so the whole product ships as **one jar**. The browser only talks to the Java backend. The Azure OpenAI key lives only on the server.

## Project structure

```
backend/    Spring Boot app
  src/main/java/com/ejoobi/shortlist_copilot/
    controller/   REST endpoints and error handling
    service/      extraction, scoring, explanation, PDF reading, Azure client
    model/        request, response and data records
frontend/   React app (Vite)
build.sh    builds the frontend, copies it into the backend, packages one jar
docs/       architecture diagram and solution write-up
```

## API

### `POST /api/extract-text`

Reads a PDF and returns its text. The file is processed in memory and is not stored.

- Body: `multipart/form-data` with a `file` field (PDF, max 5 MB)
- Response: `{ "text": "..." }`
- Errors: `400` with a readable message (not a PDF, empty file, or a scanned PDF with no text)

### `POST /api/evaluate`

Runs the full pipeline.

Request:

```json
{
  "cvText": "Thabo Mokoena. Durban. BSc Computer Science. 1 year ...",
  "job": {
    "title": "Senior Python Developer",
    "requiredSkills": ["Python", "Java", "SQL"],
    "minYearExperience": 3,
    "city": "Johannesburg",
    "minQualificationLevel": "DEGREE"
  }
}
```

Response:

```json
{
  "candidate": {
    "name": "...",
    "skills": ["..."],
    "yearsExperience": 1.0,
    "city": "...",
    "willingToRelocate": true,
    "highestQualificationLevel": "DEGREE"
  },
  "score": {
    "skills": 26.7,
    "experience": 8.3,
    "location": 12.0,
    "qualifications": 15.0,
    "total": 62.0
  },
  "explanation": {
    "summary": "...",
    "strengths": ["..."],
    "gaps": ["..."],
    "interviewQuestions": ["... (5 items)"],
    "candidateMessage": "..."
  }
}
```

If the AI service fails, the API returns `502` with a generic message and no CV content.

## Run it locally

### Prerequisites

- Java 21
- Maven 3.8+
- Node.js 20.19 or newer (24 recommended)
- An Azure OpenAI deployment (see [Configuration](#configuration))

### Configure

```bash
cp .env.example .env
```

Edit `.env` and fill in your own values. The `.env` file is ignored by Git and must never be committed.

### Build and run

```bash
set -a; source .env; set +a      # load the settings into this terminal
./build.sh                        # frontend build + backend jar
java -jar backend/target/shortlist-copilot-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080.

### Development mode (two terminals)

```bash
# terminal 1: backend
set -a; source .env; set +a
cd backend && mvn spring-boot:run

# terminal 2: frontend (proxies /api to port 8080)
cd frontend && npm install && npm run dev
```

Open http://localhost:5173.

### Tests

```bash
cd backend && mvn test
```

## Configuration

| Variable                  | Meaning                                                                                                              |
| ------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `AZURE_OPENAI_ENDPOINT`   | Base address of the Azure OpenAI resource, for example `https://<resource-name>.openai.azure.com` (no trailing path) |
| `AZURE_OPENAI_API_KEY`    | Key for that resource. Keep it secret                                                                                |
| `AZURE_OPENAI_DEPLOYMENT` | Name of the model deployment (default `gpt-4.1-mini`)                                                                |

Create the model deployment in Microsoft Foundry (Discover, then Models, then `gpt-4.1-mini`, deployment type Standard) and copy the endpoint and key from the Foundry home page.

## Deploy to Azure

Resources used: one resource group, one App Service plan (Linux, Basic B1) and one Web App (Java 21, Java SE).

```bash
az login

# 1. store the settings in the App Service (the key never goes into Git or the jar)
set -a; source .env; set +a
az webapp config appsettings set \
  --resource-group ejoobi-agent-rg \
  --name ejoobi-shortlist-copilot-lethabo \
  --settings AZURE_OPENAI_ENDPOINT="$AZURE_OPENAI_ENDPOINT" \
             AZURE_OPENAI_API_KEY="$AZURE_OPENAI_API_KEY" \
             AZURE_OPENAI_DEPLOYMENT="$AZURE_OPENAI_DEPLOYMENT" \
  -o none

# 2. build one jar that contains the React app
./build.sh
unzip -l backend/target/shortlist-copilot-0.0.1-SNAPSHOT.jar | grep static/index.html   # must print a line

# 3. deploy
az webapp deploy \
  --resource-group ejoobi-agent-rg \
  --name ejoobi-shortlist-copilot-lethabo \
  --src-path backend/target/shortlist-copilot-0.0.1-SNAPSHOT.jar \
  --type jar
```

Deployment takes about three minutes. To watch the live log: `az webapp log tail --resource-group ejoobi-agent-rg --name ejoobi-shortlist-copilot-lethabo`.

To stop all charges after the assessment, delete the resource group.

## Test data

Four fictional candidates were used to check the scoring. Against the default job (Senior Python Developer, skills Python, Java, SQL, 3 years, Johannesburg, DEGREE):

| Candidate      | Profile                                | Expected score |
| -------------- | -------------------------------------- | -------------- |
| Nomsa Dlamini  | Strong match, Sandton                  | about 100      |
| Pieter van Wyk | No Java, Cape Town, open to relocating | about 78.7     |
| Sipho Khumalo  | IT support, Durban, certificate        | about 26.6     |
| Ayesha Naidoo  | All skills, 2 years, diploma, Midrand  | about 86.7     |

The PDFs are not stored in this repository (`*.pdf` is ignored) because CVs are personal data. Create fictional CVs of your own to test.

## Responsible AI and privacy

- **Human in the loop.** The output is decision support. Nothing is sent, rejected or ranked automatically.
- **Explainable score.** Scoring is deterministic Java code, and the response shows every component.
- **Fairness.** The prompts forbid inferring age, gender, race or other personal traits, and the score uses only job-relevant factors.
- **Not stated is not refused.** If a CV does not mention relocation, the app shows "Not stated" and the AI is told not to imply the candidate refused.
- **Data handling.** Uploaded PDFs are read in memory and not saved. CV text is sent to Azure OpenAI to be processed. Error messages never include CV content.
- **Secrets.** Keys are kept in environment variables and App Service settings, never in the repository.

## Known limitations

- Skill matching is by exact word, so "PostgreSQL" does not count as "SQL".
- The metro-area list used for location matching is small and hand-written.
- Scanned (image-only) PDFs are not supported, because no text can be extracted.
- A strong skills match can outweigh a large experience gap. There is no hard "minimum years" rule yet.
- No login or rate limiting on the demo.
- English-language CVs only.

Planned improvements are described in [docs/solution.md](docs/solution.md).
