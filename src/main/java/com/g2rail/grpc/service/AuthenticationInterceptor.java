package com.g2rail.grpc.service;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;

import io.grpc.*;
import org.apache.commons.codec.digest.DigestUtils;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.*;

public class AuthenticationInterceptor implements ClientInterceptor {
    private MessageSignature signature;
    private String apiKey;

    private final List<DescriptorProtos.FieldDescriptorProto.Type> simpleKinds = Arrays.asList(
        TYPE_DOUBLE,
        TYPE_FLOAT,
        TYPE_INT64,
        TYPE_UINT64,
        TYPE_INT32,
        TYPE_FIXED64,
        TYPE_FIXED32,
        TYPE_BOOL,
        TYPE_STRING,
        TYPE_BYTES,
        TYPE_UINT32,
        TYPE_ENUM,
        TYPE_SFIXED32,
        TYPE_SFIXED64,
        TYPE_SINT32 ,
        TYPE_SINT64
    );
    public AuthenticationInterceptor(String apiKey, MessageSignature signature) {
        this.signature = signature;
        this.apiKey = apiKey;
    }

    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> methodDescriptor, final CallOptions callOptions, final Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {

            @Override
            public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), signature.calculateSign());
                headers.put(Metadata.Key.of("date", Metadata.ASCII_STRING_MARSHALLER), getServerTime());
                headers.put(Metadata.Key.of("from", Metadata.ASCII_STRING_MARSHALLER), apiKey);

                super.start(responseListener, headers);
            }

        };
    }

    String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }
}