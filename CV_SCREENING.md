# CV Screening Bot

The fifth "bot" in HR Bots. Companies post jobs. Candidates create an account and upload a CV, either as a file or zipped. The bot reads the CV, scores it against the job and sorts it, and then a person in HR takes over.

## The flow

```
Candidate uploads CV  ->  text extracted  ->  bot scores 0-100  ->  first status
  (PDF / DOCX / TXT,       (PDFBox for PDF,     keyword score,         >= shortlistAt     SHORTLISTED
   or a ZIP of one)         plain zip for DOCX)  + AI score if on      <  autoRejectBelow AUTO_REJECTED
                                                                       in between         UNDER_REVIEW
                                                                       unreadable CV      UNDER_REVIEW
                                                        |
                                                        v
                     HR decides: INTERVIEW -> HIRED, or REJECTED
                     (an AUTO_REJECTED application can be REINSTATED)
```

## How the score works

**Keyword score.** This always runs. It needs no internet and no API key, and it lives in `screening/KeywordCvScorer.java`.

| Part | Points | How |
|---|---|---|
| Required skills | 60 | Share of the job's must-have skills found in the CV |
| Nice-to-have skills | 15 | Share of bonus skills found |
| Experience | 15 | Years detected vs the job's minimum. Years come from "5 years" or from date ranges like "2019 - present", with overlaps counted once |
| Education | 10 | Any education keyword found (e.g. BSc, HND) |

Skills match as whole words, so "javascript" doesn't count as "java" and "legit" doesn't count as "git". Common variants are understood too, for example "springboot", "mongo" and "restful".

**AI score.** This is optional and off by default. It lives in `screening/AiCvScreener.java`. It sends the job and the CV text to Claude, which returns a 0-100 score and a short explanation. The final score is the average of the keyword score and the AI score. If the AI is off or fails, the keyword score is used on its own.

To turn the AI score on:

1. Set the environment variable `ANTHROPIC_API_KEY` to your key. Never put the key in a file you commit.
2. In `application.properties`, set `app.screening.ai.enabled=true`.

## Fairness safeguards (good to mention at the defence)

- **No auto-reject on unreadable CVs.** A scanned image CV gives almost no text, so it goes to a human instead of being scored.
- **HR can reinstate.** Any auto-rejected application can be moved back to review.
- **A human makes every final decision.** The bot never invites, hires or finally rejects anyone.
- **The AI ignores personal details.** Its instructions forbid judging on name, gender, age, religion, ethnicity, state of origin or photos. It is also told to ignore any instructions written inside a CV.
- **The AI can't decide alone.** The final score is an average with the transparent keyword score.
- **Candidates see helpful feedback.** They see which skills were and weren't found. Internal labels and HR notes stay private.

## Endpoints

| Method and path | Who | What |
|---|---|---|
| `POST /api/candidates/register`, `/login` | anyone | Candidate account |
| `GET /api/candidates/me` | CANDIDATE | Own profile |
| `GET /api/jobs`, `/api/jobs/{id}` | anyone | Public job board |
| `POST /api/jobs/{id}/apply` (multipart: `cv`, `coverNote`) | CANDIDATE | Apply with a CV |
| `GET /api/applications/mine` | CANDIDATE | Own applications and status |
| `GET /api/jobs/manage` | HR_ADMIN | All jobs with application counts |
| `POST /api/jobs`, `PUT /api/jobs/{id}` | HR_ADMIN | Create or edit a job |
| `PATCH /api/jobs/{id}/close`, `/reopen` | HR_ADMIN | Open or close a job |
| `GET /api/jobs/{id}/applications` | HR_ADMIN | Applicants ranked by score |
| `GET /api/applications/{id}` | HR_ADMIN | One application |
| `GET /api/applications/{id}/cv` | HR_ADMIN | Download the original CV |
| `PATCH /api/applications/{id}/decision` | HR_ADMIN | INTERVIEW / HIRED / REJECTED / REINSTATE |

Candidates get 403 on every staff endpoint (leave, dashboard, onboarding, policy), and staff can't apply.

## Demo script

The seeder creates two jobs: "Junior Backend Developer" and "HR Assistant".

1. Open `/careers`, create a candidate account, and apply for **Junior Backend Developer** with each file in `samples/`. Each file needs its own candidate account, because one account can apply only once per job.
   - `cv-strong-chidi-eze.docx` scores 100 and is **Shortlisted**
   - `cv-middle-amaka-obi.zip` (a Word CV inside a zip) scores about 54 and goes **Under review**
   - `cv-weak-tunde-bello.txt` scores about 8 and is **Auto-rejected**
2. Apply for **HR Assistant** with `cv-hr-assistant-zainab-musa.pdf`. It scores 100 and is **Shortlisted**, which shows PDF reading working.
3. Log in as `admin@hrbots.test`, open **Recruitment** and click **Applicants**. Candidates are ranked best first. Reinstate Tunde, invite Chidi to interview, then mark him hired.
