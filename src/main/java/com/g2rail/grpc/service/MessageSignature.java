package com.g2rail.grpc.service;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.*;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64;

public class MessageSignature {
    private String apiKey;
    private String apiSecret;
    private Object message;
    private Map<String, Object> simpleFields = new TreeMap<String, Object>();
    private static final List<DescriptorProtos.FieldDescriptorProto.Type> simpleKinds = Arrays.asList(
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

    public MessageSignature(String apiKey, String apiSecret, Object message) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.message = message;
    }

    public String calculateSign() {
        List<Descriptors.FieldDescriptor> fields = ((GeneratedMessageV3)message).getDescriptorForType().getFields();
        simpleFields = new TreeMap<String, Object>();

        fields.forEach(((fieldDescriptor) -> {
            DescriptorProtos.FieldDescriptorProto fieldDesc = fieldDescriptor.toProto();
            if (simpleKinds.contains(fieldDesc.getType())) {
                Object value = ((GeneratedMessageV3)message).getField(fieldDescriptor);
                simpleFields.put(fieldDesc.getName(), value);
            }
        }));
        return getMd5Hash(buildSignString(simpleFields));
    }

    private String getMd5Hash(String source) {
        return DigestUtils.md5Hex(source).toLowerCase();
    }

    private String buildSignString(Map<String, Object> simpleFields) {
        StringBuilder source = new StringBuilder("");
        simpleFields.put("api_key", apiKey);
        simpleFields.put("t", String.valueOf(new Date().getTime()/1000));
        for (Map.Entry<String, Object> entry : simpleFields.entrySet()) {
            source.append(entry.getKey() + "=" + entry.getValue());
        }
        source.append(apiSecret);
        return source.toString();
    }
}
