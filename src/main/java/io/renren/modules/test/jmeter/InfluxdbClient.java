package io.renren.modules.test.jmeter;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InfluxdbClient extends InfluxdbBackendListenerClient {

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
        sampleResults.stream().forEach(sampleResult -> {
            if (!sampleResult.isSuccessful()) {
                String assertMsg = Arrays.stream(sampleResult.getAssertionResults()).map(assertionResult -> assertionResult.getFailureMessage()).collect(Collectors.joining(","));
                sampleResult.setResponseMessage(assertMsg);
            }
        });
        super.handleSampleResults(sampleResults, context);
    }
}
