package org.edx.mobile.http.interceptor;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * An OkHttp network interceptor that handles the qualified no-cache="header" Cache-Control directive in the
 * response, which is currently handled by OkHttp
 */
public class NoCacheHeaderStrippingInterceptor implements Interceptor {
    /**
     * A regular expression for finding a valid 'stale-if-error' directive in the Cache-Control
     * header.
     */
    private static final Pattern PATTERN_NO_CACHE_HEADER = Pattern.compile(
            "^((.*[,;])?\\s*)no-cache(\\s*=\\s*\"([^,;\\s]+)\"\\s*([,;].*)?)$");
    /**
     * The capturing group index for the value of the 'stale-if-error' directive in
     * {@link StaleIfErrorHandlingInterceptor#PATTERN_STALE_IF_ERROR}.
     */
    private static final int GROUP_NO_CACHE_HEADERS = 4;
    /**
     * The capturing group index for the value of the 'stale-if-error' directive in
     * {@link StaleIfErrorHandlingInterceptor#PATTERN_STALE_IF_ERROR}.
     */
    private static final int GROUP_PRECEDING_DIRECTIVES = 2;
    /**
     * The capturing group index for the value of the 'stale-if-error' directive in
     * {@link StaleIfErrorHandlingInterceptor#PATTERN_STALE_IF_ERROR}.
     */
    private static final int GROUP_SUBSEQUENT_DIRECTIVES = 5;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        Response.Builder strippedResponseBuilder = null;
        final Headers headers = response.headers();
        for (int i = 0, headersCount = headers.size(); i < headersCount; i++) {
            final String headerName = headers.name(i);
            String headerValue = headers.value(i);
            if (headerName.equals("Cache-Control")) {
                final Matcher directiveMatcher = PATTERN_NO_CACHE_HEADER.matcher(headerValue);
                while (directiveMatcher.find()) {
                    final String noCacheHeadersString =
                            directiveMatcher.group(GROUP_NO_CACHE_HEADERS);
                    if (noCacheHeadersString != null) {
                        if (strippedResponseBuilder == null) {
                            strippedResponseBuilder = response.newBuilder();
                        }
                        for (final String noCacheHeaderName : noCacheHeadersString.split(",")) {
                            strippedResponseBuilder.removeHeader(noCacheHeaderName);
                        }
                    }
                }
            }
        }
        if (strippedResponseBuilder != null) {
            response = strippedResponseBuilder.build();
        }
        return response;
    }
}
