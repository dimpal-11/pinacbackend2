package in.sb.pinac.dto.whatsapp;

import java.util.List;
import java.util.Map;

public class WhatsAppAnalyticsDto {

    private long totalConversations;
    private long activeChats;
    private long pendingHumanChats;
    private long aiResolvedChats;
    private long totalMessages;
    private long dailyMessages;
    private long weeklyMessages;
    private long monthlyMessages;
    private double aiSuccessRate;
    private double humanTransferRate;
    private String avgResponseTime;
    private List<Map<String, Object>> topAskedTopics;
    private List<Map<String, Object>> hourlyActivity;
    private List<Map<String, Object>> dailyTrends;

    public WhatsAppAnalyticsDto() {}

    public long getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(long totalConversations) {
        this.totalConversations = totalConversations;
    }

    public long getActiveChats() {
        return activeChats;
    }

    public void setActiveChats(long activeChats) {
        this.activeChats = activeChats;
    }

    public long getPendingHumanChats() {
        return pendingHumanChats;
    }

    public void setPendingHumanChats(long pendingHumanChats) {
        this.pendingHumanChats = pendingHumanChats;
    }

    public long getAiResolvedChats() {
        return aiResolvedChats;
    }

    public void setAiResolvedChats(long aiResolvedChats) {
        this.aiResolvedChats = aiResolvedChats;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public long getDailyMessages() {
        return dailyMessages;
    }

    public void setDailyMessages(long dailyMessages) {
        this.dailyMessages = dailyMessages;
    }

    public long getWeeklyMessages() {
        return weeklyMessages;
    }

    public void setWeeklyMessages(long weeklyMessages) {
        this.weeklyMessages = weeklyMessages;
    }

    public long getMonthlyMessages() {
        return monthlyMessages;
    }

    public void setMonthlyMessages(long monthlyMessages) {
        this.monthlyMessages = monthlyMessages;
    }

    public double getAiSuccessRate() {
        return aiSuccessRate;
    }

    public void setAiSuccessRate(double aiSuccessRate) {
        this.aiSuccessRate = aiSuccessRate;
    }

    public double getHumanTransferRate() {
        return humanTransferRate;
    }

    public void setHumanTransferRate(double humanTransferRate) {
        this.humanTransferRate = humanTransferRate;
    }

    public String getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(String avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public List<Map<String, Object>> getTopAskedTopics() {
        return topAskedTopics;
    }

    public void setTopAskedTopics(List<Map<String, Object>> topAskedTopics) {
        this.topAskedTopics = topAskedTopics;
    }

    public List<Map<String, Object>> getHourlyActivity() {
        return hourlyActivity;
    }

    public void setHourlyActivity(List<Map<String, Object>> hourlyActivity) {
        this.hourlyActivity = hourlyActivity;
    }

    public List<Map<String, Object>> getDailyTrends() {
        return dailyTrends;
    }

    public void setDailyTrends(List<Map<String, Object>> dailyTrends) {
        this.dailyTrends = dailyTrends;
    }
}
