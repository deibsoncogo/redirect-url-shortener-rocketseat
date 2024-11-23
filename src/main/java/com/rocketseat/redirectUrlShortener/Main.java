package com.rocketseat.redirectUrlShortener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;


public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        String pathParameters = (String) input.get("rawPath");

        String shortUrlCode = pathParameters.replace("/", "");

        if (shortUrlCode == null || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid input: 'shortUrlCode' is required");
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket("url-shortener-rocketseat")
                .key(shortUrlCode + ".json")
                .build();

        InputStream s3ObjectStream;

        try {
            s3ObjectStream = s3Client.getObject(request);
        } catch(Exception exception) {
            throw new RuntimeException("Error fetching data from S3: " + exception.getMessage(), exception);
        }

        UrlData urlData;

        try {
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch (Exception exception) {
            throw new RuntimeException("Error deserializing URL data: " + exception.getMessage(), exception);
        }

        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        Map<String, Object> response = new HashMap<>();

        if (urlData.getExpirationTime() < currentTimeInSeconds) {
            response.put("statusCode", 410);
            response.put("body", "URL ha expired");

            return response;
        }

        response.put("statusCode", 302);

        Map<String, String> headers = new HashMap<>();
        headers.put("Location", urlData.getOriginalUrl());
        response.put("headers", headers);

        return response;
    }
}
