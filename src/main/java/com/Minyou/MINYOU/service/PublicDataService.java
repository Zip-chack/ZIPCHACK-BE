package com.Minyou.MINYOU.service;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PublicDataService {
    // 보안 프로그램 간섭을 피하기 위해 apis.data.go.kr 표준 도메인 사용
    private static final String PUBLIC_DATA_BASE_URL = "http://apis.data.go.kr";
    private final WebClient webClient;
    private final KakaoMapService kakaoMapService;
    private String apiKey;

    public PublicDataService(KakaoMapService kakaoMapService) {
        this.kakaoMapService = kakaoMapService;
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("KDATA_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.error("KDATA_KEY가 설정되지 않았습니다.");
        }
        
        this.webClient = WebClient.builder()
                .baseUrl(PUBLIC_DATA_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 아파트 전월세 실거래가 조회
     */
    public List<Map<String, Object>> getApartmentRentData(String lawdCd, String dealYmd) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("공공데이터 API 키가 없습니다.");
        }

        try {
            // 표준 게이트웨이 주소로 요청 구성
            String path = "/1613000/RTMSOBJSvc/getRTMSDataSvcAptRent";
            
            String url = String.format(
                "%s%s?serviceKey=%s&LAWD_CD=%s&DEAL_YMD=%s&numOfRows=1000&pageNo=1",
                PUBLIC_DATA_BASE_URL, path, apiKey, lawdCd, dealYmd
            );
            
            URI uri = new URI(url);
            log.info("공공데이터 호출 주소: {}", url.replace(apiKey, "REDACTED"));

            String response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(new RuntimeException("API 에러: " + errorBody)))
                    )
                    .bodyToMono(String.class)
                    .block();

            return parseApartmentRentXml(response);
        } catch (Exception e) {
            log.error("조회 실패: {}", e.getMessage());
            throw new RuntimeException("데이터 조회 실패: " + e.getMessage());
        }
    }

    /**
     * XML 응답을 파싱하여 리스트로 변환
     */
    private List<Map<String, Object>> parseApartmentRentXml(String xmlResponse) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // 간단한 XML 파싱 (실제로는 더 정교한 파싱 필요)
            // 여기서는 기본적인 파싱만 구현
            if (xmlResponse == null || xmlResponse.isEmpty()) {
                return results;
            }

            // XML에서 item 태그 찾기
            String[] items = xmlResponse.split("<item>");
            
            for (int i = 1; i < items.length; i++) {
                String item = items[i].split("</item>")[0];
                Map<String, Object> result = new HashMap<>();
                
                // 각 필드 추출
                result.put("apartmentName", extractXmlTag(item, "아파트"));
                result.put("buildYear", extractXmlTag(item, "건축년도"));
                result.put("roadAddress", extractXmlTag(item, "도로명주소"));
                result.put("jibunAddress", extractXmlTag(item, "지번주소"));
                result.put("floor", extractXmlTag(item, "층"));
                result.put("area", extractXmlTag(item, "전용면적"));
                result.put("deposit", extractXmlTag(item, "보증금"));
                result.put("monthlyRent", extractXmlTag(item, "월세금액"));
                result.put("dealDate", extractXmlTag(item, "년"));
                result.put("dealMonth", extractXmlTag(item, "월"));
                result.put("dealDay", extractXmlTag(item, "일"));
                
                // 주소를 좌표로 변환
                String address = result.get("roadAddress") != null && !result.get("roadAddress").toString().isEmpty()
                    ? result.get("roadAddress").toString()
                    : result.get("jibunAddress").toString();
                
                if (address != null && !address.isEmpty()) {
                    try {
                        Map<String, Object> coord = kakaoMapService.searchAddress(address);
                        if (coord != null) {
                            result.put("lat", coord.get("lat"));
                            result.put("lng", coord.get("lng"));
                        }
                    } catch (Exception e) {
                        log.warn("주소 좌표 변환 실패: {}", address);
                    }
                }
                
                results.add(result);
            }
        } catch (Exception e) {
            log.error("XML 파싱 실패: {}", e.getMessage());
        }
        
        return results;
    }

    /**
     * XML 태그에서 값 추출
     */
    private String extractXmlTag(String xml, String tagName) {
        try {
            String startTag = "<" + tagName + ">";
            String endTag = "</" + tagName + ">";
            int start = xml.indexOf(startTag);
            if (start == -1) return "";
            start += startTag.length();
            int end = xml.indexOf(endTag, start);
            if (end == -1) return "";
            return xml.substring(start, end).trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 지역 코드로 아파트 전월세 데이터 조회 (최근 3개월)
     */
    public List<Map<String, Object>> getRecentApartmentRentData(String lawdCd) {
        List<Map<String, Object>> allResults = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        
        // 최근 3개월 데이터 조회
        for (int i = 0; i < 3; i++) {
            LocalDate targetDate = now.minusMonths(i);
            String dealYmd = targetDate.format(formatter);
            
            try {
                List<Map<String, Object>> monthlyData = getApartmentRentData(lawdCd, dealYmd);
                allResults.addAll(monthlyData);
                Thread.sleep(200); // API 호출 제한 고려
            } catch (Exception e) {
                log.warn("{} 데이터 조회 실패: {}", dealYmd, e.getMessage());
            }
        }
        
        return allResults;
    }
}

