package com.resumeai.jobmatch.service.impl;

import com.resumeai.jobmatch.dto.request.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.request.CreateJobMatchRequest;
import com.resumeai.jobmatch.dto.request.JobSearchRequest;
import com.resumeai.jobmatch.dto.response.JobListingResponse;
import com.resumeai.jobmatch.dto.response.JobMatchResponse;
import com.resumeai.jobmatch.dto.response.ResumeDataResponse;
import com.resumeai.jobmatch.dto.response.SectionDataResponse;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.exception.ResourceNotFoundException;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import com.resumeai.jobmatch.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "are", "was", "were", "have", "has", "had",
            "this", "that", "these", "those", "from", "into", "onto", "about", "your",
            "their", "there", "here", "will", "would", "should", "could", "can",
            "job", "role", "looking", "need", "needs", "want", "wants", "required",
            "require", "requires", "using", "used", "use", "you", "our", "who",
            "how", "why", "when", "where", "what", "which", "while", "also", "than",
            "then", "them", "they", "been", "being", "over", "under", "more", "less",
            "very", "such", "through", "across", "between", "within", "without",
            "including", "include", "includes", "etc", "all", "any", "each", "every",
            "some", "not", "but"
    );

    // ── Simulated LinkedIn company data ─────────────────────────────────────
    private static final String[][] LINKEDIN_COMPANIES = {
            {"Google", "Mountain View, CA"},
            {"Microsoft", "Redmond, WA"},
            {"Amazon", "Seattle, WA"},
            {"Meta", "Menlo Park, CA"},
            {"Apple", "Cupertino, CA"},
            {"Netflix", "Los Gatos, CA"},
            {"Salesforce", "San Francisco, CA"},
            {"Adobe", "San Jose, CA"},
            {"Oracle", "Austin, TX"},
            {"IBM", "Armonk, NY"},
            {"Stripe", "San Francisco, CA"},
            {"Uber", "San Francisco, CA"},
            {"Airbnb", "San Francisco, CA"},
            {"Twitter", "San Francisco, CA"},
            {"LinkedIn", "Sunnyvale, CA"},
            {"Spotify", "Stockholm, Sweden"},
            {"Shopify", "Ottawa, Canada"},
            {"Atlassian", "Sydney, Australia"},
            {"Zoom", "San Jose, CA"},
            {"Slack", "San Francisco, CA"}
    };

    // ── Simulated Naukri company data (Indian market) ───────────────────────
    private static final String[][] NAUKRI_COMPANIES = {
            {"Tata Consultancy Services", "Mumbai"},
            {"Infosys", "Bangalore"},
            {"Wipro", "Bangalore"},
            {"HCL Technologies", "Noida"},
            {"Tech Mahindra", "Pune"},
            {"Cognizant", "Chennai"},
            {"Capgemini India", "Mumbai"},
            {"Accenture India", "Bangalore"},
            {"Reliance Jio", "Mumbai"},
            {"Flipkart", "Bangalore"},
            {"Paytm", "Noida"},
            {"Zoho Corporation", "Chennai"},
            {"Freshworks", "Chennai"},
            {"PhonePe", "Bangalore"},
            {"MakeMyTrip", "Gurgaon"},
            {"Swiggy", "Bangalore"},
            {"Razorpay", "Bangalore"},
            {"CRED", "Bangalore"},
            {"Dream11", "Mumbai"},
            {"Ola", "Bangalore"}
    };

    private static final String[] SALARY_RANGES_USD = {
            "$80,000 - $120,000", "$100,000 - $150,000", "$120,000 - $180,000",
            "$90,000 - $130,000", "$110,000 - $160,000", "$130,000 - $200,000",
            "$70,000 - $100,000", "$140,000 - $220,000"
    };

    private static final String[] SALARY_RANGES_INR = {
            "₹8L - ₹15L", "₹10L - ₹20L", "₹12L - ₹25L", "₹15L - ₹30L",
            "₹6L - ₹12L", "₹18L - ₹35L", "₹20L - ₹40L", "₹25L - ₹50L"
    };

    private static final String[] POSTED_DATES = {
            "1 day ago", "2 days ago", "3 days ago", "5 days ago",
            "1 week ago", "2 weeks ago", "Just now", "4 hours ago"
    };

    private static final String[] EMPLOYMENT_TYPES = {
            "Full-time", "Full-time", "Full-time", "Contract",
            "Full-time", "Remote", "Hybrid", "Full-time"
    };

    private final JobMatchRepository jobMatchRepository;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${services.resume-service-url}")
    private String resumeServiceUrl;

    @Value("${services.section-service-url}")
    private String sectionServiceUrl;

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE / ANALYZE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public JobMatchResponse createJobMatch(Integer userId, CreateJobMatchRequest request, String authorizationHeader) {
        ResumeDataResponse resume = fetchResume(request.getResumeId(), authorizationHeader);
        List<SectionDataResponse> sections = fetchSections(request.getResumeId(), authorizationHeader);
        String combinedResumeText = buildResumeText(resume, sections);

        return saveAndScore(userId, request.getResumeId(), request.getJobTitle(),
                request.getCompanyName(), request.getJobUrl(), request.getJobDescription(), combinedResumeText);
    }

    @Override
    @Transactional
    public JobMatchResponse analyzeJobFit(Integer userId, AnalyzeJobFitRequest request, String authorizationHeader) {
        ResumeDataResponse resume = fetchResume(request.getResumeId(), authorizationHeader);
        List<SectionDataResponse> sections = fetchSections(request.getResumeId(), authorizationHeader);
        String combinedResumeText = buildResumeText(resume, sections);

        return saveAndScore(userId, request.getResumeId(), request.getJobTitle(),
                request.getCompanyName(), request.getJobUrl(), request.getJobDescription(), combinedResumeText);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  QUERIES
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<JobMatchResponse> getMyJobMatches(Integer userId) {
        return jobMatchRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobMatchResponse> getJobMatchesByResume(Integer userId, Integer resumeId) {
        return jobMatchRepository.findByResumeIdAndUserIdOrderByCreatedAtDesc(resumeId, userId)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobMatchResponse getJobMatchById(Integer userId, Integer jobMatchId) {
        return mapToResponse(findOwnedJobMatch(userId, jobMatchId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobMatchResponse> getTopMatches(Integer userId, int limit) {
        return jobMatchRepository.findByUserIdOrderByMatchScoreDesc(userId)
                .stream()
                .limit(limit)
                .map(this::mapToResponse)
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKMARKS
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public JobMatchResponse updateBookmark(Integer userId, Integer jobMatchId, boolean bookmarked) {
        JobMatch jobMatch = findOwnedJobMatch(userId, jobMatchId);
        jobMatch.setBookmarked(bookmarked);
        return mapToResponse(jobMatchRepository.save(jobMatch));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobMatchResponse> getBookmarkedMatches(Integer userId) {
        return jobMatchRepository.findByUserIdAndBookmarkedTrueOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void deleteMatch(Integer userId, Integer jobMatchId) {
        JobMatch jobMatch = findOwnedJobMatch(userId, jobMatchId);
        jobMatchRepository.delete(jobMatch);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TAILORING RECOMMENDATIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public String getTailoringRecommendations(Integer userId, Integer jobMatchId) {
        JobMatch jobMatch = findOwnedJobMatch(userId, jobMatchId);

        List<String> missing = jobMatch.getMissingKeywords() == null || jobMatch.getMissingKeywords().isBlank()
                ? List.of()
                : Arrays.stream(jobMatch.getMissingKeywords().split(",\\s*")).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("## Tailoring Recommendations for: ").append(jobMatch.getJobTitle()).append("\n\n");

        if (jobMatch.getMatchScore() >= 80) {
            sb.append("✅ **Excellent Match (").append(jobMatch.getMatchScore()).append("%)** — Your resume is well-aligned.\n\n");
            sb.append("### Fine-tuning suggestions:\n");
            sb.append("- Mirror the exact phrasing from the job description in your experience bullets\n");
            sb.append("- Quantify your achievements with specific metrics (e.g., 'improved performance by 30%')\n");
            sb.append("- Ensure your summary section directly references the role title\n");
        } else if (jobMatch.getMatchScore() >= 50) {
            sb.append("⚠️ **Moderate Match (").append(jobMatch.getMatchScore()).append("%)** — Some improvements needed.\n\n");
            sb.append("### Key improvements:\n");
            if (!missing.isEmpty()) {
                sb.append("- **Add missing skills to your Skills section:** ").append(String.join(", ", missing)).append("\n");
            }
            sb.append("- Rewrite your experience bullets to include terminology from the job posting\n");
            sb.append("- Add a 'Technical Skills' or 'Core Competencies' section if missing\n");
            sb.append("- Tailor your professional summary to match the role's primary requirements\n");
        } else {
            sb.append("❌ **Low Match (").append(jobMatch.getMatchScore()).append("%)** — Significant gaps detected.\n\n");
            sb.append("### Critical actions:\n");
            if (!missing.isEmpty()) {
                sb.append("- **Missing critical keywords:** ").append(String.join(", ", missing)).append("\n");
            }
            sb.append("- Consider restructuring your resume to prioritize relevant experience\n");
            sb.append("- Add relevant certifications or coursework if applicable\n");
            sb.append("- Rewrite your professional summary to directly address the job requirements\n");
            sb.append("- Consider adding a 'Relevant Projects' section showcasing applicable work\n");
        }

        sb.append("\n### General ATS optimization tips:\n");
        sb.append("- Use standard section headings (Experience, Education, Skills)\n");
        sb.append("- Avoid tables, columns, or graphics that ATS systems may not parse\n");
        sb.append("- Use both spelled-out and abbreviated forms (e.g., 'Artificial Intelligence (AI)')\n");

        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LIVE JOB SEARCH (LinkedIn via simulation + Naukri via simulation)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<JobListingResponse> searchLiveJobs(Integer userId, JobSearchRequest request) {
        String title = request.getJobTitle().trim();
        String location = request.getLocation() == null || request.getLocation().isBlank()
                ? "Remote" : request.getLocation().trim();

        List<JobListingResponse> results = new ArrayList<>();

        // Fetch from LinkedIn (simulated via JSearch-style data)
        try {
            List<Map<String, Object>> linkedInJobs = fetchJobsFromLinkedIn(title, location);
            for (Map<String, Object> job : linkedInJobs) {
                results.add(JobListingResponse.builder()
                        .source("LinkedIn")
                        .jobTitle(String.valueOf(job.get("jobTitle")))
                        .companyName(String.valueOf(job.get("companyName")))
                        .location(String.valueOf(job.get("location")))
                        .jobUrl(String.valueOf(job.get("jobUrl")))
                        .relevanceScore((Integer) job.get("relevanceScore"))
                        .description(String.valueOf(job.get("description")))
                        .salary(String.valueOf(job.get("salary")))
                        .postedDate(String.valueOf(job.get("postedDate")))
                        .employmentType(String.valueOf(job.get("employmentType")))
                        .build());
            }
        } catch (Exception ex) {
            log.warn("Error fetching LinkedIn jobs: {}", ex.getMessage());
        }

        // Fetch from Naukri (simulated)
        try {
            List<Map<String, Object>> naukriJobs = fetchJobsFromNaukri(title, location);
            for (Map<String, Object> job : naukriJobs) {
                results.add(JobListingResponse.builder()
                        .source("Naukri")
                        .jobTitle(String.valueOf(job.get("jobTitle")))
                        .companyName(String.valueOf(job.get("companyName")))
                        .location(String.valueOf(job.get("location")))
                        .jobUrl(String.valueOf(job.get("jobUrl")))
                        .relevanceScore((Integer) job.get("relevanceScore"))
                        .description(String.valueOf(job.get("description")))
                        .salary(String.valueOf(job.get("salary")))
                        .postedDate(String.valueOf(job.get("postedDate")))
                        .employmentType(String.valueOf(job.get("employmentType")))
                        .build());
            }
        } catch (Exception ex) {
            log.warn("Error fetching Naukri jobs: {}", ex.getMessage());
        }

        // Sort by relevance score descending
        results.sort((a, b) -> Integer.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        return results;
    }

    @Override
    public List<Map<String, Object>> fetchJobsFromLinkedIn(String query, String location) {
        log.info("Fetching LinkedIn jobs for query='{}', location='{}'", query, location);
        List<Map<String, Object>> jobs = new ArrayList<>();

        String titleLower = query.toLowerCase();
        int count = 5 + random.nextInt(4); // 5-8 results

        for (int i = 0; i < count && i < LINKEDIN_COMPANIES.length; i++) {
            int companyIdx = (i + Math.abs(titleLower.hashCode()) % 7) % LINKEDIN_COMPANIES.length;
            String company = LINKEDIN_COMPANIES[companyIdx][0];
            String companyLoc = location.equalsIgnoreCase("Remote") ? LINKEDIN_COMPANIES[companyIdx][1] : location;

            String jobTitle = generateJobTitle(query, i);
            int relevance = 95 - (i * 4) - random.nextInt(5);

            Map<String, Object> job = new LinkedHashMap<>();
            job.put("jobTitle", jobTitle);
            job.put("companyName", company);
            job.put("location", companyLoc);
            job.put("jobUrl", "https://www.linkedin.com/jobs/view/" + (3800000000L + Math.abs(titleLower.hashCode()) + i));
            job.put("relevanceScore", Math.max(60, Math.min(99, relevance)));
            job.put("description", generateDescription(jobTitle, company));
            job.put("salary", SALARY_RANGES_USD[i % SALARY_RANGES_USD.length]);
            job.put("postedDate", POSTED_DATES[i % POSTED_DATES.length]);
            job.put("employmentType", EMPLOYMENT_TYPES[i % EMPLOYMENT_TYPES.length]);
            jobs.add(job);
        }

        return jobs;
    }

    @Override
    public List<Map<String, Object>> fetchJobsFromNaukri(String query, String location) {
        log.info("Fetching Naukri jobs for query='{}', location='{}'", query, location);
        List<Map<String, Object>> jobs = new ArrayList<>();

        String titleLower = query.toLowerCase();
        int count = 5 + random.nextInt(4); // 5-8 results

        for (int i = 0; i < count && i < NAUKRI_COMPANIES.length; i++) {
            int companyIdx = (i + Math.abs(titleLower.hashCode()) % 5 + 3) % NAUKRI_COMPANIES.length;
            String company = NAUKRI_COMPANIES[companyIdx][0];
            String companyLoc = location.equalsIgnoreCase("Remote") ? NAUKRI_COMPANIES[companyIdx][1] : location;

            String jobTitle = generateJobTitle(query, i);
            int relevance = 93 - (i * 5) - random.nextInt(4);

            Map<String, Object> job = new LinkedHashMap<>();
            job.put("jobTitle", jobTitle);
            job.put("companyName", company);
            job.put("location", companyLoc);
            job.put("jobUrl", "https://www.naukri.com/" + titleLower.replace(" ", "-") + "-jobs-" + (i + 1));
            job.put("relevanceScore", Math.max(55, Math.min(98, relevance)));
            job.put("description", generateDescription(jobTitle, company));
            job.put("salary", SALARY_RANGES_INR[i % SALARY_RANGES_INR.length]);
            job.put("postedDate", POSTED_DATES[(i + 2) % POSTED_DATES.length]);
            job.put("employmentType", EMPLOYMENT_TYPES[(i + 1) % EMPLOYMENT_TYPES.length]);
            jobs.add(job);
        }

        return jobs;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private JobMatchResponse saveAndScore(Integer userId, Integer resumeId, String jobTitle,
                                          String companyName, String jobUrl, String jobDescription,
                                          String combinedResumeText) {
        Set<String> resumeWords = tokenize(combinedResumeText);
        Set<String> jobWords = tokenize(jobDescription);

        List<String> matchedKeywords = jobWords.stream()
                .filter(resumeWords::contains).limit(15).toList();
        List<String> missingKeywords = jobWords.stream()
                .filter(word -> !resumeWords.contains(word)).limit(15).toList();

        int totalKeywords = Math.max(jobWords.size(), 1);
        int matchScore = (int) Math.round((matchedKeywords.size() * 100.0) / totalKeywords);
        matchScore = Math.max(0, Math.min(100, matchScore));

        String recommendation;
        if (matchScore >= 80) {
            recommendation = "Excellent match! Your resume strongly aligns with this role. "
                    + "Fine-tune by mirroring exact phrases from the job description.";
        } else if (matchScore >= 50) {
            recommendation = "Moderate match. Improve by adding keywords: " + String.join(", ", missingKeywords);
        } else {
            recommendation = "Low match. Significant gaps detected. Add these critical skills: "
                    + String.join(", ", missingKeywords);
        }

        JobMatch jobMatch = JobMatch.builder()
                .userId(userId)
                .resumeId(resumeId)
                .jobTitle(jobTitle)
                .companyName(companyName)
                .jobUrl(jobUrl)
                .source(resolveSource(jobUrl))
                .jobDescription(jobDescription)
                .matchScore(matchScore)
                .matchedKeywords(String.join(", ", matchedKeywords))
                .missingKeywords(String.join(", ", missingKeywords))
                .recommendation(recommendation)
                .bookmarked(false)
                .build();

        return mapToResponse(jobMatchRepository.save(jobMatch));
    }

    private String generateJobTitle(String baseQuery, int index) {
        String[] prefixes = {"Senior ", "", "Lead ", "Staff ", "Junior ", "", "Principal ", ""};
        String[] suffixes = {"", " Engineer", " Developer", " Analyst", " Specialist", " Architect", "", " Consultant"};

        if (index == 0) return baseQuery;

        String prefix = prefixes[index % prefixes.length];
        String base = baseQuery.contains(" ") ? baseQuery : baseQuery + suffixes[index % suffixes.length];
        return prefix + base;
    }

    private String generateDescription(String jobTitle, String company) {
        return company + " is looking for a talented " + jobTitle
                + " to join our growing team. You will work on cutting-edge projects, "
                + "collaborate with cross-functional teams, and drive innovation. "
                + "We offer competitive compensation, flexible work arrangements, "
                + "and opportunities for career growth.";
    }

    private String buildResumeText(ResumeDataResponse resume, List<SectionDataResponse> sections) {
        StringBuilder builder = new StringBuilder();
        builder.append(resume.getTitle()).append(" ");
        if (resume.getTargetJobTitle() != null) {
            builder.append(resume.getTargetJobTitle()).append(" ");
        }
        for (SectionDataResponse section : sections) {
            if (Boolean.TRUE.equals(section.getIsVisible())) {
                builder.append(section.getTitle()).append(" ");
                builder.append(section.getContent()).append(" ");
            }
        }
        return builder.toString();
    }

    private Set<String> tokenize(String input) {
        return Arrays.stream(input.toLowerCase()
                        .replaceAll("[^a-z0-9 ]", " ")
                        .split("\\s+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings("unchecked")
    private ResumeDataResponse fetchResume(Integer resumeId, String authorizationHeader) {
        String url = resumeServiceUrl + "/api/v1/resumes/" + resumeId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        ResumeDataResponse resume = new ResumeDataResponse();
        resume.setResumeId((Integer) data.get("resumeId"));
        resume.setUserId((Integer) data.get("userId"));
        resume.setTitle((String) data.get("title"));
        resume.setTargetJobTitle((String) data.get("targetJobTitle"));
        resume.setTemplateId((Integer) data.get("templateId"));
        resume.setAtsScore((Integer) data.get("atsScore"));
        resume.setStatus(String.valueOf(data.get("status")));
        resume.setLanguage((String) data.get("language"));
        resume.setIsPublic((Boolean) data.get("isPublic"));
        resume.setViewCount((Integer) data.get("viewCount"));
        return resume;
    }

    @SuppressWarnings("unchecked")
    private List<SectionDataResponse> fetchSections(Integer resumeId, String authorizationHeader) {
        String url = sectionServiceUrl + "/api/v1/sections/resume/" + resumeId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return data.stream().map(item -> {
            SectionDataResponse section = new SectionDataResponse();
            section.setSectionId((Integer) item.get("sectionId"));
            section.setResumeId((Integer) item.get("resumeId"));
            section.setUserId((Integer) item.get("userId"));
            section.setSectionType(String.valueOf(item.get("sectionType")));
            section.setTitle((String) item.get("title"));
            section.setContent((String) item.get("content"));
            section.setDisplayOrder((Integer) item.get("displayOrder"));
            section.setIsVisible((Boolean) item.get("isVisible"));
            return section;
        }).toList();
    }

    private JobMatchResponse mapToResponse(JobMatch jobMatch) {
        List<String> matched = jobMatch.getMatchedKeywords() == null || jobMatch.getMatchedKeywords().isBlank()
                ? List.of()
                : Arrays.stream(jobMatch.getMatchedKeywords().split(",\\s*")).toList();
        List<String> missing = jobMatch.getMissingKeywords() == null || jobMatch.getMissingKeywords().isBlank()
                ? List.of()
                : Arrays.stream(jobMatch.getMissingKeywords().split(",\\s*")).toList();

        return JobMatchResponse.builder()
                .jobMatchId(jobMatch.getJobMatchId())
                .userId(jobMatch.getUserId())
                .resumeId(jobMatch.getResumeId())
                .jobTitle(jobMatch.getJobTitle())
                .companyName(jobMatch.getCompanyName())
                .jobUrl(jobMatch.getJobUrl())
                .source(jobMatch.getSource())
                .jobDescription(jobMatch.getJobDescription())
                .matchScore(jobMatch.getMatchScore())
                .matchedKeywords(matched)
                .missingKeywords(missing)
                .recommendation(jobMatch.getRecommendation())
                .bookmarked(Boolean.TRUE.equals(jobMatch.getBookmarked()))
                .createdAt(jobMatch.getCreatedAt())
                .build();
    }

    private JobMatch findOwnedJobMatch(Integer userId, Integer jobMatchId) {
        JobMatch jobMatch = jobMatchRepository.findById(jobMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("JobMatch", "jobMatchId", jobMatchId));
        if (!jobMatch.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("JobMatch", "jobMatchId", jobMatchId);
        }
        return jobMatch;
    }

    private String resolveSource(String jobUrl) {
        if (jobUrl == null) return "Manual";
        String normalized = jobUrl.toLowerCase();
        if (normalized.contains("linkedin")) return "LinkedIn";
        if (normalized.contains("naukri")) return "Naukri";
        return "Manual";
    }
}
