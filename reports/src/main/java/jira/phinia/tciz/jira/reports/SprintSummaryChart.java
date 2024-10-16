/*package com.atlassian.plugins.tutorial.jira.customcharts;

import com.atlassian.jira.charts.Chart;
import com.atlassian.jira.charts.jfreechart.ChartHelper;
import com.atlassian.jira.charts.util.ChartUtils;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.model.ChangeItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.tutorial.jira.classes.Sprint;
import com.atlassian.plugins.tutorial.jira.classes.SprintSummaryTable;
import com.atlassian.plugins.tutorial.jira.classes.TargetPI;
import com.atlassian.plugins.tutorial.jira.reportutils.CommonUtils;
import com.atlassian.plugins.tutorial.jira.reportutils.LoggingUtils;
import com.atlassian.plugins.tutorial.jira.reportutils.TableGenerator;
import com.atlassian.sal.api.search.SearchProvider;
import org.apache.commons.lang.time.DateUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import sun.rmi.runtime.Log;

import javax.swing.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class SprintSummaryChart {

    private final SearchProvider searchProvider;
    private final VersionManager versionManager;
    private final FieldManager fieldManager;
    private final TimeZoneManager timeZoneManager;
    private DefaultCategoryDataset versionCategoryDataset;
    TargetPI targetPI;

    public SprintSummaryChart(SearchProvider searchProvider, VersionManager versionManager, FieldManager fieldManager, TimeZoneManager timeZoneManager) {
        this.searchProvider = searchProvider;
        this.versionManager = versionManager;
        this.fieldManager = fieldManager;
        this.timeZoneManager = timeZoneManager;
        this.targetPI = null;
    }

    public Chart generateChart(ApplicationUser remoteUser, SearchRequest searchRequest, String projectKey, String targetPI, int width, int height, Map reqParams) {
        return this.generateChartInternal(remoteUser, searchRequest, projectKey, targetPI,
                width, height, false, reqParams);
    }

    public Chart generateInlineChart(ApplicationUser remoteUser, SearchRequest searchRequest, String projectKey, String targetPI, int width, int height, Map reqParams) {
        return this.generateChartInternal(remoteUser, searchRequest, projectKey, targetPI,
                width, height, true, reqParams);
    }

    private Chart generateChartInternal(final ApplicationUser remoteUser,
                                        final SearchRequest searchRequest,
                                        final String projectKey,
                                        final String targetPI,
                                        int width,
                                        int height,
                                        boolean inline,
                                        Map reqParams) {

        try
        {
            HashMap params = new HashMap();
            versionCategoryDataset = new DefaultCategoryDataset();
            this.targetPI = CommonUtils.getTargetPI(targetPI);

            String projectName = "Not Found";
            Project selectedProject = CommonUtils.getProjectObjectByKey(projectKey);
            if (selectedProject == null) selectedProject = CommonUtils.getCurrentProject();
            if(selectedProject != null) projectName = selectedProject.getName();

            ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
            CustomFieldManager customFieldManager = (CustomFieldManager) ComponentAccessor.getComponent(CustomFieldManager.class);

            CustomField customFieldStory = customFieldManager.getCustomFieldObjectByName("Story Points");
            CustomField customFieldSprint = customFieldManager.getCustomFieldObjectByName("Sprint");
            CustomField customFieldEpicLink = customFieldManager.getCustomFieldObjectByName("Epic Link");

            List<Sprint> sprintList = CommonUtils.getSprintList(projectKey);
            List<Issue> issuesList = CommonUtils.getStoriesAndDefectsByProject(projectKey);
            List<Double> velocityList = new ArrayList<>();
            List<SprintSummaryTable> sprintSummaryTableData = new ArrayList<>();

 
            for (Sprint sprint : sprintList)
            {
                Double plannedLoad = 0.0; // SP of the defects and stories at the beginning of the sprint
                Double actualLoad = 0.0;  // SP of the defects and stories at the end of the sprint
                Double doneStoryPoints = 0.0; // defect + story
                int doneStoryCount = 0;
                int totalStoryCount = 0;
                int doneDefectCount = 0;
                int totalDefectCount = 0;
                int totalEpicCount = 0;
                int doneEpicCount = 0;
                int percentageDoneSp = 0;
                int percentageDoneFeature = 0;
                int percentageVelocity = 0;
                String previousIssueKey = "key-0";
                SprintSummaryTable sprintSummaryTableRow = new SprintSummaryTable();

                if (sprint.startDate.after(this.targetPI.startDate) && sprint.startDate.before(this.targetPI.endDate))
                {
                    List<Issue> epicList = new ArrayList<>();
                    for (Issue issue : issuesList)
                    {
                        List<ChangeItemBean> changeItemBeansSprint = changeHistoryManager.getChangeItemsForField(issue, "Sprint");

                        Object sprintObject = issue.getCustomFieldValue(customFieldSprint);
                        if (sprintObject != null)
                        {
                            ArrayList<com.atlassian.greenhopper.service.sprint.Sprint> sprArray = (ArrayList<com.atlassian.greenhopper.service.sprint.Sprint>)sprintObject;
                            if (!sprArray.isEmpty() && sprArray.size() > 0 && (changeItemBeansSprint.isEmpty() || changeItemBeansSprint.size() == 0 || changeItemBeansSprint == null))
                            {
                                for (int i=0 ; i < sprArray.size(); i++){
                                    if(sprArray.get(i).getName().equals(sprint.sprintName)){

                                        Date issueCreated = issue.getCreated();
                                        double storyPointsOfIssue = issue.getCustomFieldValue(customFieldStory) != null ? (Double) issue.getCustomFieldValue(customFieldStory) : 0;
                                        if(issueCreated.before(sprint.startDate) || issueCreated.equals(sprint.startDate)){
                                            storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.startDate);
                                            plannedLoad = plannedLoad + storyPointsOfIssue;
                                            actualLoad = actualLoad + storyPointsOfIssue;
                                        } else if (issueCreated.after(sprint.startDate) && issueCreated.before(sprint.endDate)){
                                            storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.endDate);
                                            actualLoad = actualLoad + storyPointsOfIssue;
                                        }

                                        storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.endDate);
                                        boolean isIssueDoneInSprint = CommonUtils.checkIfStatusIsDone(changeHistoryManager, issue, sprint.startDate, sprint.endDate);
                                        if (issue.getIssueType().getName().equalsIgnoreCase("Story"))
                                        {
                                            totalStoryCount++;
                                            if (isIssueDoneInSprint)
                                            {
                                                doneStoryCount++;
                                                doneStoryPoints = doneStoryPoints + storyPointsOfIssue;   // defect + story
                                            }
                                            Object storyEpicLink = issue.getCustomFieldValue(customFieldEpicLink);
                                            if (storyEpicLink != null)
                                            {
                                                Issue epic = CommonUtils.getIssueByKey(storyEpicLink.toString());
                                                if (!(epicList.contains(epic))) // multiple stories might have the same epic
                                                {
                                                    epicList.add(epic);
                                                    totalEpicCount++;

                                                    if (epic.getStatus().getName().equals("Dev Done") || epic.getStatus().getName().equals("Verifying") ||
                                                            epic.getStatus().getName().equals("Complete") || epic.getStatus().getName().equals("Released")) {
                                                        doneEpicCount++;
                                                    }
                                                }
                                            }
                                        }
                                        else
                                        {
                                            totalDefectCount++;
                                            if (isIssueDoneInSprint)
                                            {
                                                doneDefectCount++;
                                                doneStoryPoints = doneStoryPoints + storyPointsOfIssue;   // defect + story
                                            }
                                        }

                                        velocityList.add(doneStoryPoints / plannedLoad);
                                    }
                                }
                            }
                        }

                        if (!changeItemBeansSprint.isEmpty() && changeItemBeansSprint.size() > 0){
                            boolean firstBeanHasSprint = true;
                            ChangeItemBean firstChange = changeItemBeansSprint.get(0);
                            Date firstChangeDate = new Date(firstChange.getCreated().getTime());
                            String firstChangeFrom = (firstChange.getFrom() != null) ? firstChange.getFrom() : "";
                            if (firstChangeFrom.equals(""))
                                firstBeanHasSprint = false;
//                            String firstChangeTo = (firstChange.getTo() != null) ? firstChange.getTo() : "";
//                            String firstDifference = CommonUtils.getStringDifference(firstChangeTo, firstChangeFrom);
//                            String sprintId = firstDifference.trim();
//                            if (sprintId.equals(""))
//                                firstBeanHasSprint = false;

                            if (firstBeanHasSprint){
                                List<String> firstChangeFromSprints = Arrays.asList(firstChangeFrom.split(","));
                                for (String sprintId : firstChangeFromSprints){
                                    if(sprintId.equals(sprint.sprintId) && !firstChangeDate.before(sprint.startDate)){

                                        Date issueCreated = issue.getCreated();
                                        double storyPointsOfIssue = issue.getCustomFieldValue(customFieldStory) != null ? (Double) issue.getCustomFieldValue(customFieldStory) : 0;
                                        if(issueCreated.before(sprint.startDate) || issueCreated.equals(sprint.startDate)){
                                            storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.startDate);
                                            plannedLoad = plannedLoad + storyPointsOfIssue;
                                            actualLoad = actualLoad + storyPointsOfIssue;

                                        } else if (issueCreated.after(sprint.startDate) && issueCreated.before(sprint.endDate)){
                                            storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.endDate);
                                            actualLoad = actualLoad + storyPointsOfIssue;
                                        }

                                        storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.endDate);
                                        boolean isIssueDoneInSprint = CommonUtils.checkIfStatusIsDone(changeHistoryManager, issue, sprint.startDate, sprint.endDate);
                                        if (issue.getIssueType().getName().equalsIgnoreCase("Story"))
                                        {
                                            totalStoryCount++;
                                            if (isIssueDoneInSprint)
                                            {
                                                doneStoryCount++;
                                                doneStoryPoints = doneStoryPoints + storyPointsOfIssue;   // defect + story
                                            }
                                            Object storyEpicLink = issue.getCustomFieldValue(customFieldEpicLink);
                                            if (storyEpicLink != null)
                                            {
                                                Issue epic = CommonUtils.getIssueByKey(storyEpicLink.toString());
                                                if (!(epicList.contains(epic))) // multiple stories might have the same epic
                                                {
                                                    epicList.add(epic);
                                                    totalEpicCount++;

                                                    if (epic.getStatus().getName().equals("Dev Done") || epic.getStatus().getName().equals("Verifying") ||
                                                            epic.getStatus().getName().equals("Complete") || epic.getStatus().getName().equals("Released")) {
                                                        doneEpicCount++;
                                                    }
                                                }
                                            }
                                        }
                                        else
                                        {
                                            totalDefectCount++;
                                            if (isIssueDoneInSprint)
                                            {
                                                doneDefectCount++;
                                                doneStoryPoints = doneStoryPoints + storyPointsOfIssue;   // defect + story
                                            }
                                        }

                                        velocityList.add(doneStoryPoints / plannedLoad);
                                    }
                                }
                            }
                        }

                        for (ChangeItemBean changeItemBean : changeItemBeansSprint) {
                            Date changedDate = new Date(changeItemBean.getCreated().getTime());

                            String sprintChangeFrom = (changeItemBean.getFrom() != null) ? changeItemBean.getFrom() : "";
                            String sprintChangeTo = (changeItemBean.getTo() != null) ? changeItemBean.getTo() : "";
                            if (sprintChangeTo.equals(""))
                                continue;
                            String difference = CommonUtils.getStringDifference(sprintChangeFrom, sprintChangeTo);
//                            difference = difference.equals("") ? CommonUtils.getStringDifference(sprintChangeTo, sprintChangeFrom) : difference;
                            String sprintId = difference.trim();

                            boolean nextChangeHasSprint;
                            boolean countSprint = true;

                            if (sprintId.equals(sprint.sprintId)) {
                                if (changeItemBeansSprint.indexOf(changeItemBean) < changeItemBeansSprint.size()-1) {
                                    ChangeItemBean nextBean = changeItemBeansSprint.get(changeItemBeansSprint.indexOf(changeItemBean) + 1);
                                    Date nextBeanDate = new Date(nextBean.getCreated().getTime());
                                    String nextChanges = (nextBean.getTo() != null) ? nextBean.getTo() : "";

                                    nextChangeHasSprint = nextChanges.contains(sprintId);
                                    if (!nextChangeHasSprint && (nextBeanDate.before(sprint.startDate) || nextBeanDate.equals(sprint.startDate) || nextBeanDate.equals(changedDate))) {
                                        countSprint = !countSprint;
                                    }
                                }
                            }

                            if (sprintId.equals(sprint.sprintId) && countSprint)
                            {
                                if (previousIssueKey.equals(issue.getKey()))
                                    continue;
                                else
                                    previousIssueKey = issue.getKey();

//                                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
//                                Date newSprintStartDate = formatter.parse(formatter.format(sprint.startDate));
//                                newSprintStartDate = DateUtils.addHours(newSprintStartDate, 23);
                                Date newSprintStartDate = sprint.startDate;

                                double storyPointsOfIssue = issue.getCustomFieldValue(customFieldStory) != null ? (Double) issue.getCustomFieldValue(customFieldStory) : 0;

                                if (changedDate.before(newSprintStartDate) || changedDate.equals(newSprintStartDate)) /// added to the sprint in the beginning of the sprint
                                {
                                    storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.startDate);
                                    plannedLoad = plannedLoad + storyPointsOfIssue;
                                    actualLoad = actualLoad + storyPointsOfIssue;
                                }
                                else if (changedDate.after(newSprintStartDate) && changedDate.before(sprint.endDate))  /// added to the sprint after the beginning of the sprint
                                {
                                    storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.endDate);
                                    actualLoad = actualLoad + storyPointsOfIssue;
                                }

                                storyPointsOfIssue = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, sprint.endDate);

                                boolean isIssueDoneInSprint = CommonUtils.checkIfStatusIsDone(changeHistoryManager, issue, sprint.startDate, sprint.endDate);
                                if (issue.getIssueType().getName().equalsIgnoreCase("Story"))
                                {
                                    totalStoryCount++;
                                    if (isIssueDoneInSprint)
                                    {
                                        doneStoryCount++;
                                        doneStoryPoints = doneStoryPoints + storyPointsOfIssue;   // defect + story
                                    }
                                    Object storyEpicLink = issue.getCustomFieldValue(customFieldEpicLink);
                                    if (storyEpicLink != null)
                                    {
                                        Issue epic = CommonUtils.getIssueByKey(storyEpicLink.toString());
                                        if (!(epicList.contains(epic))) // multiple stories might have the same epic
                                        {
                                            epicList.add(epic);
                                            totalEpicCount++;

                                            if (epic.getStatus().getName().equals("Dev Done") || epic.getStatus().getName().equals("Verifying") ||
                                                    epic.getStatus().getName().equals("Complete") || epic.getStatus().getName().equals("Released")) {
                                                doneEpicCount++;
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    totalDefectCount++;
                                    if (isIssueDoneInSprint)
                                    {
                                        doneDefectCount++;
                                        doneStoryPoints = doneStoryPoints + storyPointsOfIssue;   // defect + story
                                    }
                                }

                                velocityList.add(doneStoryPoints / plannedLoad);

                            }
                        }
                    }
                    if (actualLoad > 0){
                        percentageDoneSp = (int) Math.round((doneStoryPoints * 100) / actualLoad);
                        versionCategoryDataset.addValue(percentageDoneSp, "Done Story Points", sprint.sprintName);
                        versionCategoryDataset.addValue(100 - percentageDoneSp, "Not Done Story Points", sprint.sprintName);
                    }

                    if (totalEpicCount > 0){
                        percentageDoneFeature = (int) Math.round((doneEpicCount * 100) / totalEpicCount);
                        versionCategoryDataset.addValue(percentageDoneFeature, "Done Features", sprint.sprintName);
                        versionCategoryDataset.addValue(100 - percentageDoneFeature, "Not Done Features", sprint.sprintName);
                    } else {
                        versionCategoryDataset.addValue(0, "Done Features", sprint.sprintName);
                        versionCategoryDataset.addValue(0, "Not Done Features", sprint.sprintName);
                    }

                    if (plannedLoad > 0){
                        percentageVelocity = (int) Math.round((doneStoryPoints * 100) / plannedLoad);
                        versionCategoryDataset.addValue(percentageVelocity, "Velocity", sprint.sprintName);
                    }

                    sprintSummaryTableRow.sprintName = sprint.sprintName;
                    sprintSummaryTableRow.storyPointsPlanned = String.valueOf((int) Math.round(plannedLoad));
                    sprintSummaryTableRow.storyPointsDone = String.valueOf((int) Math.round(doneStoryPoints));
                    sprintSummaryTableRow.featuresPlanned = String.valueOf((int) Math.round(totalEpicCount));
                    sprintSummaryTableRow.featuresDone = String.valueOf((int) Math.round(doneEpicCount));

                    sprintSummaryTableData.add(sprintSummaryTableRow);
                }

            }

            ChartHelper chartHelper = (new SprintSummaryChartGenerator(versionCategoryDataset, velocityList, projectName, targetPI)).generateChart();
            if (inline) {
                chartHelper.generateInline(width, height);
            } else {
                chartHelper.generate(width, height);
            }


            params.put("chart", chartHelper.getLocation());
            params.put("imagemap", chartHelper.getImageMap());
            params.put("imagemapName", chartHelper.getImageMapName());
            params.put("imageWidth", Integer.valueOf(width));
            params.put("imageHeight", Integer.valueOf(height));
            params.put("completedDataSet", sprintSummaryTableData);

            if (inline) {
                String base64Image = ((ChartUtils) ComponentAccessor.getComponent(ChartUtils.class)).renderBase64Chart(chartHelper.getImage(), "Requirements Growth Chart");
                params.put("base64Image", base64Image);
            }

            return new Chart(chartHelper.getLocation(), chartHelper.getImageMap(), chartHelper.getImageMapName(), params);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage() + ex.getStackTrace());
        }
    }

}
*/