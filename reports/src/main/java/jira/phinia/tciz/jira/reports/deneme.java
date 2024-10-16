/*
package com.atlassian.plugins.tutorial.jira.customcharts;

import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.*;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.plugins.tutorial.jira.classes.FtqReportTable;
import com.atlassian.plugins.tutorial.jira.reportutils.CommonUtils;
import com.atlassian.plugins.tutorial.jira.reportutils.TableGenerator;

import java.util.*;
import java.time.*;
import  java.time.format.DateTimeFormatter;

public class FtqReport {

    public FtqReport() {

    }

    public TableGenerator generateChart(String startDate, String endDate) {
        return this.generateChartInternal(startDate, endDate);
    }

    private TableGenerator generateChartInternal(final String startDate,
                                        final String endDate) {
        try
        {
            SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
            SearchResults searchResults = null;
            SearchRequestManager srManager = ComponentAccessor.getComponentOfType(SearchRequestManager.class);
            ProjectManager projectManager = ComponentAccessor.getComponentOfType(ProjectManager.class);
            PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
            HashMap params = new HashMap();
            String reportStartDate = startDate.substring(6) + startDate.substring(2,6) + startDate.substring(0, 2);
            String reportEndDate = endDate.substring(6) + endDate.substring(2,6) + endDate.substring(0, 2);

            CustomFieldManager customFieldManager = (CustomFieldManager) ComponentAccessor.getComponent(CustomFieldManager.class);
            CustomField customFieldActivity = customFieldManager.getCustomFieldObjectByName("Activity");
            CustomField customFieldReviewCount = customFieldManager.getCustomFieldObjectByName("Review Count");
            CustomField customFieldVvResult = customFieldManager.getCustomFieldObjectByName("VV Result");

            List<FtqReportTable> ftqDataTable = new ArrayList<>();

            List<FtqReportTable> ftqCriticalCrDataTable = ftqDataTable; //// needed a second datatable with project names for critical CR's
            // -------
            SearchRequest ftqReportFilter = srManager.getSearchRequestById((long)35855);
            String filterQuery = ftqReportFilter.getQuery().getQueryString();

            String parsedFilter = filterQuery.split("project in ")[1].replace("(", "").replace(")", "");
            String[] prjInQuery = parsedFilter.split(",");

            List<String> prjKeysList = new ArrayList<>();
            for(String prjKey : prjInQuery){
                FtqReportTable ftqReportTable = new FtqReportTable();
                ftqReportTable.projectName = projectManager.getProjectObjByKey(prjKey.trim()).getName();
                ftqDataTable.add(ftqReportTable);

                prjKeysList.add(prjKey.trim());
            }
            // ----

            //String jqlStatChgQueryIssues = "project in (CQY, CRA, TRX, TQZ, TPG, TSC) AND type = CR AND Activity in (\"Software\", \"Software Specification\", \"SYS Func Test Spec\", \"SYS Func Test Exec\") AND status changed TO Closed during (\"" + reportStartDate + "\", \"" + reportEndDate + "\")";
            String jqlStatChgQueryIssues = filterQuery + " AND type = CR AND Activity in (Software, \"Software Specification\", \"SYS Func Test Spec\", \"SYS Func Test Exec\") AND status changed TO Closed during (\"" + reportStartDate + "\", \"" + reportEndDate + "\")";
            SearchService.ParseResult parseResIssues = searchService.parseQuery(CommonUtils.getCurrentLoggedUser(), jqlStatChgQueryIssues);
            if (parseResIssues.isValid())
            {
                Query buildIssuesQuery = parseResIssues.getQuery();
                searchResults = searchService.search(CommonUtils.getCurrentLoggedUser(), buildIssuesQuery, pagerFilter);
                List<Issue> issueList = CommonUtils.getStoriesByProjectUpgradeCompatible(searchResults);

                for(Issue issue : issueList)
                {
                    String issueProjectKey = issue.getKey().split("-")[0].trim();
                    int tableIndex = (prjKeysList.contains(issueProjectKey)) ? prjKeysList.indexOf(issueProjectKey):0;

                    double reviewCountField = issue.getCustomFieldValue(customFieldReviewCount) != null ? (double) issue.getCustomFieldValue(customFieldReviewCount) : 0;
                    String activityField = issue.getCustomFieldValue(customFieldActivity) != null ? issue.getCustomFieldValue(customFieldActivity).toString() : "";
                    FtqReportTable ftqTempRow;

                    if (activityField.equalsIgnoreCase("Software"))
                    {
                        if (prjKeysList.contains(issueProjectKey)){
                            ftqTempRow = ftqDataTable.get(tableIndex);
                            ftqTempRow.softwareTotal++;
                            if (reviewCountField == 1)
                                ftqTempRow.softwareFirstTimePass++;
                            ftqDataTable.set(tableIndex, ftqTempRow);
                        }

                    } else if (activityField.equalsIgnoreCase("Software Specification"))
                    {
                        if (prjKeysList.contains(issueProjectKey)){
                            ftqTempRow = ftqDataTable.get(tableIndex);
                            ftqTempRow.softwareSpecificationTotal++;
                            if (reviewCountField == 1)
                                ftqTempRow.softwareSpecificationFirstTimePass++;
                            ftqDataTable.set(tableIndex, ftqTempRow);
                        }

                    } else if (activityField.equalsIgnoreCase("SYS Func Test Spec"))
                    {
                        if (prjKeysList.contains(issueProjectKey)){
                            ftqTempRow = ftqDataTable.get(tableIndex);
                            ftqTempRow.functionalTestSpecification++;
                            if (reviewCountField == 1)
                                ftqTempRow.functionalTestSpecificationFirstTimePass++;
                            ftqDataTable.set(tableIndex, ftqTempRow);
                        }

                    } else if (activityField.equalsIgnoreCase("SYS Func Test Exec"))
                    {
                        String vvResultField = issue.getCustomFieldValue(customFieldVvResult) != null ? issue.getCustomFieldValue(customFieldVvResult).toString() : "";
                        boolean passFlag = vvResultField.equalsIgnoreCase("Pass") || vvResultField.equalsIgnoreCase("Warn");
                        boolean failFlag = vvResultField.equalsIgnoreCase("Fail C") || vvResultField.equalsIgnoreCase("Fail B") || vvResultField.equalsIgnoreCase("Fail A");

                        if (prjKeysList.contains(issueProjectKey)){
                            ftqTempRow = ftqDataTable.get(tableIndex);
                            ftqTempRow.functionalTestExecution++;
                            if (reviewCountField == 1)
                                ftqTempRow.functionalTestExecutionFirstTimePass++;
                            if (passFlag)
                                ftqTempRow.functionalTestPass++;
                            else if (failFlag)
                                ftqTempRow.functionalTestFail++;
                            ftqDataTable.set(tableIndex, ftqTempRow);
                        }

                    }
                }
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH);
            LocalDate unclosedIssueDate = LocalDate.parse(reportEndDate, formatter).minusDays(91); // unclosed issues should be created before this date
            String unclosedIssueDateStr = unclosedIssueDate.toString();

            String jqlStatCriticalIssues = filterQuery + " AND type = CR AND status not in (Closed, Rejected) and Severity in (1-Critical, Critical) and created <= \"" + unclosedIssueDateStr + "\"";
            SearchService.ParseResult parseResCriticalIssues = searchService.parseQuery(CommonUtils.getCurrentLoggedUser(), jqlStatCriticalIssues);
            if (parseResCriticalIssues.isValid())
            {
                Query buildCriticalIssuesQuery = parseResCriticalIssues.getQuery();
                searchResults = searchService.search(CommonUtils.getCurrentLoggedUser(), buildCriticalIssuesQuery, pagerFilter);
                List<Issue> criticalIssueList = CommonUtils.getStoriesByProjectUpgradeCompatible(searchResults);

                for(Issue issue : criticalIssueList)
                {
                    String issueProjectKey = issue.getKey().split("-")[0].trim();
                    int tableIndex = (prjKeysList.contains(issueProjectKey)) ? prjKeysList.indexOf(issueProjectKey):0;

                    FtqReportTable ftqTempRow;
                    String activityStr = issue.getCustomFieldValue(customFieldActivity) != null ? issue.getCustomFieldValue(customFieldActivity).toString() : "";
                    boolean activityFlag = activityStr.equalsIgnoreCase("MUT") || activityStr.equalsIgnoreCase("PolySpace only") || activityStr.equalsIgnoreCase("RTRT only") || activityStr.equalsIgnoreCase("S-Function only");

                    if (prjKeysList.contains(issueProjectKey)){
                        ftqTempRow = ftqCriticalCrDataTable.get(tableIndex);
                        ftqTempRow.criticalIssueTotal++;
                        if (activityFlag)
                            ftqTempRow.criticalIssueUt++;
                        ftqDataTable.set(tableIndex, ftqTempRow);
                    }
                }
            }

            params.put("ftqDataSet", ftqDataTable);
            params.put("ftqDataSetSec", ftqCriticalCrDataTable);

            return new TableGenerator(params);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage() + ex.getStackTrace());
        }
    }

}


 */