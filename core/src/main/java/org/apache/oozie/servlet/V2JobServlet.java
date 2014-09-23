/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.BaseEngineException;
import org.apache.oozie.BundleEngine;
import org.apache.oozie.CoordinatorEngine;
import org.apache.oozie.CoordinatorEngineException;
import org.apache.oozie.DagEngine;
import org.apache.oozie.DagEngineException;
import org.apache.oozie.client.rest.JsonBean;
import org.apache.oozie.client.rest.JsonTags;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.service.BundleEngineService;
import org.apache.oozie.service.CoordinatorEngineService;
import org.apache.oozie.service.DagEngineService;
import org.apache.oozie.service.Services;
import org.apache.oozie.util.XConfiguration;
import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class V2JobServlet extends V1JobServlet {

    private static final String INSTRUMENTATION_NAME = "v2job";

    public V2JobServlet() {
        super(INSTRUMENTATION_NAME);
    }

    @Override
    protected JsonBean getWorkflowJob(HttpServletRequest request, HttpServletResponse response) throws XServletException {
        JsonBean jobBean = super.getWorkflowJobBean(request, response);
        return jobBean;
    }

    @Override
    protected JsonBean getWorkflowAction(HttpServletRequest request, HttpServletResponse response) throws XServletException {
        JsonBean actionBean = super.getWorkflowActionBean(request, response);
        return actionBean;
    }

    @Override
    protected int getCoordinatorJobLength(int defaultLen, int len) {
        return (len < 0) ? defaultLen : len;
    }

    @Override
    protected String getJMSTopicName(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        String topicName;
        String jobId = getResourceName(request);
        DagEngine dagEngine = Services.get().get(DagEngineService.class).getDagEngine(getUser(request));
        try {
            topicName = dagEngine.getJMSTopicName(jobId);
        }
        catch (DagEngineException ex) {
            throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ex);
        }
        return topicName;
    }

    @Override
    protected JSONObject getJobsByParentId(HttpServletRequest request, HttpServletResponse response)
            throws XServletException, IOException {
        return super.getJobsByParentId(request, response);
    }

    /**
     * Update coord job.
     *
     * @param request the request
     * @param response the response
     * @return the JSON object
     * @throws XServletException the x servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected JSONObject updateJob(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        CoordinatorEngine coordEngine = Services.get().get(CoordinatorEngineService.class)
                .getCoordinatorEngine(getUser(request));
        JSONObject json = new JSONObject();
        try {
            Configuration conf= new XConfiguration(request.getInputStream());
            String jobId = getResourceName(request);
            boolean dryrun = StringUtils.isEmpty(request.getParameter(RestConstants.JOB_ACTION_DRYRUN)) ? false
                    : Boolean.parseBoolean(request.getParameter(RestConstants.JOB_ACTION_DRYRUN));
            boolean showDiff = StringUtils.isEmpty(request.getParameter(RestConstants.JOB_ACTION_SHOWDIFF)) ? true
                    : Boolean.parseBoolean(request.getParameter(RestConstants.JOB_ACTION_SHOWDIFF));

            String diff = coordEngine.updateJob(conf, jobId, dryrun, showDiff);
            JSONObject diffJson = new JSONObject();
            diffJson.put(JsonTags.COORD_UPDATE_DIFF, diff);
            json.put(JsonTags.COORD_UPDATE, diffJson);
        }
        catch (CoordinatorEngineException e) {
            throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, e);
        }
        return json;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String getJobStatus(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        String status;
        String jobId = getResourceName(request);
        try {
            if (jobId.endsWith("-B")) {
                BundleEngine engine = Services.get().get(BundleEngineService.class).getBundleEngine(getUser(request));
                status = engine.getJobStatus(jobId);
            } else if (jobId.endsWith("-W")) {
                DagEngine engine = Services.get().get(DagEngineService.class).getDagEngine(getUser(request));
                status = engine.getJobStatus(jobId);
            } else {
                CoordinatorEngine engine =
                        Services.get().get(CoordinatorEngineService.class).getCoordinatorEngine(getUser(request));
                if (jobId.contains("-C@")) {
                    status = engine.getActionStatus(jobId);
                } else {
                    status = engine.getJobStatus(jobId);
                }
            }
        } catch (BaseEngineException ex) {
            throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ex);
        }
        return status;
    }
}
