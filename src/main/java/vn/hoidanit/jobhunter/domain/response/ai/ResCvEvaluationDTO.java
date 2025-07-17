package vn.hoidanit.jobhunter.domain.response.ai;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResCvEvaluationDTO {
    private int overallScore;
    private String summary;
    private List<String> strengths;
    private List<ImprovementSuggestion> improvements;
    private String estimatedSalaryRange;
    private List<RoadmapStep> suggestedRoadmap;
    private List<SuggestedJob> relevantJobs;

    @Getter
    @Setter
    public static class ImprovementSuggestion {
        private String area;
        private String suggestion;
    }

    @Getter
    @Setter
    public static class RoadmapStep {
        private int step;
        private String action;
        private String reason;
    }

    @Getter
    @Setter
    public static class SuggestedJob {
        private long jobId;
        private String jobTitle;
        private String companyName;
        private String matchReason;
    }
}