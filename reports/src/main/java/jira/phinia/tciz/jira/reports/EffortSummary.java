package jira.phinia.tciz.jira.reports;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.ProjectActionSupport;
import jira.phinia.tciz.utils.Logging;

import java.util.*;
import java.util.stream.Collectors;

import static jira.phinia.tciz.utils.Utils.getIssuesByFilterId;

public class EffortSummary extends AbstractReport {

    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
    private final IssueManager issueManager = ComponentAccessor.getIssueManager();
    private final IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();

    public String generateReportHtml(ProjectActionSupport projectActionSupport, Map map) throws Exception {

        String filterId = (String) map.get("filterid");
        long filteridlongId = Long.parseLong(filterId);
        Logging.log(String.format("Filter Id: %s", filterId));

        List<Issue> mainIssues = getIssuesByFilterId(filteridlongId);
        Logging.log(String.format("Number of Main Issues: %s", mainIssues.size()));

        Map<String, Collection<Issue>> mainToLinkedIssueMap = retrieveLinkedIssuesRecursively(mainIssues);

        HashMap<String, Integer> countryFieldSums = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> countryActivitySums = new HashMap<>();
        HashMap<String, Map<String, Map<String, Integer>>> countryActivityEffortSums = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> fieldSiteEfforts = new HashMap<>();

        List<String> fields = new ArrayList<>();
        fields.add("Resolution Effort (hrs)");
        fields.add("Review Effort (hrs)");
        fields.add("Rework Effort (hrs)");

        Map<String, CustomField> effortFields = new HashMap<>();
        for (String field : fields) {
            List<CustomField> customFieldsByName = (List<CustomField>) customFieldManager.getCustomFieldObjectsByName(field);
            if (customFieldsByName.size() > 0) {
                effortFields.put(field, customFieldsByName.get(0));
            }
        }

        CustomField activityField = customFieldManager.getCustomFieldObjectByName("Activity Field");
        if (activityField == null || effortFields.values().size() == 0) {
            Logging.log("One or more Custom Fields not found!");
            return null;
        }

        processIssues(mainIssues, mainToLinkedIssueMap, effortFields, activityField, countryFieldSums, countryActivitySums, countryActivityEffortSums, fieldSiteEfforts);

        HashMap<String, Object> params = new HashMap<>();
        params.put("filterid", filteridlongId);
        params.put("countryFieldSums", countryFieldSums);
        params.put("countryActivitySums", countryActivitySums);
        params.put("countryActivityEffortSums", countryActivityEffortSums);
        params.put("fieldSiteEfforts", fieldSiteEfforts);
        return descriptor.getHtml("view", params);
    }

    private Map<String, Collection<Issue>> retrieveLinkedIssuesRecursively(Collection<Issue> issues) {
        Map<String, Collection<Issue>> mainToLinkedIssueMap = new HashMap<>();
        Set<Issue> visitedIssues = new HashSet<>();

        for (Issue mainIssue : issues) {
            Logging.log("Processing Main Issue: " + mainIssue.getKey());
            Collection<Issue> linkedIssues = getAllLinkedIssues(mainIssue, visitedIssues);

            if (!linkedIssues.isEmpty()) {
                mainToLinkedIssueMap.put(mainIssue.getKey(), linkedIssues);
                Logging.log("Found " + linkedIssues.size() + " linked issues for Main Issue: " + mainIssue.getKey());
            } else {
                Logging.log("No linked issues found for Main Issue: " + mainIssue.getKey());
            }
        }
        return mainToLinkedIssueMap;
    }

    private Collection<Issue> getAllLinkedIssues(Issue issue, Set<Issue> visitedIssues) {
        if (visitedIssues.contains(issue)) {
            return Collections.emptyList();
        }
        visitedIssues.add(issue);

        Collection<Issue> outwardLinkedIssues = issueLinkManager.getOutwardLinks(issue.getId()).stream()
                .map(IssueLink::getDestinationObject)
                .collect(Collectors.toList());

        Collection<Issue> inwardLinkedIssues = issueLinkManager.getInwardLinks(issue.getId()).stream()
                .map(IssueLink::getSourceObject)
                .collect(Collectors.toList());

        Collection<Issue> allLinkedIssues = new HashSet<>(outwardLinkedIssues);
        allLinkedIssues.addAll(inwardLinkedIssues);

        for (Issue linkedIssue : new ArrayList<>(allLinkedIssues)) {
            allLinkedIssues.addAll(getAllLinkedIssues(linkedIssue, visitedIssues));
        }

        return allLinkedIssues;
    }

    private void processIssues(Collection<Issue> mainIssues,
                               Map<String, Collection<Issue>> mainToLinkedIssueMap,
                               Map<String, CustomField> effortFields,
                               CustomField activityField,
                               Map<String, Integer> countryFieldSums,
                               Map<String, HashMap<String, Integer>> countryActivitySums,
                               Map<String, Map<String, Map<String, Integer>>> countryActivityEffortSums,
                               Map<String, HashMap<String, Integer>> fieldSiteEfforts
    ) {
        List<Issue> allIssues = new ArrayList<>();
        for (Issue mainIssue : mainIssues) {
            Collection<Issue> linkedIssues = mainToLinkedIssueMap.get(mainIssue.getKey());
            allIssues.addAll(linkedIssues);
            if (!allIssues.contains(mainIssue)) {
                allIssues.add(mainIssue);
            }
        }

        for (Issue issue : allIssues) {
            String country = "Unknown Site";
            ApplicationUser assignee = issue.getAssignee();
            if (assignee != null) {
                String username = assignee.getDisplayName();
                country = extractCountryFromUsername(username);
            }
            String activity = issue.getCustomFieldValue(activityField) != null
                    ? issue.getCustomFieldValue(activityField).toString()
                    : "Unknown Activity";

            for (Map.Entry<String, CustomField> effortEntry : effortFields.entrySet()) {
                String effortType = effortEntry.getKey();
                CustomField effortField = effortEntry.getValue();

                String effortStr = issue.getCustomFieldValue(effortField) != null ?
                        !issue.getCustomFieldValue(effortField).toString().equals("") ?
                                issue.getCustomFieldValue(effortField).toString() : "0"
                        : "0";
                int effort = Integer.parseInt(effortStr);

                Logging.log(String.format("%s for %s: %d", effortType, country, effort));

                countryFieldSums.merge(country, effort, Integer::sum);

                countryActivitySums
                        .computeIfAbsent(country, k -> new HashMap<>())
                        .merge(activity, 1, Integer::sum);

                countryActivityEffortSums
                        .computeIfAbsent(effortType, k -> new HashMap<>())
                        .computeIfAbsent(activity, t -> new HashMap<>())
                        .merge(country, effort, Integer::sum);

                fieldSiteEfforts
                        .computeIfAbsent(effortType, k -> new HashMap<>())
                        .merge(country, effort, Integer::sum);
            }
        }
    }

    private String extractCountryFromUsername(String username) {
        Logging.log("Extracting country from username: " + username);

        try {
            int start = username.lastIndexOf('(');
            int end = username.lastIndexOf(')');

            if (start != -1 && end != -1 && start < end) {
                String country = username.substring(start + 1, end);
                Logging.log("Country found: " + country);
                return country;
            } else {
                Logging.log("Country not found, using 'Unknown Site'.");
                return "Unknown Site";
            }
        } catch (Exception e) {
            Logging.log("Error extracting country from username: " + username);
            return "Unknown Country";
        }
    }
}
