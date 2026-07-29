package com.school.teaching.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardOverviewVO {
    private long onlineUsers;
    private long todayLogins;
    private String uptime;
    private long todayExamsCreated;
    private long ongoingExams;
    private long pendingExamGrades;
    private long todayHomeworkAssigned;
    private long pendingSubmissions;
    private long overdueSubmissions;
    private long ungradedSubmissions;
    private long todayBbsPosts;
    private long todayBbsReplies;
    private long todayCreditsAwarded;
    private long todayRedeemCount;
    private List<Map<String, Object>> weeklyActiveTrend;
    private List<Map<String, Object>> topApiRequests;
}
