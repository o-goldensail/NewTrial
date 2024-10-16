package jira.phinia.tciz.utils;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.workflow.WorkflowActionsBean;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.query.Query;
import org.ofbiz.core.entity.comparator.OFBizDateComparator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    static final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Get formatted date.
     *
     * @param date: Date
     * @return Date in format of "yyyy-MM-dd": Date
     */
    public static Date formatDate(Date date) {
        try {
            return formatter.parse(formatter.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get formatted date as string.
     *
     * @param date: Date
     * @return Date in format of "yyyy-MM-dd": String
     */
    public static String formatDateToString(Date date) {
        return formatter.format(date);
    }

    /**
     * @return Server base url.
     */
    public static String getCurrentBaseURL() {
        return ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
    }

    /**
     * @return Server version: String
     */
    public static String getJiraVersion() {
        return ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_VERSION);
    }

    /**
     * Get current project.
     *
     * @return Project
     */
    public static Project getCurrentProject() {
        UserProjectHistoryManager userProjectHistoryManager = ComponentAccessor.getComponentOfType(UserProjectHistoryManager.class);
        return userProjectHistoryManager.getCurrentProject(10, getCurrentUser());
    }

    /**
     * Get current user.
     *
     * @return ApplicationUser
     */
    public static ApplicationUser getCurrentUser() {
        JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();

        return authenticationContext.getLoggedInUser();
    }

    /**
     * Returns project by given name
     *
     * @param projectName: String
     * @return Project
     */
    public static Project getProjectByName(String projectName) {
        return ComponentAccessor.getProjectManager().getProjectObjByName(projectName);
    }

    /**
     * Returns project by given id
     *
     * @param projectId: Long
     * @return Project
     */
    public static Project getProjectById(Long projectId) {
        return ComponentAccessor.getProjectManager().getProjectObj(projectId);
    }

    /**
     * Returns release (version) by given id
     *
     * @param releaseId: Long
     * @return Project
     */
    public static Version getRelease(long releaseId) {
        return ComponentAccessor.getVersionManager().getVersion(releaseId);
    }

    /**
     * Returns release (version) by given name in given project.
     *
     * @param projectId   project id: Long
     * @param versionName version name: String
     * @return Version
     */
    public static Version getProjectVersionByName(long projectId, String versionName) {
        VersionManager versionManager = ComponentAccessor.getVersionManager();
        return versionManager.getVersion(projectId, versionName);
    }

    /**
     * Get issues in given release.
     *
     * @param version: Version
     * @return Collection of issues in given version.
     */
    public static Collection<Issue> getIssuesInRelease(Version version) {
        Date lastDate = version.getReleaseDate() != null ? version.getReleaseDate() : new Date();
        lastDate.setTime(lastDate.getTime()+8600000);
        return ComponentAccessor.getVersionManager().getIssuesWithFixVersion(version).stream().filter(issue -> issue.getCreated().before(lastDate)).collect(Collectors.toList());
    }

    /**
     * @param issue: Issue
     * @return boolean: true if issue is resolved, otherwise false.
     */
    public static boolean isIssueResolved(Issue issue) {
        return issue.getResolution() != null;
    }

    /**
     * Get issue's resolution date if issue is resolved between given dates.
     *
     * @param issue:     Issue
     * @param startDate: Date
     * @param endDate:   Date
     * @return issue resolution date: Date
     */
    public static Date issueResolutionDateIfResolvedBetweenDates(Issue issue, Date startDate, Date endDate) {
        if (!isIssueResolved(issue))
            return null;

        Date resolutionDate = issue.getResolutionDate();
        if (resolutionDate != null) {
            if (startDate.before(resolutionDate) && endDate.after(resolutionDate))
                return formatDate(resolutionDate);
        }

        return null;
    }

    /**
     * Get issue creation date if created between given dates
     *
     * @param issue:     Issue
     * @param startDate: Date
     * @param endDate:   Date
     * @return formatted date if true, null otherwise.
     */
    public static Date issueCreatedDateIfCreatedBetweenDates(Issue issue, Date startDate, Date endDate) {
        Date createdDate = issue.getCreated();
        if (createdDate != null) {
            if (startDate.before(createdDate) && endDate.after(createdDate))
                return formatDate(createdDate);
        }
        return null;
    }

    /**
     * Query issues with given query.
     *
     * @param issueQuery: Query
     * @return Search result.
     */
    public static SearchResults queryIssues(Query issueQuery) {
        Logging.log(String.format("Query: %s", issueQuery.getQueryString()));
        SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
        try {
            return searchService.search(getCurrentUser(), issueQuery, PagerFilter.getUnlimitedFilter());
        } catch (SearchException e) {
            Logging.log(e.getStackTrace()[0].getMethodName());
            Logging.log(e.getMessage());
        }
        return null;
    }

    /**
     * Query issues with given query string.
     *
     * @param jqlQuery: String
     * @return Search result.
     */
    public static SearchResults queryIssues(String jqlQuery) {
        Logging.log(String.format("Query: %s", jqlQuery));
        SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
        SearchService.ParseResult parsedResult = searchService.parseQuery(getCurrentUser(), jqlQuery);
        if (parsedResult.isValid()) {
            Query issueQuery = parsedResult.getQuery();
            try {
                return searchService.search(getCurrentUser(), issueQuery, PagerFilter.getUnlimitedFilter());
            } catch (SearchException e) {
                Logging.log(e.getStackTrace()[0].getMethodName());
                Logging.log(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Get issue list by search result.
     *
     * @param searchResults: SearchResults
     * @return List of issues.
     */
    public static List<Issue> getIssueListBySearchResult(SearchResults searchResults) {
        List<Issue> issueList = new ArrayList<>();

        try {
            Method newGetMethod = null;

            try {
                newGetMethod = SearchResults.class.getMethod("getIssues");
            } catch (NoSuchMethodException e) {
                try {
                    newGetMethod = SearchResults.class.getMethod("getResults");
                } catch (NoSuchMethodError e2) {
                    e2.printStackTrace();
                }
            }

            if (newGetMethod != null) {
                issueList = (List<Issue>) newGetMethod.invoke(searchResults);
            }
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return issueList;
    }

    /**
     * Get list issues of jql query
     *
     * @param jqlQuery: String
     * @return List of Issues
     */
    public static List<Issue> getIssueByQuery(String jqlQuery) {
        SearchResults searchResults = queryIssues(jqlQuery);
        return getIssueListBySearchResult(searchResults);
    }

    /**
     * Add day to given date and set time to end of the day: 23:59:59.999
     *
     * @param date: Date
     * @param day:  int (+/-)
     * @return Date with added days.
     */
    public static Date addDaysTillEndOfDay(Date date, int day) {
        Date ret;

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, day);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        ret = c.getTime();

        return ret;
    }

    /**
     * Get issue list by given filter id
     *
     * @param filterId: Long
     * @return List of issues
     * @throws SearchException: Exception
     */
    public static List<Issue> getIssuesByFilterId(long filterId) throws SearchException {
        SearchRequestManager searchRequestManager = ComponentAccessor.getComponentOfType(SearchRequestManager.class);
        SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
        SearchRequest searchRequest = searchRequestManager.getSearchRequestById(filterId);
        SearchResults searchResults = searchService.search(getCurrentUser(), searchRequest.getQuery(), PagerFilter.getUnlimitedFilter());
        return getIssueListBySearchResult(searchResults);
    }

    /**
     * Get object as comma separated string.
     * If object string, it returns value.
     * If object is null, returns empty string.
     *
     * @param obj Object
     * @return String
     */
    public static String getObjectByCommaSeparatedString(Object obj) {
        String value = "";
        if (obj instanceof String[]) {
            String[] x = (String[]) obj;
            value = String.join(",", x);
        } else if (obj instanceof String) {
            value = obj.toString();
        }
        return value;
    }

    public static String getIssueInitialStatus(Issue issue) {
        WorkflowManager wfManager = ComponentAccessor.getWorkflowManager();
        return wfManager.getWorkflow(issue).getLinkedStatusObjects().get(0).getSimpleStatus().getName();
    }

    public static String getFieldValueByDate(Issue issue, String fieldName, Date date) {
        ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
        List<ChangeItemBean> changeItemBeans = changeHistoryManager.getChangeItemsForField(issue, fieldName);
        List<ChangeItemBean> changeItemBeansForFieldAtDate = changeItemBeans.stream().filter(bean -> formatDateToString(bean.getCreated()).equalsIgnoreCase(formatDateToString(date)) && bean.getField().equalsIgnoreCase(fieldName)).collect(Collectors.toList());

        if (changeItemBeansForFieldAtDate.size() == 0) {
            return null;
        }
        ChangeItemBean bean = changeItemBeansForFieldAtDate.get(changeItemBeansForFieldAtDate.size() - 1);
        return bean.getToString();
    }

    public static String getStatusAtDate(Issue issue, Date date) {
        ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
        List<ChangeItemBean> changeItemBeans = changeHistoryManager.getChangeItemsForField(issue, "status").stream().filter(bean -> bean.getCreated().before(date)).collect(Collectors.toList());
        if (changeItemBeans.size() == 0)
            return getIssueInitialStatus(issue);

        return changeItemBeans.get(changeItemBeans.size() - 1).getToString();
    }

}