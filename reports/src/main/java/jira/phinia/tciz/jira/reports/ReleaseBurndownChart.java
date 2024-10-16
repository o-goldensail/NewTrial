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
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.tutorial.jira.classes.*;
import com.atlassian.plugins.tutorial.jira.reportutils.CommonUtils;
import com.atlassian.plugins.tutorial.jira.reportutils.LoggingUtils;
import com.atlassian.sal.api.search.SearchProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.time.Minute;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ReleaseBurndownChart {
    private final SearchProvider searchProvider;
    private final VersionManager versionManager;
    private final FieldManager fieldManager;
    private final TimeZoneManager timeZoneManager;
    private ChangeHistoryManager changeHistoryManager;
    private CustomFieldManager customFieldManager;
    private Map<String, Sprint> sprintList;
    private List<EpicVersionIssue> incompleteIssues;
    private Map<Sprint, List<Issue>> versionSprintList = new TreeMap<Sprint, List<Issue>>(
            new Comparator<Sprint>() {
                @Override public int compare(Sprint p1, Sprint p2)
                {
                    return p1.getSprintId() - p2.getSprintId();
                }
            }
    );
    private DefaultCategoryDataset versionCategoryDataset;

    public ReleaseBurndownChart(SearchProvider searchProvider, VersionManager versionManager, FieldManager fieldManager, TimeZoneManager timeZoneManager)
    {
        this.searchProvider = searchProvider;
        this.versionManager = versionManager;
        this.fieldManager = fieldManager;
        this.timeZoneManager = timeZoneManager;
        this.sprintList = new HashMap<>();
        this.incompleteIssues = new ArrayList<>();
    }

    public Chart generateChart(ApplicationUser remoteUser, SearchRequest searchRequest, String projectKey, String versionKey, boolean cumulative, int width, int height, Map reqParams) {
        return this.generateChartInternal(remoteUser, searchRequest, projectKey, versionKey, cumulative, width, height, false, reqParams);
    }

    public Chart generateInlineChart(ApplicationUser remoteUser, SearchRequest searchRequest, String projectKey, String versionKey, boolean cumulative, int width, int height, Map reqParams) {
        return this.generateChartInternal(remoteUser, searchRequest, projectKey, versionKey, cumulative, width, height, true, reqParams);
    }

    private Chart generateChartInternal(final ApplicationUser remoteUser, final SearchRequest searchRequest, final String projectKey, final String versionKey, final boolean cumulative, int width, int height, boolean inline, Map reqParams)
    {
        HashMap params = new HashMap();

        try
        {
            versionCategoryDataset = new DefaultCategoryDataset();
            changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
            customFieldManager = (CustomFieldManager) ComponentAccessor.getComponent(CustomFieldManager.class);
            CustomField sprintCustomField = customFieldManager.getCustomFieldObjectByName("Sprint");

            String versionName = "", strCompletedStoryPoints = "";
            double unestimatedPercentage = 0;
            EpicVersionIssueData dataset = new EpicVersionIssueData();

            String datePattern = "MM/dd/yyyy";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
            Date versionStartDate = new Date();
            Date versionReleaseDate = new Date();
            Long projectId;

            String projectName = "Not Found";
            Project projectObj = CommonUtils.getProjectObjectByKey(projectKey);
            if (projectObj == null) projectObj = CommonUtils.getCurrentProject();
            if(projectObj != null) projectName = projectObj.getName();

            if (projectKey.matches("[0-9]+")) {
                projectId = Long.parseLong(projectKey);
            } else {
                Project selectedProject = ComponentAccessor.getProjectManager().getProjectObjByKey(projectKey);
                projectId = selectedProject.getId();
            }

            Collection<Version> versionList = CommonUtils.getVersions(projectId);
            for (Version v : versionList) {
                if (versionKey.equals(v.getName()))
                {
                    versionStartDate = v.getStartDate();
                    versionReleaseDate = v.getReleaseDate();
                    break;
                }
            }

            if (versionStartDate == null) {
                versionStartDate = new Date();
            }

            if (versionReleaseDate == null) {
                versionReleaseDate = new Date();
            }

            List<Issue> epics = CommonUtils.getEpicsByProject(projectKey);
            Collection<Version> versionCollection;
            List<String> previousSprintIdList = new ArrayList<>();

            if (epics != null)
            {
                int unEstimatedIssues = 0, estimatedIssues = 0;
                double completedStoryPoints = 0;

                for (Issue epic : epics)  //Iterate through epics
                {
                    versionCollection = epic.getFixVersions();

                    if (versionCollection != null && versionCollection.size() > 0) {
                        for (Version version : versionCollection) // Iterate through fix versions in a epic
                        {
                            if (versionKey.equals(version.getName()))  // checks selected fix version is available on the epics
                            {
                                versionName = version.getName();
                                Collection<Issue> stories = CommonUtils.getIssuesInEpic(epic);
                                if (stories != null && stories.size() > 0) {
                                    for (Issue story: stories)
                                    {
                                        Collection<Version> storyVersionCollection = story.getFixVersions();
                                        if (storyVersionCollection != null && storyVersionCollection.size() > 0) {
                                            for (Version storyVersion : storyVersionCollection) // Iterate through fix versions in a story
                                            {
                                                if (versionKey.equals(storyVersion.getName()))  // checks selected fix version is available on the story
                                                {
                                                    String issueKey = story.getKey();
                                                    List<ChangeItemBean> changeItemBeansSprint = changeHistoryManager.getChangeItemsForField(story, "Sprint");

                                                    if (changeItemBeansSprint != null && changeItemBeansSprint.size() > 0)
                                                    {
                                                        for (ChangeItemBean changeItemBean : changeItemBeansSprint)
                                                        {
                                                            String sprintChangeTo = (changeItemBean.getTo() != null) ? changeItemBean.getTo() : "";
                                                            String sprintChangeFrom = (changeItemBean.getFrom() != null) ? changeItemBean.getFrom() : "";
                                                            String difference = CommonUtils.getStringDifference(sprintChangeFrom, sprintChangeTo);
                                                            difference = difference.equals("") ? CommonUtils.getStringDifference(sprintChangeTo, sprintChangeFrom) : difference;
                                                            String sprintId = difference.trim();

                                                            for(String prevSprintId : previousSprintIdList){
                                                                Issue issueToRemove = null;
                                                                Sprint spr = GetSprintById(prevSprintId);
                                                                if (spr != null && spr.sprintId != null)
                                                                {
                                                                    if (versionSprintList.containsKey(spr))
                                                                    {
                                                                        List<Issue> issueList = versionSprintList.get(spr);
                                                                        for(Issue issue : issueList){
                                                                            if (issue.getKey() == story.getKey()){
                                                                                issueToRemove = issue;
                                                                                break;
                                                                            }
                                                                        }
                                                                    }

                                                                    if(issueToRemove != null)
                                                                        RemoveFromVersionSprintList(issueToRemove, prevSprintId);
                                                                }
                                                            }

                                                            if (sprintChangeTo.length() > sprintChangeFrom.length())
                                                            {
                                                                AddToVersionSprintList(story, sprintId);
                                                            }
                                                            else if (sprintChangeTo.length() == sprintChangeFrom.length())
                                                            {
                                                                AddToVersionSprintList(story, sprintChangeTo.trim());
                                                                RemoveFromVersionSprintList(story, sprintChangeFrom.trim());
                                                            }
                                                            else {
                                                                RemoveFromVersionSprintList(story, sprintId);
                                                            }

                                                            previousSprintIdList.add(sprintId);
                                                        }
                                                    }
                                                    else {
                                                        Object sprintObject = story.getCustomFieldValue(sprintCustomField);
                                                        if (sprintObject != null)
                                                        {
                                                            ArrayList sprList = (ArrayList) sprintObject;
                                                            if (sprList.size() > 0)
                                                            {
                                                                com.atlassian.greenhopper.service.sprint.Sprint sprintObj = (com.atlassian.greenhopper.service.sprint.Sprint) sprList.get(0);

                                                                for(String prevSprintId : previousSprintIdList){
                                                                    Issue issueToRemove = null;
                                                                    Sprint spr = GetSprintById(prevSprintId);
                                                                    if (spr != null && spr.sprintId != null)
                                                                    {
                                                                        if (versionSprintList.containsKey(spr))
                                                                        {
                                                                            List<Issue> issueList = versionSprintList.get(spr);
                                                                            for(Issue issue : issueList){
                                                                                if (issue.getKey() == story.getKey()){
                                                                                    issueToRemove = issue;
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }

                                                                        if(issueToRemove != null)
                                                                            RemoveFromVersionSprintList(issueToRemove, prevSprintId);
                                                                    }
                                                                }

                                                                AddToVersionSprintList(story, sprintObj.getId().toString());
                                                                previousSprintIdList.add(sprintObj.getId().toString());
                                                            }
                                                        }
                                                    }

                                                    if (!versionSprintList.entrySet().stream().anyMatch(x -> x.getValue().stream().anyMatch(y -> y.getKey().equals(issueKey)))
                                                            && !incompleteIssues.stream().anyMatch(x -> x.key.equals(issueKey)))
                                                    {
                                                        double storyPoint = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, story);
                                                        incompleteIssues.add(new EpicVersionIssue(issueKey, story.getSummary(), story.getIssueType().getName(), story.getStatus().getName(), storyPoint, story));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                List<Sprint> sprintListToRemove = new ArrayList<>();
                for(Map.Entry<Sprint,List<Issue>> entry : versionSprintList.entrySet())
                {
                    Sprint currentSprint = entry.getKey();
                    List<Issue> issues = entry.getValue();

                    if(issues.size() == 0){
                        sprintListToRemove.add(currentSprint);
                    }
                }

                for(Sprint sprint: sprintListToRemove){
                    versionSprintList.remove(sprint);
                }

                List<String> incompleteIssueKeyListToRemove = new ArrayList<>();
                for(EpicVersionIssue incompleteIssue : incompleteIssues){
                    String storyStatus = "UNDONE";
                    Date storyChangedDate = new Date();
                    long dayDiff = 100000000;
                    List<ChangeItemBean> changeItemBeansStatus = this.changeHistoryManager.getChangeItemsForField(incompleteIssue.issue, "status");

                    if (changeItemBeansStatus != null && changeItemBeansStatus.size() > 0)
                    {
                        for (ChangeItemBean changeItemBean : changeItemBeansStatus)
                        {
                            storyChangedDate = new Date(changeItemBean.getCreated().getTime());
                            if(CommonUtils.isIssueInDoneState(changeItemBean.getToString())){
                                storyStatus = "DONE";
                            }
                        }

                        if(storyStatus.equals("DONE")){
                            String sprintId = "";
                            EpicVersionIssue versionIssueToAdd = new EpicVersionIssue();
                            for(Map.Entry<Sprint,List<Issue>> entry : versionSprintList.entrySet())
                            {
                                Sprint currentSprint = entry.getKey();
                                Date sprintStartDate = currentSprint.startDate;

                                long currDayDiff = Math.abs(CommonUtils.getDifferenceDays(storyChangedDate, sprintStartDate));
                                if (currDayDiff < dayDiff && sprintStartDate.before(storyChangedDate))
                                {
                                    dayDiff = currDayDiff;
                                    versionIssueToAdd = incompleteIssue;
                                    sprintId = currentSprint.getSprintId().toString();
                                }
                            }

                            boolean isExistInPrevSprint = false;
                            if (versionIssueToAdd != null && versionIssueToAdd.issue != null){
                                for(String prevSprintId : previousSprintIdList){
                                    Sprint spr = GetSprintById(prevSprintId);
                                    if (spr != null && spr.sprintId != null)
                                    {
                                        if (versionSprintList.containsKey(spr))
                                        {
                                            List<Issue> issueList = versionSprintList.get(spr);
                                            for(Issue issue : issueList){
                                                if (issue.getKey() == versionIssueToAdd.issue.getKey()){
                                                    isExistInPrevSprint = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if(!isExistInPrevSprint && versionIssueToAdd != null && versionIssueToAdd.issue != null && sprintId != "" && !incompleteIssueKeyListToRemove.contains(versionIssueToAdd.key)){
                                AddToVersionSprintList(versionIssueToAdd.issue, sprintId);
                                incompleteIssueKeyListToRemove.add(versionIssueToAdd.key);
                            }
                        }
                    }
                }

                for(String incompleteIssueKey: incompleteIssueKeyListToRemove){
                    incompleteIssues.removeIf(x -> x.key == incompleteIssueKey);
                }

                List<String> versionStoryList = new ArrayList<>();
                Map<Sprint, List<Issue>> completedStoryList = new LinkedHashMap<>();
                double currentStoryPoints = 0, lastThreeCompletedWorkPoints = 0;
                double totalSprintCount = versionSprintList.size();
                double curSprintCount = totalSprintCount;
                boolean isFirstSprint = true;

                for(Map.Entry<Sprint,List<Issue>> entry : versionSprintList.entrySet())
                {
                    Sprint curSprint = entry.getKey();
                    List<Issue> issues = entry.getValue();
                    double addedStoryPoints = 0, removedStoryPoints = 0;

                    for (Issue issue : issues)
                    {
                        String issueKey = issue.getKey();
                        double storyPoint = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, issue, curSprint.endDate);

                        if (!versionStoryList.contains(issueKey))
                        {
                            versionStoryList.add(issueKey);
                            addedStoryPoints += storyPoint;
                        }

                        boolean isCompleted = false;
                        List<ChangeItemBean> changeItemBeansStatus = this.changeHistoryManager.getChangeItemsForField(issue, "status");
                        if (changeItemBeansStatus != null && changeItemBeansStatus.size() > 0) {
                            for (ChangeItemBean changeItemBean : changeItemBeansStatus) {
                                if (CommonUtils.isIssueInDoneState(changeItemBean.getToString())) {
                                    isCompleted = true;
                                }
                            }
                        }

                        if (isCompleted)
                        {
                            removedStoryPoints += storyPoint;
                            currentStoryPoints -= storyPoint;

                            if (completedStoryList.containsKey(curSprint))
                                completedStoryList.get(curSprint).add(issue);
                            else
                            {
                                List<Issue> newIssueList = new ArrayList<>();
                                newIssueList.add(issue);
                                completedStoryList.put(curSprint, newIssueList);
                            }

                            incompleteIssues.removeIf(x -> x.key == issueKey);
                        }
                        else
                        {
                            if (!completedStoryList.entrySet().stream().anyMatch(x -> x.getValue().stream().anyMatch(y -> y.getKey().equals(issueKey)))
                                    && !incompleteIssues.stream().anyMatch(x -> x.key.equals(issueKey)))
                            {
                                incompleteIssues.add(new EpicVersionIssue(issueKey, issue.getSummary(), issue.getIssueType().getName(), issue.getStatus().getName(), storyPoint, issue));
                            }
                        }
                    }

                    double curStoryPoints = 0;

                    if (isFirstSprint)
                        isFirstSprint = false;
                    else
                        curStoryPoints = currentStoryPoints > 0 ? currentStoryPoints : 0;

                    AddToCurrentCategorySet(curStoryPoints, addedStoryPoints, removedStoryPoints, curSprint.sprintName);
                    currentStoryPoints += addedStoryPoints;

                    //Predicted sprints are calculated based on your team's velocity* (amount of work completed in the last three sprints), and the total work remaining for the epic.
                    // Scope change is not considered when calculating the velocity*, but is included in the total work remaining.
                    if (curSprintCount < 4)
                        lastThreeCompletedWorkPoints += removedStoryPoints;

                    curSprintCount--;
                }

                double remainingSprintCount = 0, avgCompleted = 0;

                if (lastThreeCompletedWorkPoints != 0)
                {
                    avgCompleted = Math.ceil(lastThreeCompletedWorkPoints / 3);
                    remainingSprintCount = Math.ceil(currentStoryPoints / avgCompleted);
                }

                if (remainingSprintCount != 0 && totalSprintCount > 3)
                    AddToEstimatedCategorySet(currentStoryPoints, avgCompleted, remainingSprintCount);

                // Get Completed Issues By Sprint
                List<EpicVersionSprintList> versionSprintList = new ArrayList<>();
                List<Issue> uniqueIssueList = new ArrayList<>();

                for(Map.Entry<Sprint,List<Issue>> completedIssues : completedStoryList.entrySet())
                {
                    EpicVersionSprintList versionSprint = new EpicVersionSprintList();
                    Sprint spr = completedIssues.getKey();
                    List<Issue> issues = completedIssues.getValue();

                    versionSprint.sprintName = CommonUtils.getSprintUrl(spr.sprintId, spr.sprintName);
                    versionSprint.sprintStartDate = spr.startDate;
                    versionSprint.sprintEndDate = spr.endDate;
                    versionSprint.sprintDates = DateFormatUtils.format(spr.startDate, "MMMM dd, yyyy") + " - " + DateFormatUtils.format(spr.endDate, "MMMM dd, yyyy");
                    List<EpicVersionIssue> versionIssues = new ArrayList<>();

                    double totalStoryPoints = 0;

                    for (Issue iss : issues)
                    {
                        EpicVersionIssue versionIssue = new EpicVersionIssue();
                        versionIssue.key = CommonUtils.getIssueUrl(iss.getKey());
                        versionIssue.summary = iss.getSummary();
                        versionIssue.issueType = iss.getIssueType().getName();
                        versionIssue.status = iss.getStatus().getName();
                        double storyPoints = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, iss, spr.endDate);
                        versionIssue.storyPoints = storyPoints;
                        if (!uniqueIssueList.contains(iss))
                        {
                            if (storyPoints == 0)
                                unEstimatedIssues++;
                            else
                                estimatedIssues++;

                            completedStoryPoints += storyPoints;
                            uniqueIssueList.add(iss);
                        }
                        totalStoryPoints += storyPoints;

                        versionIssues.add(versionIssue);
                    }

                    versionSprint.sprintIssues = versionIssues;
                    versionSprint.totalStoryPoints = totalStoryPoints;

                    versionSprintList.add(versionSprint);
                }

                dataset.completedIssues = versionSprintList;
                for (EpicVersionIssue incompleteIssue : incompleteIssues)
                {
                    incompleteIssue.key = CommonUtils.getIssueUrl(incompleteIssue.key);
                    if (incompleteIssue.storyPoints == 0)
                        unEstimatedIssues++;
                    else
                        estimatedIssues++;
                }

                dataset.inCompletedIssues = incompleteIssues;
                dataset.inCompletedIssuesStoryPoints = incompleteIssues.stream().mapToDouble(x -> x.storyPoints).sum();
                strCompletedStoryPoints = (int)completedStoryPoints + " of " + (int)(dataset.inCompletedIssuesStoryPoints + completedStoryPoints);
                int totalIssues = unEstimatedIssues + estimatedIssues;
                if (totalIssues > 0)
                    unestimatedPercentage = (unEstimatedIssues * 100) / totalIssues;
            }

            ChartHelper chartHelper = (new ReleaseBurndownChartGenerator(versionCategoryDataset)).generateChart();

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
            params.put("dataset", dataset);
            params.put("versionName", versionName);
            params.put("versionStartDate", simpleDateFormat.format(versionStartDate));
            params.put("versionReleaseDate", simpleDateFormat.format(versionReleaseDate));
            params.put("unestimatedPercentage", (int)unestimatedPercentage);
            params.put("completedStoryPoints", strCompletedStoryPoints);

            if (inline) {
                String base64Image = ((ChartUtils) ComponentAccessor.getComponent(ChartUtils.class)).renderBase64Chart(chartHelper.getImage(), "Requirements Growth Chart");
                params.put("base64Image", base64Image);
            }

            return new Chart(chartHelper.getLocation(), chartHelper.getImageMap(), chartHelper.getImageMapName(), params);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private void AddToVersionSprintList(Issue story, String sprintId)
    {
        List<Issue> issueList = new ArrayList<>();
        Sprint spr = GetSprintById(sprintId);

        if (spr != null && spr.sprintId != null)
        {
            if (versionSprintList.containsKey(spr))
                issueList = versionSprintList.get(spr);

            if(!issueList.stream().anyMatch(x -> x.getKey().equals(story.getKey()))){
                issueList.add(story);
                versionSprintList.put(spr, issueList);
            }
        }
        else if(!incompleteIssues.stream().anyMatch(x -> x.key.equals(story.getKey())))
        {
            double storyPoint = CommonUtils.getStoryPointByDate(customFieldManager, changeHistoryManager, story);
            incompleteIssues.add(new EpicVersionIssue(story.getKey(), story.getSummary(), story.getIssueType().getName(), story.getStatus().getName(), storyPoint, story));
        }
    }

    private void RemoveFromVersionSprintList(Issue story, String sprintId)
    {
        Sprint sprint = GetSprintById(sprintId);

        if (sprint != null && sprint.sprintId != null)
        {
            if (versionSprintList.containsKey(sprint)) {
                List<Issue> issueList = versionSprintList.get(sprint);
                issueList.remove(story);
                versionSprintList.put(sprint, issueList);
            }
        }
    }

    private Sprint GetSprintById(String sprId)
    {
        Sprint sprint;

        if (sprintList.containsKey(sprId))
            sprint = sprintList.get(sprId);
        else
        {
            sprint = CommonUtils.getSprintById(sprId);
            if (sprint != null && sprint.sprintId != "")
                sprintList.put(sprId, sprint);
        }

        return sprint;
    }

    private void AddToCurrentCategorySet(double curStoryPoints, double addedStoryPoints, double removedStoryPoints, String sprintName)
    {
        versionCategoryDataset.addValue(addedStoryPoints, "Work added", sprintName);
        versionCategoryDataset.addValue(curStoryPoints, "Work remaining", sprintName);
        versionCategoryDataset.addValue(removedStoryPoints, "Work completed", sprintName);
        versionCategoryDataset.addValue(0, "Work forecast remaining", sprintName);
        versionCategoryDataset.addValue(0, "Work forecast completed", sprintName);
    }

    private void AddToEstimatedCategorySet(double curStoryPoints, double velocityStoryPoints, double remainingSprintCount)
    {
        double remainValue = curStoryPoints - velocityStoryPoints;

        for (int i = 1; i <= remainingSprintCount; i++)
        {
            String sprintName = String.format("Forecast Sprint %s", i);

            versionCategoryDataset.addValue(0, "Work added", sprintName);
            versionCategoryDataset.addValue(remainValue, "Work forecast remaining", sprintName);
            versionCategoryDataset.addValue(0, "Work remaining", sprintName);
            versionCategoryDataset.addValue(0, "Work completed", sprintName);
            versionCategoryDataset.addValue(velocityStoryPoints, "Work forecast completed", sprintName);

            remainValue -= velocityStoryPoints;

            if (remainValue < 0)
            {
                velocityStoryPoints += remainValue;
                remainValue = 0;
            }
        }
    }
}
*/