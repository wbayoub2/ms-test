package com.machine.ms.test.infra.config;

import com.machine.ms.test.core.port.out.CoverageStore;
import com.machine.ms.test.core.port.out.DiffSnapshotStore;
import com.machine.ms.test.core.port.out.GateReportStore;
import com.machine.ms.test.core.port.out.ImpactSnapshotStore;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;
import com.machine.ms.test.core.port.out.CommitChangeStore;
import com.machine.ms.test.core.port.out.TestRunStore;
import com.machine.ms.test.core.service.DiffService;
import com.machine.ms.test.core.service.GateEvaluationService;
import com.machine.ms.test.core.service.ImpactAnalysisService;
import com.machine.ms.test.core.service.StepIdGenerator;
import com.machine.ms.test.core.service.StepRequestValidator;
import com.machine.ms.test.core.service.functional.ArtifactEvidenceRules;
import com.machine.ms.test.core.service.functional.CoverageIntelligenceService;
import com.machine.ms.test.core.service.functional.CoverageQueries;
import com.machine.ms.test.core.service.functional.EvidenceService;
import com.machine.ms.test.core.service.functional.ForensicsService;
import com.machine.ms.test.core.service.functional.ImpactComputationService;
import com.machine.ms.test.core.service.functional.QualityGateService;
import com.machine.ms.test.core.service.functional.RunSnapshotQueries;
import com.machine.ms.test.core.service.functional.TestCatalogService;
import com.machine.ms.test.core.service.functional.TraceabilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServicesConfiguration {

    @Bean
    public StepRequestValidator stepRequestValidator() {
        return new StepRequestValidator();
    }

    @Bean
    public StepIdGenerator stepIdGenerator() {
        return new StepIdGenerator();
    }

    @Bean
    public DiffService diffService(DiffSnapshotStore diffSnapshotStore,
                                   CoverageStore coverageStore,
                                   CommitChangeStore commitChangeStore,
                                   StepIdGenerator stepIdGenerator,
                                   MsTestStepValidationProperties stepValidationProperties) {
        return new DiffService(
                diffSnapshotStore,
                coverageStore,
                commitChangeStore,
                stepIdGenerator,
                stepValidationProperties.getDiffPolicy());
    }

    @Bean
    public ImpactAnalysisService impactAnalysisService(ImpactSnapshotStore impactSnapshotStore,
                                                       CoverageStore coverageStore,
                                                       DiffService diffService,
                                                       StepIdGenerator stepIdGenerator,
                                                       MsTestStepValidationProperties stepValidationProperties) {
        return new ImpactAnalysisService(
                impactSnapshotStore,
                coverageStore,
                diffService,
                stepIdGenerator,
                stepValidationProperties.getImpactPolicy());
    }

    @Bean
    public GateEvaluationService gateEvaluationService(GateReportStore gateReportStore,
                                                       TestRunStore testRunStore,
                                                       StepIdGenerator stepIdGenerator) {
        return new GateEvaluationService(gateReportStore, testRunStore, stepIdGenerator);
    }

    @Bean
    public RunSnapshotQueries runSnapshotQueries(TestRunStore testRunStore) {
        return new RunSnapshotQueries(testRunStore);
    }

    @Bean
    public CoverageQueries coverageQueries(CoverageStore coverageStore) {
        return new CoverageQueries(coverageStore);
    }

    @Bean
    public TraceabilityService traceabilityService(RunSnapshotQueries runQueries, CoverageQueries coverageQueries) {
        return new TraceabilityService(runQueries, coverageQueries);
    }

    @Bean
    public ImpactComputationService impactComputationService(CoverageQueries coverageQueries, CommitChangeStore commitChangeStore) {
        return new ImpactComputationService(coverageQueries, commitChangeStore);
    }

    @Bean
    public ForensicsService forensicsService(TraceabilityService traceabilityService, CommitChangeStore commitChangeStore) {
        return new ForensicsService(traceabilityService, commitChangeStore);
    }

    @Bean
    public CoverageIntelligenceService coverageIntelligenceService(CoverageQueries coverageQueries) {
        return new CoverageIntelligenceService(coverageQueries);
    }

    @Bean
    public EvidenceService evidenceService(RunSnapshotQueries runQueries, ArtifactLinkStore artifactLinkStore) {
        return new EvidenceService(runQueries, artifactLinkStore);
    }

    @Bean
    public TestCatalogService testCatalogService(RunSnapshotQueries runQueries) {
        return new TestCatalogService(runQueries);
    }

    @Bean
    public ArtifactEvidenceRules artifactEvidenceRules(RunSnapshotQueries runQueries, ArtifactLinkStore artifactLinkStore) {
        return new ArtifactEvidenceRules(runQueries, artifactLinkStore);
    }

    @Bean
    public QualityGateService qualityGateService(RunSnapshotQueries runQueries, ArtifactEvidenceRules artifactRules) {
        return new QualityGateService(runQueries, artifactRules);
    }
}
