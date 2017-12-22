/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: January 13, 2017
 *
 */

package com.odysseusinc.arachne.portal.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.utils.cohortcharacterization.CohortCharacterizationDocType;
import com.odysseusinc.arachne.portal.model.Submission;
import com.odysseusinc.arachne.portal.repository.SubmissionResultFileRepository;
import com.odysseusinc.arachne.storage.model.ArachneFileSourced;
import com.odysseusinc.arachne.storage.model.QuerySpec;
import com.odysseusinc.arachne.storage.service.ContentStorageService;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SubmissionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionHelper.class);
    private static final String PLE_SUMMARY_FILENAME = "PLE_summary.csv";
    private static final String PLP_SUMMARY_FILENAME = "PLP_summary.csv";
    private static final String INCIDENCE_SUMMARY_FILENAME = "ir_summary.csv";
    private static final String CAN_NOT_PARSE_LOG = "Can not parse '{}'";
    private static final String CAN_NOT_BUILD_EXTEND_INFO_LOG = "Can not build extendInfo for submission with id='{}'";

    private final SubmissionResultFileRepository submissionResultFileRepository;
    private final AnalysisHelper analysisHelper;
    private final ContentStorageService contentStorageService;
    private final ContentStorageHelper contentStorageHelper;

    public SubmissionHelper(SubmissionResultFileRepository submissionResultFileRepository, AnalysisHelper analysisHelper, ContentStorageService contentStorageService, ContentStorageHelper contentStorageHelper) {

        this.submissionResultFileRepository = submissionResultFileRepository;
        this.analysisHelper = analysisHelper;
        this.contentStorageService = contentStorageService;
        this.contentStorageHelper = contentStorageHelper;
    }

    public void updateSubmissionExtendedInfo(final Submission submission) {

        final CommonAnalysisType analysisType = submission.getSubmissionGroup().getAnalysisType();
        SubmissionExtendInfoAnalyzeStrategy strategy;
        switch (analysisType) {
            case COHORT: {
                strategy = new CohortSubmissionExtendInfoStrategy();
                break;
            }
            case COHORT_CHARACTERIZATION: {
                strategy = new CohortCharacterizationSubmissionExtendInfoStrategy();
                break;
            }
            case INCIDENCE: {
                strategy = new IncidenceSubmissionExtendInfoStrategy();
                break;
            }
            case ESTIMATION: {
                strategy = new EstimationSubmissionExtendInfoStrategy();
                break;
            }
            case PREDICTION: {
                strategy = new PredictionSubmissionExtendInfoStrategy();
                break;
            }
            default: {
                strategy = new DefaultSubmissionExtendInfoStrategy();
            }
        }
        strategy.updateExtendInfo(submission);
    }

    private static abstract class SubmissionExtendInfoAnalyzeStrategy {

        public abstract void updateExtendInfo(final Submission submission);
    }

    private static class DefaultSubmissionExtendInfoStrategy extends SubmissionExtendInfoAnalyzeStrategy {

        @Override
        public void updateExtendInfo(final Submission submission) {

        }
    }

    private class CohortSubmissionExtendInfoStrategy extends SubmissionExtendInfoAnalyzeStrategy {

        @Override
        public void updateExtendInfo(final Submission submission) {

            QuerySpec querySpec = new QuerySpec();

            querySpec.setPath(contentStorageHelper.getResultFilesDir(submission));
            querySpec.setName("%count%.csv");
            querySpec.setNameLike(true);

            final List<ArachneFileSourced> files = contentStorageService.searchFiles(querySpec);

            final JsonObject resultInfo = new JsonObject();
            final String jsonColumnName = "persons";
            resultInfo.add(jsonColumnName, new JsonPrimitive(0));
            files.forEach(f -> {
                try {
                    final CSVParser parser = CSVParser.parse(f.getInputStream(), Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader());
                    final Integer countColumnNumber = parser.getHeaderMap().get("count");
                    final List<CSVRecord> records = parser.getRecords();
                    if (!CollectionUtils.isEmpty(records)) {
                        final long count = Long.parseLong(records.get(0).get(countColumnNumber));
                        final JsonPrimitive person = resultInfo.getAsJsonPrimitive(jsonColumnName);
                        final long asLong = person.getAsLong();
                        resultInfo.add(jsonColumnName, new JsonPrimitive(asLong + count));
                    }
                } catch (IOException e) {
                    LOGGER.warn("Can not open \"count\" file, ResultFile={}. Error={}", f, e.getMessage());
                }
            });
            submission.setResultInfo(resultInfo);
        }
    }

    private class CohortCharacterizationSubmissionExtendInfoStrategy extends SubmissionExtendInfoAnalyzeStrategy {

        @Override
        public void updateExtendInfo(final Submission submission) {

            final JsonObject resultInfo = new JsonObject();
            try {
                final Set<String> docTypes = Arrays.stream(CohortCharacterizationDocType.values())
                        .filter(docType -> CohortCharacterizationDocType.UNKNOWN != docType)
                        .map(docType -> docType.getTitle())
                        .collect(Collectors.toSet());

                final String resultsDir = contentStorageHelper.getResultFilesDir(submission);

                final QuerySpec querySpec = new QuerySpec();
                querySpec.setPath(resultsDir);
                querySpec.setContentTypes(docTypes);

                final int count = contentStorageService.searchFiles(querySpec).size();

                final JsonElement element = new JsonPrimitive(count);
                resultInfo.add("reports", element);
            } catch (Exception e) {
                LOGGER.warn(CAN_NOT_BUILD_EXTEND_INFO_LOG, submission.getId());
                LOGGER.warn("Error: ", e);
            }
            submission.setResultInfo(resultInfo);
        }
    }

    private class IncidenceSubmissionExtendInfoStrategy extends SubmissionExtendInfoAnalyzeStrategy {

        @Override
        public void updateExtendInfo(Submission submission) {

            final JsonObject resultInfo = new JsonObject();

            final String resultsDir = contentStorageHelper.getResultFilesDir(submission);

            final QuerySpec querySpec = new QuerySpec();
            querySpec.setPath(resultsDir);
            querySpec.setName(INCIDENCE_SUMMARY_FILENAME);

            final List<ArachneFileSourced> files = contentStorageService.searchFiles(querySpec);

            if (files.size() > 0) {
                ArachneFileSourced arachneFile = files.get(0);
                try {
                    final CSVParser parser = CSVParser.parse(arachneFile.getInputStream(), Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader());
                    final Map<String, Integer> headers = parser.getHeaderMap();

                    final String personCountHeader = "PERSON_COUNT";
                    final String timeAtRiskHeader = "TIME_AT_RISK";
                    final String casesHeader = "CASES";

                    Map<String, Integer> values =
                            Arrays.asList(personCountHeader, timeAtRiskHeader, casesHeader)
                                    .stream()
                                    .collect(Collectors.toMap(header -> header, headers::get));
                    final List<CSVRecord> records = parser.getRecords();

                    if (!CollectionUtils.isEmpty(records)) {
                        final CSVRecord firstRecord = records.get(0);
                        final String personCount = firstRecord.get(values.get(personCountHeader));
                        final String timeAtRisk = firstRecord.get(values.get(timeAtRiskHeader));
                        final String cases = firstRecord.get(values.get(casesHeader));

                        resultInfo.add(personCountHeader, getJsonPrimitive(personCount));
                        resultInfo.add(timeAtRiskHeader, getJsonPrimitive(timeAtRisk));
                        resultInfo.add(casesHeader, getJsonPrimitive(cases));
                        try {
                            final float casesFloat = cast(cases).floatValue();
                            try {
                                final float timeAtRiskFloat = cast(timeAtRisk).floatValue();
                                final float rate = timeAtRiskFloat > 0 ? casesFloat / timeAtRiskFloat * 1000 : 0F;
                                resultInfo.add("RATE", new JsonPrimitive(rate));
                            } catch (IllegalArgumentException e) {
                                LOGGER.debug("'TIME_AT_RISK' is not correct value, skipping calculate 'RATE' value");
                            }
                            try {
                                final float personsFloat = cast(personCount).floatValue();
                                final float proportion = personsFloat > 0 ? casesFloat / personsFloat * 1000 : 0F;
                                resultInfo.add("PROPORTION", new JsonPrimitive(proportion));
                            } catch (IllegalArgumentException e) {
                                LOGGER.debug("'TIME_AT_RISK' is not correct value, skipping calculate 'PROPORTION' value");
                            }
                        } catch (IllegalArgumentException e) {
                            LOGGER.debug("'PERSON_COUNT' is not correct value, skipping calculate 'RATE' & 'PROPORTION' values");
                        }

                    }
                } catch (IOException e) {
                    LOGGER.debug(CAN_NOT_PARSE_LOG, INCIDENCE_SUMMARY_FILENAME);
                } catch (Exception e) {
                    LOGGER.warn(CAN_NOT_BUILD_EXTEND_INFO_LOG, submission.getId());
                    LOGGER.warn("Error: ", e);
                }
            }
            submission.setResultInfo(resultInfo);
        }
    }

    private class EstimationSubmissionExtendInfoStrategy extends SubmissionExtendInfoAnalyzeStrategy {

        @Override
        public void updateExtendInfo(final Submission submission) {

            final JsonObject resultInfo = new JsonObject();

            final String resultsDir = contentStorageHelper.getResultFilesDir(submission);

            final QuerySpec querySpec = new QuerySpec();
            querySpec.setPath(resultsDir);
            querySpec.setName(PLE_SUMMARY_FILENAME);

            final List<ArachneFileSourced> files = contentStorageService.searchFiles(querySpec);

            if (!CollectionUtils.isEmpty(files)) {
                ArachneFileSourced arachneFile = files.get(0);
                try {
                    final CSVParser parser = CSVParser.parse(arachneFile.getInputStream(), Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader());
                    final Map<String, Integer> headers = parser.getHeaderMap();
                    Map<String, Integer> headerMap =
                            Arrays.asList("lower .95", "upper .95", "logRr", "seLogRr", "p", "cal p",
                                    "cal p - lower .95", "cal p - upper .95")
                                    .stream()
                                    .collect(Collectors.toMap(header -> header, headers::get));
                    final List<CSVRecord> records = parser.getRecords();
                    if (!CollectionUtils.isEmpty(records)) {
                        final CSVRecord firstRecord = records.get(0);
                        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                            final String header = entry.getKey();
                            final Integer entryValue = entry.getValue();
                            if (Objects.nonNull(entryValue)) {
                                final String value = firstRecord.get(entryValue);
                                resultInfo.add(header, getJsonPrimitive(value));
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn(CAN_NOT_PARSE_LOG, PLE_SUMMARY_FILENAME);
                } catch (Exception e) {
                    LOGGER.warn(CAN_NOT_BUILD_EXTEND_INFO_LOG, submission.getId());
                    LOGGER.warn("Error: ", e);
                }

            }
            submission.setResultInfo(resultInfo);
        }
    }

    private class PredictionSubmissionExtendInfoStrategy extends SubmissionExtendInfoAnalyzeStrategy {

        @Override
        public void updateExtendInfo(final Submission submission) {

            final JsonObject resultInfo = new JsonObject();

            final String resultsDir = contentStorageHelper.getResultFilesDir(submission);

            final QuerySpec querySpec = new QuerySpec();
            querySpec.setPath(resultsDir);
            querySpec.setName(PLP_SUMMARY_FILENAME);
            final List<ArachneFileSourced> files = contentStorageService.searchFiles(querySpec);
            if (!CollectionUtils.isEmpty(files)) {
                ArachneFileSourced arachneFile = files.get(0);
                try {
                    final CSVParser parser = CSVParser.parse(arachneFile.getInputStream(), Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader());
                    final List<CSVRecord> records = parser.getRecords();
                    for (CSVRecord record : records) {
                        final ArrayList<String> recordAsList = new ArrayList<>();
                        for (String s : record) {
                            recordAsList.add(s);
                        }
                        get(resultInfo, recordAsList);
                    }
                } catch (IOException e) {
                    LOGGER.warn(CAN_NOT_PARSE_LOG, PLP_SUMMARY_FILENAME);
                } catch (Exception e) {
                    LOGGER.warn(CAN_NOT_BUILD_EXTEND_INFO_LOG, submission.getId());
                    LOGGER.warn("Error: ", e);
                }

            }
            submission.setResultInfo(resultInfo);
        }

        void get(JsonObject object, final List<String> record) {

            if (record.size() == 2) {
                object.add(record.remove(0), getJsonPrimitive(record.remove(0)));
            } else {
                final String property = record.remove(0);
                JsonObject jsonObject = object.getAsJsonObject(property);
                if (Objects.isNull(jsonObject)) {
                    jsonObject = new JsonObject();
                }
                object.add(property, jsonObject);
                get(jsonObject, record);
            }
        }
    }

    private static JsonElement getJsonPrimitive(final String value) {

        if (value == null) {
            return JsonNull.INSTANCE;
        }
        try {
            return new JsonPrimitive(cast(value));
        } catch (IllegalArgumentException e) {
            return new JsonPrimitive(value);
        }
    }

    private static Number cast(final String value) throws IllegalArgumentException {

        if (value != null && value.matches("^\\d+(\\.)\\d*$")) {
            return Float.valueOf(value);
        } else if (value != null && value.matches("^\\d+$")) {
            return Long.valueOf(value);
        } else {
            throw new IllegalArgumentException();
        }
    }

}
