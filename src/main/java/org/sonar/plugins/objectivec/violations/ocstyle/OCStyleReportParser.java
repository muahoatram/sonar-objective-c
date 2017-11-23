/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 OCTO Technology, Backelite
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.objectivec.violations.ocstyle;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by Danh Hung on 11/23/17.
 */
public class OCStyleReportParser {
    private final Project project;
    private final SensorContext context;
    private final ResourcePerspectives resourcePerspectives;

    private static final Logger LOGGER = LoggerFactory.getLogger(OCStyleReportParser.class);

    public OCStyleReportParser(final Project project, final SensorContext context, final ResourcePerspectives resourcePerspectives) {
        this.project = project;
        this.context = context;
        this.resourcePerspectives = resourcePerspectives;
    }

    public void parseReport(File reportFile) {
        try {
            FileReader fileReader = new FileReader(reportFile);
            Object reportObj = JSONValue.parse(fileReader);
            IOUtils.closeQuietly(fileReader);

            if (reportObj != null) {
                JSONObject reportJson = (JSONObject)reportObj;
                JSONArray violations = (JSONArray)reportJson.get("violations");
                for (Object obj : violations) {
                    recordIssue((JSONObject) obj);
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to parse OCStyle report file", e);
        }
    }

    private void recordIssue(final JSONObject violation) {
        String filePath = (String)violation.get("file");
        if (filePath != null) {
            org.sonar.api.resources.File resource = org.sonar.api.resources.File.fromIOFile(new File(filePath), project);
            Issuable issuable = resourcePerspectives.as(Issuable.class, resource);

            if (issuable != null) {
                String info = (String)violation.get("issue");
                int lineNum = Integer.parseInt(violation.get("beginline").toString());
                if (lineNum == 0) {
                    lineNum++;
                }

                Issue issue = issuable.newIssueBuilder()
                        .ruleKey(RuleKey.of(OCStyleRulesDefinition.REPOSITORY_KEY, (String) violation.get("rule")))
                        .line(lineNum)
                        .message(info)
                        .build();

                issuable.addIssue(issue);
            }
        }
    }
}
