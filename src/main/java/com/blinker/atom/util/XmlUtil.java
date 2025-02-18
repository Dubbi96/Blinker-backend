package com.blinker.atom.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

public class XmlUtil {
    private static final ObjectMapper xmlMapper = new XmlMapper();
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static String convertXmlToJson(String xml) {
        try {
            JsonNode jsonNode = xmlMapper.readTree(xml.getBytes());
            return jsonMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("XML -> JSON 변환 중 오류 발생", e);
        }
    }

    public static PGobject convertToJsonb(String jsonString) throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(jsonString);
        return jsonObject;
    }
}
