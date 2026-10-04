# ShortList Copilot: Solution write-up

This document answers the four questions in the task brief:

1. [The pain point](#1-the-pain-point)
2. [How the agent solves it](#2-how-the-agent-solves-it)
3. [How it scales to customers worldwide](#3-how-it-scales-to-customers-worldwide)
4. [How it can be improved after this first version](#4-how-it-can-be-improved)

The system design is in [architecture.drawio.png](architecture.drawio.png). Setup and deployment steps are in the [README](../README.md).

## 1. The pain point

**Who has it:** recruiters and hiring managers who use Ejoobi to find candidates.

Ejoobi ranks candidates with a match score. A score such as "88%" is useful for sorting, but it does not answer the questions a recruiter actually has:

- **Why** did this person score 88%?
- What is **missing** compared with the job?
- What should I **ask** them in the interview?
- What do I **tell** the candidate next?

Without those answers the recruiter has to open each CV and read it to trust the number, then write interview questions and candidate messages by hand. The score saves time on sorting, but the reading and writing work is still manual. CVs also arrive as PDFs in many different layouts, which adds a copy-and-paste step.

**The problem to solve:** turn a CV and a job into a result the recruiter can trust and act on within seconds, without removing the recruiter from the decision.

## 2. How the agent solves it

A recruiter uploads a CV (PDF) or pastes its text, enters the job requirements, and presses Evaluate. The agent returns a score with its breakdown, a plain-language explanation, five interview questions, and a draft message to the candidate.

### The pipeline

PDF or text, then extract facts (AI), then score (Java), then explain (AI).

1. **Extract (AI).** The model reads the CV and returns structured facts: name, skills, years of experience, city, willingness to relocate and highest qualification.
2. **Score (Java).** Ordinary Java code calculates the score from those facts, using the same four categories as the Ejoobi match score (skills 40, experience 25, location 20, qualifications 15).
3. **Explain (AI).** The model receives the job, the facts and the finished score, and writes the summary, strengths, gaps, questions and draft message. It is instructed not to change the score.

### Why the design is split this way

The most important decision was **not letting the AI do the maths**. In an early test in the Foundry playground, the model counted the same weakness in two categories and invented its own numbers. A language model can give slightly different answers to the same question, which is a problem when a number is used to compare people. So the AI is used for what it is good at (reading messy CVs and writing explanations) and plain code is used for what must be exact and repeatable.

This gives three benefits:

- **Consistent.** The same CV and job always give the same score.
- **Explainable.** Every number can be traced to a rule, and the screen shows each category separately.
- **Auditable.** The extracted facts are shown next to the score, so a recruiter can spot a reading mistake. This is how a qualification bug was found during development.

### Keeping a human in charge

- The agent never rejects, ranks or contacts anyone. It produces decision support.
- The candidate message is a draft for the recruiter to edit and send.
- The AI prompts forbid inferring age, gender, race or other personal traits. The score uses only job-relevant facts.
- If a CV does not mention relocation, the screen shows "Not stated".

### Evidence it works

Four fictional candidates were scored against one job (Senior Python Developer, skills Python, Java and SQL, 3 years, Johannesburg, degree). The expected scores were calculated by hand before running them, and all four results matched.

| Candidate | Profile | Score |
|---|---|---|
| Nomsa Dlamini | All skills, 6 years, degree, Sandton | 100 |
| Ayesha Naidoo | All skills, 2 years, diploma, Midrand | 86.7 |
| Pieter van Wyk | No Java, 4 years, degree, Cape Town, open to relocating | 78.7 |
| Sipho Khumalo | IT support, 1 year, certificate, Durban | 26.6 |

The scoring logic also has an automated unit test, and the app runs live on Azure App Service so it can be tested from a browser on a laptop or a phone.

### Data and privacy

- Uploaded PDFs are read in memory and are not saved.
- CV text is sent to Azure OpenAI to be processed. Error messages never include CV content.
- The API key is held in Azure App Service settings and never in the repository or the jar.
- The repository does not contain CVs, and the demo uses fictional candidates only.

## 3. How it can scale to customers worldwide

The current app is deliberately small: one stateless service and no database. That makes it easy to grow in the following ways.

### More users and more CVs

- **Scale out.** The app keeps no state between requests, so more instances can run behind App Service with no code change.
- **Bulk processing.** Screening a whole pile of CVs should not wait on a browser. Uploads would go onto a queue (for example Azure Service Bus) and background workers would process them and store results for the recruiter to open.
- **Cost control.** A small, cheap model is already used. Limits on tokens and requests per client, and caching of repeated work, keep cost predictable as volume grows.

### Different clients, countries and jobs

- **Configurable scoring.** Today the weights (40, 25, 20, 15) and rules sit in the code. For clients with different needs they should move into configuration per client and per role, so one customer can weight experience more heavily than another.
- **Better location logic.** The demo uses a small hand-written list of metro areas. Globally, location should use a maps or geocoding service (for example Azure Maps) and compare real distances or commute times, with remote and hybrid roles as options.
- **Languages.** The model reads CVs in many languages. The next steps are testing and tuning for key markets, and translating the screen and the generated text into the recruiter's language.
- **Local conventions.** Qualification names, CV formats and date styles differ by country, so the qualification levels and extraction prompts would be adapted for each market.

### Data protection and regulation

- **Regional deployments.** A separate deployment per region (South Africa, EU, and so on), each with its own Azure OpenAI resource in the same region, keeps CV data inside the required area. This is relevant to POPIA in South Africa and GDPR in Europe.
- **Hiring is a sensitive use of AI** in many jurisdictions, and some regulators treat it as high-risk. The existing design (human decision, explainable score, no protected traits, no automatic rejection) is the right foundation. At scale it would be extended with audit logs, bias testing and clear notices to candidates.

### Multi-client operation

- **Authentication and tenants.** Each client signs in (for example with Microsoft Entra ID or OAuth), and its data, settings and usage are kept separate.
- **API first.** The `/api/evaluate` endpoint can already be called by another system, so Ejoobi's own platform could call it directly and show the results inside the existing candidate screen.
- **Operations.** Application Insights is already enabled. At scale this grows into dashboards, alerts and usage reports per client.

## 4. How it can be improved

These are in rough priority order. The first four come from real weaknesses found while testing.

1. **Smarter skill matching.** Skills are matched by exact word, so a CV that says "PostgreSQL" does not count for a job asking for "SQL". A synonym list, or letting the model judge related skills, would fix this.
2. **A hard minimum for must-have requirements.** A candidate with 2 of the 3 required years scored 86.7, because a full skills match can outweigh an experience gap. Jobs should be able to mark requirements as essential, so a missing essential is flagged clearly or caps the score.
3. **Scanned PDFs.** Image-only CVs return no text and the app asks the user to paste it instead. Azure Document Intelligence can read scans with OCR. Word documents (.docx) should be supported as well.
4. **Location matching.** Replace the hand-written metro list with a proper geocoding service (see section 3).
5. **Compare many candidates.** Upload a batch of CVs for one job and get a ranked list with the same explanations, so a recruiter can compare candidates side by side.
6. **Learn from recruiters.** Let recruiters mark candidates as hired or rejected, and use that history to suggest better weights for each role. The scoring would stay explainable, and only the settings would change.
7. **Test the AI, not only the code.** Build a fixed set of CVs with known answers and run it whenever a prompt changes, so a change cannot quietly make extraction worse. Add bias checks by running the same CV with names or details changed and confirming the result does not move.
8. **Stronger security.** Move secrets from App Service settings into Azure Key Vault with a managed identity, add sign-in and rate limiting to the demo, and keep an audit log of who evaluated whom.
9. **Saved results.** Store evaluations in a database so recruiters can revisit and compare them. This needs a retention policy and deletion on request, because CVs are personal data.
10. **Candidate communication.** Send the approved message by email or WhatsApp straight from the app, always after the recruiter has reviewed it.
11. **Honesty about uncertainty.** Show a warning when the extraction looks unreliable, for example when a CV has no dates or very little text, so the recruiter knows to check the original.
