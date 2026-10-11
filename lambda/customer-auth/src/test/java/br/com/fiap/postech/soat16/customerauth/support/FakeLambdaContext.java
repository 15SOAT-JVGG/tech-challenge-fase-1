package br.com.fiap.postech.soat16.customerauth.support;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class FakeLambdaContext implements Context {

    public static final String REQUEST_ID = "c0ffee00-0000-4000-8000-000000000001";

    private final List<String> logLines = new ArrayList<>();

    public List<String> logLines() {
        return logLines;
    }

    @Override
    public String getAwsRequestId() {
        return REQUEST_ID;
    }

    @Override
    public String getLogGroupName() {
        return "/aws/lambda/customer-auth";
    }

    @Override
    public String getLogStreamName() {
        return "test";
    }

    @Override
    public String getFunctionName() {
        return "customer-auth";
    }

    @Override
    public String getFunctionVersion() {
        return "$LATEST";
    }

    @Override
    public String getInvokedFunctionArn() {
        return "arn:aws:lambda:us-east-1:000000000000:function:customer-auth";
    }

    @Override
    public CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return 10_000;
    }

    @Override
    public int getMemoryLimitInMB() {
        return 512;
    }

    @Override
    public LambdaLogger getLogger() {
        return new LambdaLogger() {
            @Override
            public void log(String message) {
                logLines.add(message);
            }

            @Override
            public void log(byte[] message) {
                logLines.add(new String(message, StandardCharsets.UTF_8));
            }
        };
    }
}
